package ar.edu.utn.dds.k3003.observability;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.*;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;

public class TracePropagationInterceptor implements ClientHttpRequestInterceptor {
  private static final Logger log = LoggerFactory.getLogger(TracePropagationInterceptor.class);
  @Override public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    String trace = MDC.get("traceId");
    if (trace != null) request.getHeaders().set("X-Trace-Id", trace);
    long start = System.nanoTime();
    String status = "error";
    Map<String,String> previous = MDC.getCopyOfContextMap();
    try {
      ClientHttpResponse response = execution.execute(request, body);
      status = String.valueOf(response.getStatusCode().value());
      return response;
    } finally {
      long elapsed = System.nanoTime() - start;
      // Configured service host only: never tag full URLs, IDs, query strings or bodies.
      String destination = request.getURI().getHost();
      Timer.builder("donatrack.integraciones.duracion").tags("destino", destination,
          "metodo", request.getMethod().name(), "status", status)
          .publishPercentileHistogram().register(Metrics.globalRegistry).record(elapsed, TimeUnit.NANOSECONDS);
      MDC.put("event", "integracion.finalizada"); MDC.put("destination", destination);
      MDC.put("status", status); MDC.put("durationMs", String.valueOf(elapsed / 1_000_000));
      MDC.put("outcome", status.equals("error") || status.startsWith("5") ? "error" : status.startsWith("4") ? "rechazada" : "ok");
      if (status.equals("error") || status.startsWith("5")) log.error("integracion.finalizada destino={} status={}", destination, status);
      else log.info("integracion.finalizada destino={} status={}", destination, status);
      MDC.clear(); if (previous != null) MDC.setContextMap(previous);
  }
}
}
