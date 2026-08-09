package ar.edu.utn.dds.k3003.worker;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;
import ar.edu.utn.dds.k3003.integration.FachadaDonadoresYEntidadesHttp;
import ar.edu.utn.dds.k3003.messaging.RabbitConfiguration;
import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;
import ar.edu.utn.dds.k3003.model.algoritmos.ResultadoMatchmaking;
import ar.edu.utn.dds.k3003.services.MatchmakingService;

import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
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

        ResultadoMatchmaking resultado = matchmakingService.procesar(
            message.algoritmo(),
            necesidades == null ? List.of() : necesidades,
            message.productoId(),
            message.cantidad()
        );

        String necesidadId = resultado.necesidad()
            .map(NecesidadMaterialDTO::id)
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
