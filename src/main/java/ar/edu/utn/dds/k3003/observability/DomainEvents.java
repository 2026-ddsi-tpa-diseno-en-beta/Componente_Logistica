package ar.edu.utn.dds.k3003.observability;

import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.MDC;

/** Adds searchable fields to existing domain events without changing their timing or content. */
public final class DomainEvents {
  private static final Pattern FIELD = Pattern.compile("(\\w+)=\\{\\}");
  private static final Map<String,String> NAMES = Map.ofEntries(
      Map.entry("donacion", "donacionId"), Map.entry("donador", "donadorId"),
      Map.entry("paquete", "paqueteId"), Map.entry("necesidad", "necesidadId"),
      Map.entry("mision", "misionId"), Map.entry("cantidad", "quantity"));
  private DomainEvents() {}
  public static void info(Logger logger, String message, Object... arguments) {
    var previous = MDC.getCopyOfContextMap();
    try {
      MDC.put("event", message.split(" ",2)[0]);
      var matcher = FIELD.matcher(message);
      int index = 0;
      while (matcher.find() && index < arguments.length) {
        Object value = arguments[index++];
        if (value instanceof String || value instanceof Number || value instanceof Enum<?>) {
          String name = NAMES.getOrDefault(matcher.group(1), matcher.group(1));
          MDC.put(name, value.toString());
        }
      }
      logger.info(message, arguments);
    } finally { MDC.clear(); if (previous != null) MDC.setContextMap(previous); }
  }
}
