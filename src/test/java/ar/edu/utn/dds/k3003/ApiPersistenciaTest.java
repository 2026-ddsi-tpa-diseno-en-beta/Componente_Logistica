package ar.edu.utn.dds.k3003;

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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Exercises HTTP controllers, real JPA mappings and outbound HTTP with isolated H2. */
@SpringBootTest(classes=ar.edu.utn.dds.k3003.app.Application.class, properties={
    "spring.config.location=classpath:application-integration.properties"})
@AutoConfigureMockMvc
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
class ApiPersistenciaTest {
  static final HttpServer remote=startRemote();
  static final AtomicInteger satisfactions=new AtomicInteger();
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  static HttpServer startRemote() {
    try {
      HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
      server.createContext("/",exchange->{
        String path=exchange.getRequestURI().getPath();
        String output;
        if(path.endsWith("/periodo")) output="{\"inicio\":null}";
        else if(path.endsWith("/satisfaccion")) {
          satisfactions.incrementAndGet();output="{}";
        } else if(path.startsWith("/donaciones/")) output="{\"id\":\"d\",\"estado\":\"ACEPTADA\"}";
        else output="[]";
        byte[] bytes=output.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type","application/json");
        exchange.sendResponseHeaders(200,bytes.length);
        exchange.getResponseBody().write(bytes);exchange.close();
      });
      server.start();return server;
    } catch(Exception ex){throw new IllegalStateException(ex);}
  }
  @DynamicPropertySource static void urls(DynamicPropertyRegistry properties) {
    String url="http://127.0.0.1:"+remote.getAddress().getPort();
    properties.add("integrations.donadores-url",()->url);
    properties.add("integrations.donaciones-url",()->url);
  }
  @BeforeEach void clear() throws Exception {call("DELETE","/admin/db",null,204);satisfactions.set(0);}
  @AfterAll static void close(){remote.stop(0);}
  JsonNode call(String method,String path,Object body,int status)throws Exception {
    var request=request(org.springframework.http.HttpMethod.valueOf(method),path)
        .header("X-Trace-Id","prueba-jpa").contentType(MediaType.APPLICATION_JSON);
    if(body!=null)request.content(json.writeValueAsString(body));
    var result=mvc.perform(request).andReturn();
    assertEquals(status,result.getResponse().getStatus(),result.getResponse().getContentAsString());
    assertEquals("prueba-jpa",result.getResponse().getHeader("X-Trace-Id"));
    String content=result.getResponse().getContentAsString();
    return content.isBlank()?json.nullNode():json.readTree(content);
  }
  String depot() throws Exception {
    String id=call("POST","/depositos",Map.of("nombre","Depósito","direccion","Medrano","capacidadMaxima",100),200).path("id").asText();
    call("PATCH","/depositos/"+id+"/algoritmo",Map.of("algoritmo","SUB_ATENDIDOS"),200);
    return id;
  }
  String pack(String depot,String donation,int quantity)throws Exception {
    var stock=call("POST","/depositos/"+depot+"/donacion",Map.of("donacionID",donation,"productoID","p","cantidad",quantity),202).path("stockActual");
    return stock.get(stock.size()-1).path("id").asText();
  }
  Object result(String depot,String pack,String need,int quantity,int leftover){
    Map<String,Object> body=new HashMap<>();body.put("depositoId",depot);body.put("paqueteId",pack);
    body.put("necesidadId",need);body.put("cantidadAsignada",quantity);body.put("cantidadSobrante",leftover);return body;
  }
  @Test void conservaStockAsignacionesHistorialYEntregaSinDuplicados()throws Exception {
    String d=depot(),p=pack(d,"donacion1",10);
    var callback=result(d,p,"n",4,6);
    String assignment=call("POST","/internal/matchmaking/resultados",callback,200).path("id").asText();
    call("POST","/internal/matchmaking/resultados",callback,200);
    assertEquals(4,call("GET","/internal/matchmaking/necesidades/n/cantidad-asignada",null,200).asInt());
    assertEquals(6,call("GET","/stock/p",null,200).path("cantidadDisponible").asInt());
    assertEquals("ASIGNADA",call("GET","/asignaciones/"+assignment,null,200).path("estado").asText());
    call("POST","/entregas",Map.of("id",p),200);
    assertEquals(1,satisfactions.get());
    assertEquals("COMPLETADA",call("GET","/asignaciones/"+assignment,null,200).path("estado").asText());
    call("POST","/entregas",Map.of("id",p),409);
    call("POST","/internal/matchmaking/resultados",callback,200);
    assertEquals(1,call("GET","/asignaciones",null,200).size());
    assertEquals(1,satisfactions.get());
  }
  @Test void reservaVariosPaquetesYEntregaLoteUnaSolaVez()throws Exception {
    String d=depot(),a=pack(d,"a",4),b=pack(d,"b",6);
    call("POST","/internal/matchmaking/resultados",result(d,a,null,0,4),204);
    call("POST","/internal/matchmaking/resultados",result(d,b,null,0,6),204);
    var assignments=call("POST","/stock/asignaciones",Map.of("necesidadId","n","productoId","p","cantidad",10),200);
    assertEquals(2,assignments.size());assertEquals(0,satisfactions.get());
    var ids=new ArrayList<String>();assignments.forEach(item->ids.add(item.path("paqueteID").asText()));
    call("POST","/entregas/lote",Map.of("paqueteIds",ids),200);
    assertEquals(1,satisfactions.get());
    call("POST","/entregas/lote",Map.of("paqueteIds",ids),409);
    assertEquals(0,call("GET","/stock/p",null,200).path("cantidadDisponible").asInt());
  }
  @Test void abmRechazaPerderStockYValidaCampos()throws Exception {
    call("POST","/depositos",Map.of("nombre","","direccion","Medrano","capacidadMaxima",100),400);
    call("GET","/depositos/99999",null,404);
    String d=depot();
    call("PUT","/depositos/"+d,Map.of("nombre","Otro","direccion","Campus","capacidadMaxima",50),200);
    pack(d,"a",10);
    call("PUT","/depositos/"+d,Map.of("nombre","Otro","direccion","Campus","capacidadMaxima",5),409);
    call("DELETE","/depositos/"+d,null,409);
    assertEquals(1,call("GET","/depositos",null,200).size());
    assertEquals(1,call("GET","/admin/db/status",null,200).path("paquetes").asInt());
    String empty=depot();call("DELETE","/depositos/"+empty,null,200);
    call("POST","/stock/asignaciones",Map.of("necesidadId","n","productoId","otro","cantidad",1),204);
    call("POST","/stock/asignaciones",Map.of("necesidadId","n","productoId","p","cantidad",0),400);
  }
  @Test void publicaMetricasOpenapiYNoFiltraMdc()throws Exception {
    depot();
    var response=mvc.perform(get("/actuator/prometheus")).andReturn().getResponse();
    assertEquals(200,response.getStatus());assertTrue(response.getContentAsString().contains("logistica_deposito_capacidad"));
    assertEquals(200,mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getStatus());
    assertNull(org.slf4j.MDC.get("traceId"));
  }
  @Test void erroresDelClienteSon400YLaTrazaNoContaminaElHilo()throws Exception {
    call("GET","/depositos/no-numerico",null,400);
    assertEquals(400,mvc.perform(post("/depositos").contentType(MediaType.APPLICATION_JSON)
        .content("{invalido")).andReturn().getResponse().getStatus());
    org.slf4j.MDC.put("contextoAnterior","anterior");
    try {
      var response=mvc.perform(get("/depositos").header("X-Trace-Id","traza con espacios")).andReturn().getResponse();
      assertEquals(200,response.getStatus());
      assertNotEquals("traza con espacios",response.getHeader("X-Trace-Id"));
      assertEquals("anterior",org.slf4j.MDC.get("contextoAnterior"));
      assertNull(org.slf4j.MDC.get("traceId"));
    } finally {org.slf4j.MDC.clear();}
  }
}
