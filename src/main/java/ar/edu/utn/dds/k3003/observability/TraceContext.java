package ar.edu.utn.dds.k3003.observability;

import org.slf4j.MDC;

public final class TraceContext {

  public static final String TRACE_ID = "traceId";
  public static final String REQUEST_ID = "requestId";
  public static final String INSTANCE_ID = "instanceId";
  public static final String COMPONENT = "component";
  public static final String WORKER_ID = "workerId";
  public static final String TRACE_ID_HEADER = "X-Trace-Id";

  private TraceContext() {}

  public static String currentTraceId() {
    return MDC.get(TRACE_ID);
  }

  public static void put(String key, String value) {
    if (value != null && !value.isBlank()) {
      MDC.put(key, value);
    }
  }
}
