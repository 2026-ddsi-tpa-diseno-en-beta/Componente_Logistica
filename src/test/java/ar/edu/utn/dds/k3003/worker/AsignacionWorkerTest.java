package ar.edu.utn.dds.k3003.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.TipoNecesidadMaterialEnum;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;
import ar.edu.utn.dds.k3003.integration.FachadaDonadoresYEntidadesHttp;
import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;
import ar.edu.utn.dds.k3003.metrics.WorkerMetrics;
import ar.edu.utn.dds.k3003.observability.InstanceInfo;
import ar.edu.utn.dds.k3003.services.MatchmakingService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AsignacionWorkerTest {
  @Test void errorDelWorkerSePropagaSinPerderElContextoPrevio() {
    var donadores = mock(FachadaDonadoresYEntidadesHttp.class);
    var metrics = mock(WorkerMetrics.class);
    when(donadores.obtenerNecesidadesInsatisfechasDe("p"))
        .thenThrow(new IllegalStateException("API no disponible"));
    var worker = new AsignacionWorker(donadores, new MatchmakingService(),
        mock(LogisticaInternalClient.class), metrics, new InstanceInfo("worker-2", "worker"));
    org.slf4j.MDC.put("traceId", "contexto-previo");
    try {
      org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> worker.procesar(
          new DonacionPendienteMessage("d", "p1", "donacion", "p", 10,
              ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum.SUB_ATENDIDOS)));
      verify(metrics).error();
      verify(metrics, never()).callbackExitoso();
      org.junit.jupiter.api.Assertions.assertEquals("contexto-previo", org.slf4j.MDC.get("traceId"));
    } finally { org.slf4j.MDC.clear(); }
  }

  @Test
  void procesaMensajeYPublicaResultado() {
    FachadaDonadoresYEntidadesHttp donadores = mock(FachadaDonadoresYEntidadesHttp.class);
    MatchmakingService matchmaking = new MatchmakingService();
    LogisticaInternalClient logistica = mock(LogisticaInternalClient.class);
    WorkerMetrics metrics = mock(WorkerMetrics.class);
    InstanceInfo instanceInfo = new InstanceInfo("worker-test", "worker");

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
        new AsignacionWorker(donadores, matchmaking, logistica, metrics, instanceInfo);

    worker.procesar(
        new DonacionPendienteMessage(
            "deposito1", "paquete1", "donacion1", "producto1", 10,
            ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum.SUB_ATENDIDOS));

    verify(logistica).cantidadAsignada("necesidad1");
    verify(logistica).registrarResultado(any(ResultadoMatchmakingRequest.class));
    verify(metrics).callbackExitoso();
    verify(metrics).mensajeProcesado();
  }
}
