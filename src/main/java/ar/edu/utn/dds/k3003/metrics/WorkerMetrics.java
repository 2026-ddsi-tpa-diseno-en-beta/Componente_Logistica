package ar.edu.utn.dds.k3003.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class WorkerMetrics {

  private final Counter mensajesProcesados;
  private final Counter errores;
  private final Counter callbacksExitosos;

  public WorkerMetrics(MeterRegistry registry, String workerId) {

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
