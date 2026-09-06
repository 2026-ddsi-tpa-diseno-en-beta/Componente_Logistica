package ar.edu.utn.dds.k3003.observability;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import java.io.IOException;

public class TracePropagationInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    String traceId = TraceContext.currentTraceId();
    if (traceId != null && !traceId.isBlank()) {
      request.getHeaders().set(TraceContext.TRACE_ID_HEADER, traceId);
    }
    return execution.execute(request, body);
  }
}
