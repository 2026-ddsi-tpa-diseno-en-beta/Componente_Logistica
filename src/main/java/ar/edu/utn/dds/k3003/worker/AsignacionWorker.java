package ar.edu.utn.dds.k3003.worker;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;
import ar.edu.utn.dds.k3003.integration.FachadaDonadoresYEntidadesHttp;
import ar.edu.utn.dds.k3003.messaging.RabbitConfiguration;
import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;
import ar.edu.utn.dds.k3003.model.algoritmos.ResultadoMatchmaking;
import ar.edu.utn.dds.k3003.metrics.WorkerMetrics;
import ar.edu.utn.dds.k3003.observability.InstanceInfo;
import ar.edu.utn.dds.k3003.observability.TraceContext;
import ar.edu.utn.dds.k3003.services.MatchmakingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.UUID;

@Component
@Profile("worker")
public class AsignacionWorker {

    private static final Logger log = LoggerFactory.getLogger(AsignacionWorker.class);

    private final FachadaDonadoresYEntidadesHttp donadoresClient;
    private final MatchmakingService matchmakingService;
    private final LogisticaInternalClient logisticaClient;
    private final WorkerMetrics metrics;
    private final InstanceInfo instanceInfo;

    public AsignacionWorker(
        FachadaDonadoresYEntidadesHttp donadoresClient,
        MatchmakingService matchmakingService,
        LogisticaInternalClient logisticaClient,
        WorkerMetrics metrics,
        InstanceInfo instanceInfo
    ) {
        this.donadoresClient = donadoresClient;
        this.matchmakingService = matchmakingService;
        this.logisticaClient = logisticaClient;
        this.metrics = metrics;
        this.instanceInfo = instanceInfo;
    }

    @RabbitListener(queues = RabbitConfiguration.QUEUE)
    public void procesar(DonacionPendienteMessage message) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        String traceId = message.traceId();
        if (traceId == null || !traceId.matches("[A-Za-z0-9_-]{1,128}")) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(TraceContext.TRACE_ID, traceId);
        MDC.put(TraceContext.INSTANCE_ID, instanceInfo.getInstanceId());
        MDC.put(TraceContext.COMPONENT, instanceInfo.getComponent());
        MDC.put(TraceContext.WORKER_ID, instanceInfo.getInstanceId());

        long start = System.nanoTime();
        metrics.intento();
        MDC.put("donacionId", message.donacionId()); MDC.put("paqueteId", message.paqueteId());
        MDC.put("event", "matchmaking.recibido");
        try {
            log.info(
                "matchmaking.recibido paquete={} donacion={} producto={} algoritmo={}",
                message.paqueteId(),
                message.donacionId(),
                message.productoId(),
                message.algoritmo()
            );

            List<NecesidadMaterialDTO> necesidades =
                donadoresClient.obtenerNecesidadesInsatisfechasDe(
                    message.productoId()
                );

            List<NecesidadMaterialDTO> necesidadesSeguras =
                necesidades == null ? List.of() : necesidades;

            Map<String, Integer> cantidadesSatisfechas = new HashMap<>();
            necesidadesSeguras.stream()
                .filter(n -> n != null && n.id() != null)
                .forEach(n -> cantidadesSatisfechas.put(
                    n.id(),
                    logisticaClient.cantidadAsignada(n.id())
                ));

            ResultadoMatchmaking resultado = matchmakingService.procesar(
                message.algoritmo(),
                necesidadesSeguras,
                message.productoId(),
                message.cantidad(),
                cantidadesSatisfechas
            );

            String necesidadId = resultado.necesidad()
                .map(n -> n.id())
                .orElse(null);

            logisticaClient.registrarResultado(
                new ResultadoMatchmakingRequest(
                    message.depositoId(),
                    message.paqueteId(),
                    necesidadId,
                    resultado.cantidadAsignada(),
                    resultado.cantidadSobrante()
                )
            );

            metrics.callbackExitoso();
            metrics.mensajeProcesado();
            metrics.resultado(necesidadId != null, !necesidadesSeguras.isEmpty());
            MDC.put("event", "matchmaking.finalizado"); MDC.put("outcome", necesidadId == null ? "sin_asignacion" : "asignado");
            if (necesidadId != null) MDC.put("necesidadId", necesidadId);
            MDC.put("quantity", String.valueOf(resultado.cantidadAsignada()));
            MDC.put("durationMs", String.valueOf((System.nanoTime() - start) / 1_000_000));
            log.info(
                "matchmaking.finalizado paquete={} necesidad={} asignada={} sobrante={}",
                message.paqueteId(),
                necesidadId,
                resultado.cantidadAsignada(),
                resultado.cantidadSobrante()
            );
        } catch (RuntimeException ex) {
            metrics.error();
            MDC.put("event", "matchmaking.error"); MDC.put("outcome", "error");
            MDC.put("reason", ex.getClass().getSimpleName());
            MDC.put("durationMs", String.valueOf((System.nanoTime() - start) / 1_000_000));
            log.error(
                "matchmaking.error paquete={} donacion={}",
                message.paqueteId(),
                message.donacionId(),
                ex
            );
            throw ex;
        } finally {
            metrics.duracion(System.nanoTime() - start);
            MDC.clear();
            if (previous != null) MDC.setContextMap(previous);
        }
    }
}
