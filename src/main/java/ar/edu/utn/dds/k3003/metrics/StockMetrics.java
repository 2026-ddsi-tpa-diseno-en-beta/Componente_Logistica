package ar.edu.utn.dds.k3003.metrics;

import ar.edu.utn.dds.k3003.Fachada;
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

@Component
@org.springframework.context.annotation.Profile("!worker")
public class StockMetrics {
  private final Fachada fachada;
  private final MultiGauge occupancy;
  private final MultiGauge capacity;
  private volatile Fachada.OperacionSnapshot snapshot;
  private long refreshed;

  public StockMetrics(MeterRegistry registry, Fachada fachada) {
    this.fachada = fachada;
    occupancy = MultiGauge.builder("logistica.depositos.ocupacion").register(registry);
    capacity = MultiGauge.builder("logistica.depositos.capacidad").register(registry);
    Gauge.builder("logistica.asignaciones.unidades.reservadas", () -> current().reservadas()).register(registry);
    Gauge.builder("logistica.asignaciones.pendientes.antiguedad", () -> {
      var oldest = current().oldest();
      return oldest == null ? 0 : Math.max(0, java.time.Duration.between(oldest, java.time.LocalDateTime.now()).toSeconds());
    }).baseUnit("seconds").description("Tiempo desde asignación; reserva no equivale a entrega").register(registry);
    Gauge.builder("logistica.paquetes.pendientes", fachada, Fachada::paquetesPendientes)
        .description("Paquetes que esperan al worker").register(registry);
    Gauge.builder("logistica.deposito.ocupacion", fachada, Fachada::ocupacionTotal)
        .description("Unidades aún presentes en depósitos").register(registry);
    Gauge.builder("logistica.deposito.capacidad", fachada, Fachada::capacidadTotal)
        .description("Capacidad total de depósitos").register(registry);
  }

  private synchronized Fachada.OperacionSnapshot current() {
    if (snapshot == null || System.currentTimeMillis() - refreshed > 10_000) {
      snapshot = fachada.observabilitySnapshot();
      occupancy.register(snapshot.depositos().stream().map(d -> MultiGauge.Row.of(Tags.of("deposito", d.id()), d.ocupacion())).toList(), true);
      capacity.register(snapshot.depositos().stream().map(d -> MultiGauge.Row.of(Tags.of("deposito", d.id()), d.capacidad())).toList(), true);
      refreshed = System.currentTimeMillis();
}
    return snapshot;
  }
}
