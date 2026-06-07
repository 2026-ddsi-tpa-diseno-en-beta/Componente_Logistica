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

    public LogisticaMetrics(MeterRegistry registry) {

        depositosCreados = Counter.builder("logistica.depositos.creados")
                .description("Cantidad de depositos creados")
                .register(registry);

        donacionesGestionadas = Counter.builder("logistica.donaciones.gestionadas")
                .description("Cantidad de donaciones gestionadas")
                .register(registry);

        entregasReportadas = Counter.builder("logistica.entregas.reportadas")
                .description("Cantidad de entregas reportadas")
                .register(registry);

        errores = Counter.builder("logistica.errores")
                .description("Cantidad de errores")
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
}