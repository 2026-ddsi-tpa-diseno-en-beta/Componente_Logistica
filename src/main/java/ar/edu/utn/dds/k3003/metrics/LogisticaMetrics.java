package ar.edu.utn.dds.k3003.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class LogisticaMetrics {

    private final Counter depositosCreados;
    private final Counter donacionesGestionadas;
    private final Counter entregasReportadas;
    private final Counter errores;

    private final Counter consultasStock;
    private final Counter asignacionesMatchmaking;
    private final Counter asignacionesSolicitudEntidad;
    private final Counter matchmakingSinAsignacion;

    public LogisticaMetrics(
        MeterRegistry registry
    ) {
        depositosCreados =
            Counter.builder("logistica.depositos.creados")
            .description("Cantidad de depósitos creados")
            .register(registry);

        donacionesGestionadas =
            Counter.builder("logistica.donaciones.gestionadas")
            .description("Cantidad de donaciones recibidas")
            .register(registry);

        entregasReportadas =
            Counter.builder("logistica.entregas.reportadas")
            .description("Cantidad de entregas reportadas")
            .register(registry);

        errores =
            Counter.builder("logistica.errores")
            .description("Cantidad de errores")
            .register(registry);

        consultasStock =
            Counter.builder("logistica.stock.consultas")
            .description("Cantidad de consultas de stock")
            .register(registry);

        asignacionesMatchmaking =
            Counter.builder("logistica.asignaciones.matchmaking")
            .description("Asignaciones realizadas por matchmaking")
            .register(registry);

        asignacionesSolicitudEntidad =
            Counter.builder("logistica.asignaciones.solicitud_entidad")
            .description("Asignaciones solicitadas desde Donadores y Entidades")
            .register(registry);

        matchmakingSinAsignacion =
            Counter.builder("logistica.matchmaking.sin_asignacion")
            .description("Donaciones procesadas sin asignación")
            .register(registry);
    }

    public void depositoCreado() {
        depositosCreados.increment();
    }

    public void donacionGestionada() {
        donacionesGestionadas.increment();
    }

    public void entregaReportada() {
        entregasReportadas.increment();
    }

    public void error() {
        errores.increment();
    }

    public void consultaStock() {
        consultasStock.increment();
    }

    public void asignacionMatchmaking() {
        asignacionesMatchmaking.increment();
    }

    public void asignacionSolicitudEntidad() {
        asignacionesSolicitudEntidad.increment();
    }

    public void matchmakingSinAsignacion() {
        matchmakingSinAsignacion.increment();
    }
}
