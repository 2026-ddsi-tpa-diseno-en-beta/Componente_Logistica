package ar.edu.utn.dds.k3003.integration;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConsultaNecesidadHttpTest {
  @Test void periodComesFromTheSameSnapshotIncludingExplicitNull() throws Exception {
    checkSnapshot(",\"inicioPeriodo\":\"2026-11-01\"", LocalDate.of(2026, 11, 1), 0);
    checkSnapshot(",\"inicioPeriodo\":null", null, 0);
  }

  @Test void oldServerWithoutPeriodFieldKeepsTheCompatibilityEndpoint() throws Exception {
    checkSnapshot("", LocalDate.of(2026, 1, 1), 1);
  }

  private void checkSnapshot(String field, LocalDate expected, int expectedFallbacks) throws Exception {
    var fallbacks = new AtomicInteger(); var snapshots = new AtomicInteger();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      String output;
      if (exchange.getRequestURI().getPath().endsWith("/periodo")) {
        fallbacks.incrementAndGet(); output = "{\"inicio\":\"2026-01-01\"}";
      } else {
        snapshots.incrementAndGet();
        output = "{\"id\":\"n\",\"entidadID\":\"e\",\"productoSolicitadoID\":\"p\","
            + "\"nivelDeUrgencia\":1,\"cantidadObjetivo\":10,\"cantidadAsignada\":4,"
            + "\"descripcion\":\"necesidad\",\"tipo\":\"RECURRENTE\"" + field + "}";
      }
      var bytes = output.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
    });
    server.start();
    try {
      var snapshot = new FachadaDonadoresYEntidadesHttp("http://127.0.0.1:" + server.getAddress().getPort()).consultarNecesidad("n");
      assertEquals(expected, snapshot.inicio()); assertEquals(4, snapshot.recibida());
      assertEquals(1, snapshots.get()); assertEquals(expectedFallbacks, fallbacks.get());
    } finally { server.stop(0); }
  }
}
