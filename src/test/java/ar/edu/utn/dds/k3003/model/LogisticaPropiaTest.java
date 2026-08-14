package ar.edu.utn.dds.k3003.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import ar.edu.utn.dds.k3003.Fachada;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.DonacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.EstadoDonacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.TipoNecesidadMaterialEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsignacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.PaqueteDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonaciones;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonadoresYEntidades;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaLogistica;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogisticaPropiaTest {

  private FachadaLogistica fachada;
  private FachadaDonadoresYEntidades fachadaDonadoresYEntidades;
  private FachadaDonaciones fachadaDonaciones;

  @BeforeEach
  void setUp() {
    fachada = new Fachada();
    fachadaDonadoresYEntidades = mock(FachadaDonadoresYEntidades.class);
    fachadaDonaciones = mock(FachadaDonaciones.class);
    fachada.setFachadaDonadoresYEntidades(fachadaDonadoresYEntidades);
    fachada.setFachadaDonaciones(fachadaDonaciones);
  }

  @Test
  void agregarDepositoYBuscarlo() {
    DepositoDTO creado = crearDeposito();

    assertNotNull(creado.id());
    assertEquals("Deposito A", creado.nombre());

    DepositoDTO buscado = fachada.buscarDepositoPorID(creado.id());
    assertEquals(creado.id(), buscado.id());
    assertEquals("Direccion A", buscado.direccion());
  }

  @Test
  void gestionarDonacionPublicaMensajeSinConsultarNecesidades() {
    DepositoDTO deposito = crearDeposito();

    DepositoDTO resultado =
        fachada.gestionarDonacion(deposito.id(), "donacion1", "producto1", 10);

    assertEquals(deposito.id(), resultado.id());
    assertEquals(1, resultado.stockActual().size());
    assertEquals("donacion1", resultado.stockActual().getFirst().donacionID());
    verifyNoInteractions(fachadaDonadoresYEntidades);
  }

  @Test
  void ejecutarMatchmakingCreaAsignacionYConservaSobrante() {
    DepositoDTO deposito = crearDeposito();
    DepositoDTO conPaquete =
        fachada.gestionarDonacion(deposito.id(), "donacion1", "producto1", 30);
    PaqueteDTO paquete = conPaquete.stockActual().getFirst();

    List<NecesidadMaterialDTO> necesidades =
        List.of(
            new NecesidadMaterialDTO(
                "necesidad1", "entidad1", 5, "desc", 20, "producto1", TipoNecesidadMaterialEnum.EXTRAORDINARIA));

    AsignacionDTO asignacion =
        fachada.ejecutarMatchmaking(conPaquete.id(), paquete, necesidades);

    assertNotNull(asignacion);
    assertEquals("necesidad1", asignacion.necesidadID());
    assertEquals(20, asignacion.cantidadAsignada());
    assertEquals(EstadoAsignacionEnum.ASIGNADA, asignacion.estado());

    DepositoDTO actualizado = fachada.buscarDepositoPorID(deposito.id());
    assertEquals(2, actualizado.stockActual().size());
    assertTrue(
        actualizado.stockActual().stream().anyMatch(p -> p.cantidad() == 10 && p.donacionID().equals("donacion1")));
  }

  @Test
  void registrarResultadoMatchmakingEsIdempotente() {
    DepositoDTO deposito = crearDeposito();
    DepositoDTO conPaquete =
        fachada.gestionarDonacion(deposito.id(), "donacion1", "producto1", 10);
    PaqueteDTO paquete = conPaquete.stockActual().getFirst();

    var request =
        new ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest(
            deposito.id(), paquete.id(), "necesidad1", 10, 0);

    AsignacionDTO primera = fachada.registrarResultadoMatchmaking(request);
    AsignacionDTO segunda = fachada.registrarResultadoMatchmaking(request);

    assertEquals(primera.id(), segunda.id());
    assertEquals(primera.id(), fachada.buscarAsignacionPorPaqueteID(paquete.id()).id());
  }

  @Test
  void reportarEntregaUsaDonacionPersistidaYCompletaAsignacion() {
    DepositoDTO deposito = crearDeposito();
    DepositoDTO conPaquete =
        fachada.gestionarDonacion(deposito.id(), "donacion1", "producto1", 10);
    PaqueteDTO paquete = conPaquete.stockActual().getFirst();

    fachada.registrarResultadoMatchmaking(
        new ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest(
            deposito.id(), paquete.id(), "necesidad1", 10, 0));

    when(fachadaDonadoresYEntidades.satisfacerNecesidad("necesidad1", 10))
        .thenReturn(
            new NecesidadMaterialDTO(
                "necesidad1", "entidad1", 5, "desc", 10, "producto1", TipoNecesidadMaterialEnum.EXTRAORDINARIA));
    when(fachadaDonaciones.cambiarEstadoDeDonacion("donacion1", EstadoDonacionEnum.ACEPTADA))
        .thenReturn(
            new DonacionDTO(
                "donacion1", "donador1", deposito.id(), "desc", "producto1", 10, EstadoDonacionEnum.ACEPTADA));

    fachada.reportarEntrega(paquete);

    assertEquals(
        EstadoAsignacionEnum.COMPLETADA,
        fachada.buscarAsignacionPorPaqueteID(paquete.id()).estado());
    verify(fachadaDonadoresYEntidades).satisfacerNecesidad("necesidad1", 10);
    verify(fachadaDonaciones).cambiarEstadoDeDonacion("donacion1", EstadoDonacionEnum.ACEPTADA);
  }

  @Test
  void ejecutarMatchmakingRecurrenteSoloSiLaCantidadCubreElPendiente() {
    DepositoDTO deposito = crearDeposito();
    DepositoDTO conPaquete = fachada.gestionarDonacion(deposito.id(), "donacion1", "producto1", 5);
    PaqueteDTO paquete = conPaquete.stockActual().getFirst();

    List<NecesidadMaterialDTO> necesidades =
        List.of(
            new NecesidadMaterialDTO(
                "recurrente", "entidad1", 5, "desc", 10, "producto1", TipoNecesidadMaterialEnum.RECURRENTE));

    AsignacionDTO resultado = fachada.ejecutarMatchmaking(conPaquete.id(), paquete, necesidades);
    assertNull(resultado);

    DepositoDTO actualizado = fachada.buscarDepositoPorID(deposito.id());
    assertEquals(1, actualizado.stockActual().size());
  }

  private DepositoDTO crearDeposito() {
    DepositoDTO deposito =
        fachada.agregarDeposito(
            new DepositoDTO(null, null, "Deposito A", "Direccion A", 100, null));
    fachada.setAlgoritmoMM(deposito.id(), TipoAlgoritmoEnum.SUB_ATENDIDOS);
    return deposito;
  }
}
