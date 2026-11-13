package ar.edu.utn.dds.k3003;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;

/** Exercises HTTP controllers, real JPA mappings and outbound HTTP with isolated H2. */
@SpringBootTest(
    classes = ar.edu.utn.dds.k3003.app.Application.class,
    properties = {"spring.config.location=classpath:application-integration.properties"})
@AutoConfigureMockMvc
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
class ApiPersistenciaTest {
  static final HttpServer remote = startRemote();
  static final AtomicInteger satisfactions = new AtomicInteger();
  static final AtomicInteger receivedUnits = new AtomicInteger();
  static final java.util.concurrent.atomic.AtomicReference<java.time.LocalDate> periodStart =
      new java.util.concurrent.atomic.AtomicReference<>();
  static final Set<String> deliveryKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();
  static final List<String> attemptedKeys = new java.util.concurrent.CopyOnWriteArrayList<>();
  static final java.util.concurrent.atomic.AtomicBoolean failAcceptance =
      new java.util.concurrent.atomic.AtomicBoolean();
  static final Map<String, Map<String, Object>> needs = new java.util.concurrent.ConcurrentHashMap<>();
  static final Map<String, AtomicInteger> receivedByNeed = new java.util.concurrent.ConcurrentHashMap<>();
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

  @Test
  void databaseResetAlsoRemovesPendingWorkerTasksWhenMessagingIsDisabled() throws Exception {
    jdbc.update(
        "insert into matchmaking_outbox (paquete_id, content, sent) values (?, ?, ?)",
        "reset-task",
        "{}",
        false);
    call("DELETE", "/admin/db", null, 204);
    assertEquals(0L, jdbc.queryForObject("select count(*) from matchmaking_outbox", Long.class));
  }

  static HttpServer startRemote() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/estado") && failAcceptance.compareAndSet(true, false)) {
              exchange.sendResponseHeaders(503, -1);
              exchange.close();
              return;
            }
            String output;
            if (path.endsWith("/periodo"))
              output =
                  periodStart.get() == null
                      ? "{\"inicio\":null}"
                      : "{\"inicio\":\"" + periodStart.get() + "\"}";
            else if (path.endsWith("/satisfaccion")) {
              String key = exchange.getRequestHeaders().getFirst("Idempotency-Key");
              if (key != null) attemptedKeys.add(key);
              if (key == null || deliveryKeys.add(key)) {
                satisfactions.incrementAndGet();
                receivedUnits.addAndGet(
                    Integer.parseInt(exchange.getRequestURI().getQuery().split("=")[1]));
                String need = path.substring("/necesidades/".length(), path.length() - "/satisfaccion".length());
                receivedByNeed.computeIfAbsent(need, ignored -> new AtomicInteger()).addAndGet(
                    Integer.parseInt(exchange.getRequestURI().getQuery().split("=")[1]));
              }
              output = "{}";
            } else if (path.startsWith("/necesidades/")) {
              String id = path.substring("/necesidades/".length());
              if (id.startsWith("missing")) {
                exchange.sendResponseHeaders(404, -1); exchange.close(); return;
              }
              var value = new HashMap<>(needs.getOrDefault(id, Map.of("id", id, "entidadID", "e",
                  "nivelDeUrgencia", 1, "descripcion", "necesidad", "cantidadObjetivo", 1000,
                  "productoSolicitadoID", "p", "tipo", "EXTRAORDINARIA")));
              value.put("cantidadAsignada", receivedByNeed.getOrDefault(id, new AtomicInteger()).get());
              output = new ObjectMapper().writeValueAsString(value);
            } else if (path.equals("/necesidades")) {
              output = new ObjectMapper().writeValueAsString(needs.values());
            } else if (path.startsWith("/donaciones/"))
              output = "{\"id\":\"d\",\"estado\":\"ACEPTADA\"}";
            else output = "[]";
            byte[] bytes = output.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
          });
      server.start();
      return server;
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  @DynamicPropertySource
  static void urls(DynamicPropertyRegistry properties) {
    String url = "http://127.0.0.1:" + remote.getAddress().getPort();
    properties.add("integrations.donadores-url", () -> url);
    properties.add("integrations.donaciones-url", () -> url);
  }

  @BeforeEach
  void clear() throws Exception {
    call("DELETE", "/admin/db", null, 204);
    satisfactions.set(0);
    receivedUnits.set(0);
    periodStart.set(null);
    deliveryKeys.clear();
    attemptedKeys.clear();
    failAcceptance.set(false);
    needs.clear();
    receivedByNeed.clear();
  }

  @AfterAll
  static void close() {
    remote.stop(0);
  }

  JsonNode call(String method, String path, Object body, int status) throws Exception {
    var request =
        request(org.springframework.http.HttpMethod.valueOf(method), path)
            .header("X-Trace-Id", "prueba-jpa")
            .contentType(MediaType.APPLICATION_JSON);
    if (body != null) request.content(json.writeValueAsString(body));
    var result = mvc.perform(request).andReturn();
    assertEquals(
        status, result.getResponse().getStatus(), result.getResponse().getContentAsString());
    assertEquals("prueba-jpa", result.getResponse().getHeader("X-Trace-Id"));
    String content = result.getResponse().getContentAsString();
    return content.isBlank() ? json.nullNode() : json.readTree(content);
  }

  String depot() throws Exception {
    String id =
        call(
                "POST",
                "/depositos",
                Map.of("nombre", "Depósito", "direccion", "Medrano", "capacidadMaxima", 100),
                200)
            .path("id")
            .asText();
    call("PATCH", "/depositos/" + id + "/algoritmo", Map.of("algoritmo", "SUB_ATENDIDOS"), 200);
    return id;
  }

  String pack(String depot, String donation, int quantity) throws Exception {
    var stock =
        call(
                "POST",
                "/depositos/" + depot + "/donacion",
                Map.of("donacionID", donation, "productoID", "p", "cantidad", quantity),
                202)
            .path("stockActual");
    return stock.get(stock.size() - 1).path("id").asText();
  }

  Object result(String depot, String pack, String need, int quantity, int leftover) {
    Map<String, Object> body = new HashMap<>();
    body.put("depositoId", depot);
    body.put("paqueteId", pack);
    body.put("necesidadId", need);
    body.put("cantidadAsignada", quantity);
    body.put("cantidadSobrante", leftover);
    return body;
  }

  void need(String id, int quantity, String type) {
    needs.put(id, Map.of("id", id, "entidadID", "e", "nivelDeUrgencia", 1,
        "descripcion", "necesidad", "cantidadObjetivo", quantity, "productoSolicitadoID", "p", "tipo", type));
  }

  int assigned(String need) throws Exception {
    int total = 0;
    for (var assignment : call("GET", "/asignaciones", null, 200))
      if (need.equals(assignment.path("necesidadID").asText())) total += assignment.path("cantidadAsignada").asInt();
    return total;
  }

  void concurrently(java.util.concurrent.Callable<?> first, java.util.concurrent.Callable<?> second) throws Exception {
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var ready = new java.util.concurrent.CountDownLatch(2);
      var go = new java.util.concurrent.CountDownLatch(1);
      var a = pool.submit(() -> { ready.countDown(); go.await(); return first.call(); });
      var b = pool.submit(() -> { ready.countDown(); go.await(); return second.call(); });
      assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)); go.countDown();
      a.get(30, java.util.concurrent.TimeUnit.SECONDS); b.get(30, java.util.concurrent.TimeUnit.SECONDS);
    }
  }

  JsonNode callback(String depot, String packet, String need, int assigned, int leftover) throws Exception {
    var response = mvc.perform(post("/internal/matchmaking/resultados").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(result(depot, packet, need, assigned, leftover)))).andReturn().getResponse();
    assertTrue(response.getStatus() == 200 || response.getStatus() == 204,
        response.getStatus() + ": " + response.getContentAsString());
    return response.getContentAsString().isBlank() ? json.nullNode() : json.readTree(response.getContentAsString());
  }

  JsonNode stock(String need, int quantity, String key, int status) throws Exception {
    var request = post("/stock/asignaciones").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(Map.of("necesidadId", need, "productoId", "p", "cantidad", quantity)));
    if (key != null) request.header("Idempotency-Key", key);
    var response = mvc.perform(request).andReturn().getResponse();
    assertEquals(status, response.getStatus(), response.getContentAsString());
    return response.getContentAsString().isBlank() ? json.nullNode() : json.readTree(response.getContentAsString());
  }

  @Test
  void concurrentWorkersAcrossDepositsCannotOverReserveARecurringNeed() throws Exception {
    need("rec", 10, "RECURRENTE");
    String d1 = depot(), d2 = depot(), p1 = pack(d1, "concurrent-rec-a", 10), p2 = pack(d2, "concurrent-rec-b", 10);
    concurrently(() -> callback(d1, p1, "rec", 10, 0), () -> callback(d2, p2, "rec", 10, 0));
    assertEquals(10, assigned("rec"));
    assertEquals(10, call("GET", "/stock/p", null, 200).path("cantidadDisponible").asInt());
  }

  @Test
  void concurrentExtraordinaryCallbacksRevalidatePartialBalance() throws Exception {
    need("extra", 10, "EXTRAORDINARIA");
    String d1 = depot(), d2 = depot(), p1 = pack(d1, "concurrent-extra-a", 7), p2 = pack(d2, "concurrent-extra-b", 7);
    concurrently(() -> callback(d1, p1, "extra", 7, 0), () -> callback(d2, p2, "extra", 7, 0));
    assertEquals(10, assigned("extra"));
    assertEquals(4, call("GET", "/stock/p", null, 200).path("cantidadDisponible").asInt());
  }

  @Test
  void concurrentWorkersRetryAnotherEligibleNeedInsteadOfLosingTheOpportunity() throws Exception {
    need("first-choice", 5, "EXTRAORDINARIA"); need("alternative", 5, "EXTRAORDINARIA");
    String d1 = depot(), d2 = depot(), p1 = pack(d1, "two-needs-a", 5), p2 = pack(d2, "two-needs-b", 5);
    concurrently(() -> callback(d1, p1, "first-choice", 5, 0), () -> callback(d2, p2, "first-choice", 5, 0));
    assertEquals(5, assigned("first-choice")); assertEquals(5, assigned("alternative"));
    assertEquals(0, call("GET", "/stock/p", null, 200).path("cantidadDisponible").asInt());
  }

  @Test
  void staleWorkerCanSelectAnotherNeedAndDiscoverANewNeedAfterNegativeDecision() throws Exception {
    need("first", 5, "EXTRAORDINARIA");
    String d1 = depot(), d2 = depot(), p1 = pack(d1, "stale-first", 5), p2 = pack(d2, "stale-second", 5);
    callback(d1, p1, "first", 5, 0);
    need("second", 5, "EXTRAORDINARIA");
    assertEquals("second", callback(d2, p2, "first", 5, 0).path("necesidadID").asText());
    assertEquals(5, assigned("first")); assertEquals(5, assigned("second"));
    needs.clear();
    String p3 = pack(d1, "negative-decision", 5);
    need("new", 3, "EXTRAORDINARIA");
    assertEquals("new", callback(d1, p3, null, 0, 5).path("necesidadID").asText());
    assertEquals(2, call("GET", "/stock/p", null, 200).path("cantidadDisponible").asInt());
  }

  @Test
  void stockReservationsAndWorkerCallbacksUseTheSameNeedLock() throws Exception {
    String d1 = depot(), d2 = depot(), stored = pack(d1, "mixed-stock", 10), pending = pack(d2, "mixed-pending", 10);
    callback(d1, stored, null, 0, 10);
    need("mixed", 10, "RECURRENTE");
    concurrently(() -> callback(d2, pending, "mixed", 10, 0), () -> {
      var response = mvc.perform(post("/stock/asignaciones").contentType(MediaType.APPLICATION_JSON)
          .content(json.writeValueAsString(Map.of("necesidadId", "mixed", "productoId", "p", "cantidad", 10))))
          .andReturn().getResponse();
      assertTrue(response.getStatus() == 200 || response.getStatus() == 204, response.getContentAsString());
      return null;
    });
    assertEquals(10, assigned("mixed"));
    assertEquals(10, call("GET", "/stock/p", null, 200).path("cantidadDisponible").asInt());
  }

  @Test
  void completedDeliveriesAreCountedOnceWhenAStaleCallbackArrives() throws Exception {
    need("delivered", 10, "EXTRAORDINARIA");
    String d = depot(), a = pack(d, "already-delivered", 7), b = pack(d, "late-worker", 10);
    callback(d, a, "delivered", 7, 0); call("POST", "/entregas", Map.of("id", a), 200);
    assertEquals(7, receivedByNeed.get("delivered").get());
    assertEquals(3, callback(d, b, "delivered", 10, 0).path("cantidadAsignada").asInt());
    assertEquals(10, assigned("delivered"));
    assertEquals(7, call("GET", "/stock/p", null, 200).path("cantidadDisponible").asInt());
  }

  @Test
  void stockValidatesCommittedNeedProductAndWholeRecurringBalance() throws Exception {
    String d = depot(), p = pack(d, "validate-stock", 15); callback(d, p, null, 0, 15);
    stock("missing-need", 10, null, 404);
    need("whole", 10, "RECURRENTE");
    stock("whole", 5, null, 204);
    assertEquals(0, assigned("whole"));
    var original = needs.get("whole"); var different = new HashMap<>(original); different.put("productoSolicitadoID", "other");
    needs.put("whole", different); stock("whole", 10, null, 400); needs.put("whole", original);
    stock("whole", 10, null, 200); stock("whole", 10, null, 204);
    assertEquals(10, assigned("whole"));
    assertEquals(5, call("GET", "/stock/p", null, 200).path("cantidadDisponible").asInt());
  }

  @Test
  void keyedEmptyStockAttemptDoesNotBlockLaterUnkeyedReservation() throws Exception {
    String d = depot(), a = pack(d, "empty-key-a", 6); callback(d, a, null, 0, 6);
    need("empty-key", 10, "RECURRENTE");
    stock("empty-key", 10, "initial-attempt", 204);
    String b = pack(d, "empty-key-b", 4); callback(d, b, "empty-key", 0, 4);
    stock("empty-key", 10, "initial-attempt", 204);
    stock("empty-key", 10, null, 200);
    assertEquals(10, assigned("empty-key"));
  }

  @Test
  void ingressReceiptSurvivesSplittingDeliveryAndConflictingReplays() throws Exception {
    String d = depot(), other = depot(), p = pack(d, "receipt-split", 10); callback(d, p, null, 0, 10);
    need("split", 3, "EXTRAORDINARIA");
    var assignments = stock("split", 3, "split-request", 200);
    String packet = assignments.get(0).path("paqueteID").asText();
    call("POST", "/entregas", Map.of("id", packet), 200);
    assertEquals(assignments.get(0).path("id").asText(), stock("split", 3, "split-request", 200).get(0).path("id").asText());
    stock("split", 2, "split-request", 409);
    pack(d, "receipt-split", 10);
    assertEquals(2, call("GET", "/depositos/" + d, null, 200).path("stockActual").size());
    assertEquals(7, call("GET", "/stock/p", null, 200).path("cantidadDisponible").asInt());
    call("POST", "/depositos/" + d + "/donacion", Map.of("donacionID", "receipt-split", "productoID", "p", "cantidad", 11), 409);
    call("POST", "/depositos/" + other + "/donacion", Map.of("donacionID", "receipt-split", "productoID", "p", "cantidad", 10), 409);
    assertEquals(1L, jdbc.queryForObject("select count(*) from logistica_recepciones where donacion_id='receipt-split'", Long.class));
    // A deployment with old packages and no receipts can reconcile legitimate fragments.
    jdbc.update("delete from logistica_recepciones where donacion_id='receipt-split'");
    pack(d, "receipt-split", 10);
    assertEquals(2, call("GET", "/depositos/" + d, null, 200).path("stockActual").size());
  }

  @Test
  void concurrentIngressRetriesCreateOneReceiptAndOnePackage() throws Exception {
    String d = depot();
    concurrently(() -> pack(d, "simultaneous-ingress", 10), () -> pack(d, "simultaneous-ingress", 10));
    assertEquals(1, call("GET", "/depositos/" + d, null, 200).path("stockActual").size());
    assertEquals(1L, jdbc.queryForObject("select count(*) from logistica_recepciones where donacion_id='simultaneous-ingress'", Long.class));
  }

  @Test
  void conservaStockAsignacionesHistorialYEntregaSinDuplicados() throws Exception {
    String d = depot(), p = pack(d, "donacion1", 10);
    var callback = result(d, p, "n", 4, 6);
    String assignment =
        call("POST", "/internal/matchmaking/resultados", callback, 200).path("id").asText();
    call("POST", "/internal/matchmaking/resultados", callback, 200);
    assertEquals(
        4, call("GET", "/internal/matchmaking/necesidades/n/cantidad-asignada", null, 200).asInt());
    assertEquals(6, call("GET", "/stock/p", null, 200).path("cantidadDisponible").asInt());
    assertEquals(
        "ASIGNADA", call("GET", "/asignaciones/" + assignment, null, 200).path("estado").asText());
    call("POST", "/entregas", Map.of("id", p), 200);
    assertEquals(1, satisfactions.get());
    assertEquals(
        "COMPLETADA",
        call("GET", "/asignaciones/" + assignment, null, 200).path("estado").asText());
    call("POST", "/entregas", Map.of("id", p), 409);
    call("POST", "/internal/matchmaking/resultados", callback, 200);
    assertEquals(1, call("GET", "/asignaciones", null, 200).size());
    assertEquals(1, satisfactions.get());
  }

  @Test
  void crearDepositoRechazaInventarioInyectadoYAlgoritmoInicial() throws Exception {
    for (int cantidad : List.of(-100, 100)) {
      call(
          "POST",
          "/depositos",
          Map.of(
              "nombre", "Depósito inválido",
              "direccion", "Medrano",
              "capacidadMaxima", 1,
              "stockActual", List.of(Map.of(
                  "donacionID", "donacion-no-registrada", "producto", "p", "cantidad", cantidad))),
          400);
    }
    call(
        "POST", "/depositos",
        Map.of("nombre", "Depósito", "direccion", "Medrano", "capacidadMaxima", 100,
            "algoritmo", "SUB_ATENDIDOS"),
        400);
    assertEquals(0, call("GET", "/depositos", null, 200).size());
    assertEquals(0, call("GET", "/admin/db/status", null, 200).path("paquetes").asInt());

    var creado = call(
        "POST", "/depositos",
        Map.of("nombre", "Depósito", "direccion", "Medrano", "capacidadMaxima", 100,
            "stockActual", List.of()),
        200);
    assertTrue(creado.path("algoritmo").isNull());
    assertEquals(0, creado.path("stockActual").size());
  }

  @Test
  void partialRemoteFailureRetriesTheSameDeliveryWithoutDuplicatingUnits() throws Exception {
    String d = depot(), p = pack(d, "failure-donation", 3);
    var a =
        call("POST", "/internal/matchmaking/resultados", result(d, p, "n", 3, 0), 200)
            .path("id")
            .asText();
    failAcceptance.set(true);
    call("POST", "/entregas", Map.of("id", p), 502);
    assertEquals(3, receivedUnits.get());
    assertEquals("ASIGNADA", call("GET", "/asignaciones/" + a, null, 200).path("estado").asText());
    call("POST", "/entregas", Map.of("id", p), 200);
    assertEquals(3, receivedUnits.get());
    assertEquals(1, satisfactions.get());
    assertEquals(2, attemptedKeys.size());
    assertEquals(attemptedKeys.get(0), attemptedKeys.get(1));
    assertEquals(
        "COMPLETADA", call("GET", "/asignaciones/" + a, null, 200).path("estado").asText());
  }

  @Test
  void batchRetryKeepsIdentityEvenWhenPackageOrderChanges() throws Exception {
    String d = depot(), a = pack(d, "batch-a", 4), b = pack(d, "batch-b", 6);
    call("POST", "/internal/matchmaking/resultados", result(d, a, "n", 4, 0), 200);
    call("POST", "/internal/matchmaking/resultados", result(d, b, "n", 6, 0), 200);
    failAcceptance.set(true);
    call("POST", "/entregas/lote", Map.of("paqueteIds", List.of(a, b)), 502);
    call("POST", "/entregas/lote", Map.of("paqueteIds", List.of(b, a)), 200);
    assertEquals(10, receivedUnits.get());
    assertEquals(1, satisfactions.get());
    assertEquals(attemptedKeys.get(0), attemptedKeys.get(1));
  }

  @Test
  void reservaVariosPaquetesYEntregaLoteUnaSolaVez() throws Exception {
    String d = depot(), a = pack(d, "a", 4), b = pack(d, "b", 6);
    call("POST", "/internal/matchmaking/resultados", result(d, a, null, 0, 4), 204);
    call("POST", "/internal/matchmaking/resultados", result(d, b, null, 0, 6), 204);
    var assignments =
        call(
            "POST",
            "/stock/asignaciones",
            Map.of("necesidadId", "n", "productoId", "p", "cantidad", 10),
            200);
    assertEquals(2, assignments.size());
    assertEquals(0, satisfactions.get());
    var ids = new ArrayList<String>();
    assignments.forEach(item -> ids.add(item.path("paqueteID").asText()));
    call("POST", "/entregas/lote", Map.of("paqueteIds", ids), 200);
    assertEquals(1, satisfactions.get());
    call("POST", "/entregas/lote", Map.of("paqueteIds", ids), 409);
    assertEquals(0, call("GET", "/stock/p", null, 200).path("cantidadDisponible").asInt());
  }

  @Test
  void weeklyAndMonthlyReservationsSurviveUntilDeliveryAndCountInTheDeliveryPeriod()
      throws Exception {
    var today = java.time.LocalDate.now();
    for (var start :
        List.of(
            today.with(
                java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
            today.withDayOfMonth(1))) {
      periodStart.set(start);
      String d = depot(), a = pack(d, "carry-a-" + d, 4), b = pack(d, "carry-b-" + d, 6);
      String n = "recurring-" + d;
      call("POST", "/internal/matchmaking/resultados", result(d, a, n, 4, 0), 200);
      call("POST", "/internal/matchmaking/resultados", result(d, b, n, 6, 0), 200);
      jdbc.update(
          "update asignaciones set fecha=? where necesidad_id=?",
          java.sql.Timestamp.valueOf(start.minusDays(1).atTime(12, 0)),
          n);
      String quantity = "/internal/matchmaking/necesidades/" + n + "/cantidad-asignada";
      assertEquals(10, call("GET", quantity, null, 200).asInt());
      call("POST", "/entregas/lote", Map.of("paqueteIds", List.of(a, b)), 200);
      assertEquals(10, call("GET", quantity, null, 200).asInt());
      call("POST", "/entregas/lote", Map.of("paqueteIds", List.of(a, b)), 409);
      periodStart.set(today.plusDays(1));
      assertEquals(0, call("GET", quantity, null, 200).asInt());

      periodStart.set(start);
      String single = pack(d, "carry-single-" + d, 10), singleNeed = n + "-single";
      call("POST", "/internal/matchmaking/resultados", result(d, single, singleNeed, 10, 0), 200);
      jdbc.update(
          "update asignaciones set fecha=? where necesidad_id=?",
          java.sql.Timestamp.valueOf(start.minusDays(1).atTime(12, 0)),
          singleNeed);
      call("POST", "/entregas", Map.of("id", single), 200);
      assertEquals(
          10,
          call(
                  "GET",
                  "/internal/matchmaking/necesidades/" + singleNeed + "/cantidad-asignada",
                  null,
                  200)
              .asInt());
    }
  }

  @Test
  void abmRechazaPerderStockYValidaCampos() throws Exception {
    call(
        "POST",
        "/depositos",
        Map.of("nombre", "", "direccion", "Medrano", "capacidadMaxima", 100),
        400);
    call("GET", "/depositos/99999", null, 404);
    String d = depot();
    call(
        "PUT",
        "/depositos/" + d,
        Map.of("nombre", "Otro", "direccion", "Campus", "capacidadMaxima", 50),
        200);
    pack(d, "a", 10);
    call(
        "PUT",
        "/depositos/" + d,
        Map.of("nombre", "Otro", "direccion", "Campus", "capacidadMaxima", 5),
        409);
    call("DELETE", "/depositos/" + d, null, 409);
    assertEquals(1, call("GET", "/depositos", null, 200).size());
    assertEquals(1, call("GET", "/admin/db/status", null, 200).path("paquetes").asInt());
    String empty = depot();
    call("DELETE", "/depositos/" + empty, null, 200);
    call(
        "POST",
        "/stock/asignaciones",
        Map.of("necesidadId", "n", "productoId", "otro", "cantidad", 1),
        400);
    call(
        "POST",
        "/stock/asignaciones",
        Map.of("necesidadId", "n", "productoId", "p", "cantidad", 0),
        400);
  }

  @Test
  void publicaMetricasOpenapiYNoFiltraMdc() throws Exception {
    depot();
    var response = mvc.perform(get("/actuator/prometheus")).andReturn().getResponse();
    assertEquals(200, response.getStatus());
    assertTrue(response.getContentAsString().contains("logistica_deposito_capacidad"));
    assertEquals(200, mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getStatus());
    assertNull(org.slf4j.MDC.get("traceId"));
  }

  @Test
  void erroresDelClienteSon400YLaTrazaNoContaminaElHilo() throws Exception {
    call("GET", "/depositos/no-numerico", null, 400);
    assertEquals(
        400,
        mvc.perform(post("/depositos").contentType(MediaType.APPLICATION_JSON).content("{invalido"))
            .andReturn()
            .getResponse()
            .getStatus());
    org.slf4j.MDC.put("contextoAnterior", "anterior");
    try {
      var response =
          mvc.perform(get("/depositos").header("X-Trace-Id", "traza con espacios"))
              .andReturn()
              .getResponse();
      assertEquals(200, response.getStatus());
      assertNotEquals("traza con espacios", response.getHeader("X-Trace-Id"));
      assertEquals("anterior", org.slf4j.MDC.get("contextoAnterior"));
      assertNull(org.slf4j.MDC.get("traceId"));
    } finally {
      org.slf4j.MDC.clear();
    }
  }
}
