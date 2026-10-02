package ar.edu.utn.dds.k3003.metrics;

import ar.edu.utn.dds.k3003.Fachada;
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

@Component
@org.springframework.context.annotation.Profile("!worker")
public class StockMetrics {
  public StockMetrics(MeterRegistry registry, Fachada fachada) {
    Gauge.builder("logistica.paquetes.pendientes", fachada, Fachada::paquetesPendientes)
        .description("Paquetes que esperan al worker").register(registry);
    Gauge.builder("logistica.deposito.ocupacion", fachada, Fachada::ocupacionTotal)
        .description("Unidades aún presentes en depósitos").register(registry);
    Gauge.builder("logistica.deposito.capacidad", fachada, Fachada::capacidadTotal)
        .description("Capacidad total de depósitos").register(registry);
  }
}
