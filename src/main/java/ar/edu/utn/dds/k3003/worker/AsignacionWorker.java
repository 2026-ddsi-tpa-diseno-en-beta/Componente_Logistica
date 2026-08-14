package ar.edu.utn.dds.k3003.worker;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;
import ar.edu.utn.dds.k3003.integration.FachadaDonadoresYEntidadesHttp;
import ar.edu.utn.dds.k3003.messaging.RabbitConfiguration;
import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;
import ar.edu.utn.dds.k3003.model.algoritmos.ResultadoMatchmaking;
import ar.edu.utn.dds.k3003.services.MatchmakingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile("worker")
public class AsignacionWorker {

    private final FachadaDonadoresYEntidadesHttp donadoresClient;
    private final MatchmakingService matchmakingService;
    private final LogisticaInternalClient logisticaClient;

    public AsignacionWorker(
        FachadaDonadoresYEntidadesHttp donadoresClient,
        MatchmakingService matchmakingService,
        LogisticaInternalClient logisticaClient
    ) {
        this.donadoresClient = donadoresClient;
        this.matchmakingService = matchmakingService;
        this.logisticaClient = logisticaClient;
    }

    @RabbitListener(queues = RabbitConfiguration.QUEUE)
    public void procesar(DonacionPendienteMessage message) {
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
    }
}
