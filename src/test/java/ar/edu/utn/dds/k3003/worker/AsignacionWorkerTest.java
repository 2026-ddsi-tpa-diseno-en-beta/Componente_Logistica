package ar.edu.utn.dds.k3003.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.TipoNecesidadMaterialEnum;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;
import ar.edu.utn.dds.k3003.integration.FachadaDonadoresYEntidadesHttp;
import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;
import ar.edu.utn.dds.k3003.services.MatchmakingService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AsignacionWorkerTest {

  @Test
  void procesaMensajeYPublicaResultado() {
    FachadaDonadoresYEntidadesHttp donadores = mock(FachadaDonadoresYEntidadesHttp.class);
    MatchmakingService matchmaking = new MatchmakingService();
    LogisticaInternalClient logistica = mock(LogisticaInternalClient.class);

    when(donadores.obtenerNecesidadesInsatisfechasDe("producto1"))
        .thenReturn(
            List.of(
                new NecesidadMaterialDTO(
                    "necesidad1",
                    "entidad1",
                    5,
                    "desc",
                    10,
                    "producto1",
                    TipoNecesidadMaterialEnum.EXTRAORDINARIA)));
    when(logistica.cantidadAsignada("necesidad1")).thenReturn(0);

    AsignacionWorker worker =
        new AsignacionWorker(donadores, matchmaking, logistica);

    worker.procesar(
        new DonacionPendienteMessage(
            "deposito1", "paquete1", "donacion1", "producto1", 10,
            ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum.SUB_ATENDIDOS));

    verify(logistica).cantidadAsignada("necesidad1");
    verify(logistica).registrarResultado(any(ResultadoMatchmakingRequest.class));
  }
}
