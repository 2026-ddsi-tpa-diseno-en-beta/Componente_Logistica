package ar.edu.utn.dds.k3003.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class WorkerMetrics {

  private final Counter mensajesProcesados;
  private final Counter intentos;
  private final io.micrometer.core.instrument.Timer duracion;
  private final MeterRegistry registry;
  private final String workerId;
  private final Counter errores;
  private final Counter callbacksExitosos;

  public WorkerMetrics(
    MeterRegistry registry,
    @Value("${worker.id:worker-local}") String workerId
  ) {
    this.registry = registry; this.workerId = workerId;
    intentos = registry.counter("logistica.worker.intentos", "worker_id", workerId);
    duracion = io.micrometer.core.instrument.Timer.builder("logistica.worker.duracion").tag("worker_id", workerId).publishPercentileHistogram().register(registry);
    mensajesProcesados =
        Counter.builder("logistica.worker.mensajes.procesados")
            .description("Mensajes de matchmaking procesados por el worker")
            .tag("worker_id", workerId)
            .register(registry);

    errores =
        Counter.builder("logistica.worker.errores")
            .description("Errores al procesar mensajes de matchmaking")
            .tag("worker_id", workerId)
            .register(registry);

    callbacksExitosos =
        Counter.builder("logistica.worker.callbacks.exitosos")
            .description("Callbacks exitosos enviados a Logística")
            .tag("worker_id", workerId)
            .register(registry);
  }

  public void intento() { intentos.increment(); }
  public void duracion(long nanos) { duracion.record(nanos, java.util.concurrent.TimeUnit.NANOSECONDS); }
  public void resultado(boolean asignado, boolean hayNecesidades) {
    registry.counter("logistica.worker.resultados", "worker_id", workerId, "resultado",
        asignado ? "asignado" : hayNecesidades ? "sin_elegibles" : "sin_necesidades").increment();
  }
  public void mensajeProcesado() {
    mensajesProcesados.increment();
  }

  public void error() {
    errores.increment();
  }

  public void callbackExitoso() {
    callbacksExitosos.increment();
  }
}
