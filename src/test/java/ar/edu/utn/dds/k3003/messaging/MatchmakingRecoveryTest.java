package ar.edu.utn.dds.k3003.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.utn.dds.k3003.Fachada;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.*;
import ar.edu.utn.dds.k3003.controllers.MatchmakingRecoveryController;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MatchmakingRecoveryTest {
  @Test
  void recoveryQueuesOnlyPendingPackagesWithoutDuplicatingDomainRecords() {
    var facade = new Fachada();
    var donors = mock(ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonadoresYEntidades.class);
    when(donors.obtenerNecesidadesInsatisfechasDe(any())).thenReturn(java.util.List.of());
    facade.setFachadaDonadoresYEntidades(donors);
    var outbox = mock(MatchmakingOutbox.class);
    ReflectionTestUtils.setField(facade, "matchmakingOutbox", outbox);
    var d =
        facade.agregarDeposito(new DepositoDTO(null, null, "Prueba", "Medrano", 100, null)).id();
    facade.setAlgoritmoMM(d, TipoAlgoritmoEnum.SUB_ATENDIDOS);
    var pending = facade.gestionarDonacion(d, "d1", "p", 3).stockActual().getFirst().id();
    var inStock = facade.gestionarDonacion(d, "d2", "p", 2).stockActual().getLast().id();
    facade.registrarResultadoMatchmaking(new ResultadoMatchmakingRequest(d, inStock, null, 0, 2));
    assertEquals(1, facade.recuperarMatchmakingPendiente());
    assertEquals(1, facade.recuperarMatchmakingPendiente());
    verify(outbox, times(2))
        .recuperar(argThat(m -> m.paqueteId().equals(pending) && m.cantidad() == 3));
    assertEquals(2, facade.buscarDepositoPorID(d).stockActual().size());
  }

  @Test
  void recoveryIsProtectedAndDoesNotPublishWhenKeyIsMissing() {
    var facade = mock(Fachada.class);
    assertEquals(
        403, new MatchmakingRecoveryController(facade, "").recover("").getStatusCode().value());
    var controller = new MatchmakingRecoveryController(facade, "test-key");
    assertEquals(403, controller.recover("wrong").getStatusCode().value());
    verifyNoInteractions(facade);
    when(facade.recuperarMatchmakingPendiente()).thenReturn(2);
    assertEquals(2, controller.recover("test-key").getBody().get("paquetes"));
  }
}
