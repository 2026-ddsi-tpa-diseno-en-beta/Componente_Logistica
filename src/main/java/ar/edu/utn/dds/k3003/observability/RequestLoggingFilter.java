package ar.edu.utn.dds.k3003.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  private final InstanceInfo instanceInfo;

  public RequestLoggingFilter(InstanceInfo instanceInfo) {
    this.instanceInfo = instanceInfo;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri.startsWith("/actuator") || uri.equals("/health");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain)
      throws ServletException, IOException {

    String traceId = request.getHeader(TraceContext.TRACE_ID_HEADER);
    if (traceId == null || !traceId.matches("[A-Za-z0-9._-]{1,128}")) {
      traceId = UUID.randomUUID().toString();
    }

    String requestId = UUID.randomUUID().toString();

    java.util.Map<String, String> previous = MDC.getCopyOfContextMap();
    MDC.put(TraceContext.TRACE_ID, traceId);
    MDC.put(TraceContext.REQUEST_ID, requestId);
    MDC.put(TraceContext.INSTANCE_ID, instanceInfo.getInstanceId());
    MDC.put(TraceContext.COMPONENT, instanceInfo.getComponent());

    long start = System.currentTimeMillis();

    log.info("--> {} {}", request.getMethod(), request.getRequestURI());

    boolean failed = false;
    try {
      response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);
      chain.doFilter(request, response);
    } catch (IOException | ServletException | RuntimeException ex) {
      failed = true;
      throw ex;
    } finally {
      long took = System.currentTimeMillis() - start;
      int status = failed ? 500 : response.getStatus();
      MDC.put("event", "http.finalizado"); MDC.put("status", String.valueOf(status));
      MDC.put("durationMs", String.valueOf(took));
      MDC.put("outcome", status >= 500 ? "error" : status >= 400 ? "rechazada" : "ok");
      log.info(
          "<-- {} {} status={} took={}ms",
          request.getMethod(),
          request.getRequestURI(),
          status,
          took);
      if (previous == null) MDC.clear(); else MDC.setContextMap(previous);
    }
  }
}
