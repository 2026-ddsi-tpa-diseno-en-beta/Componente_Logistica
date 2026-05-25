package ar.edu.utn.dds.k3003.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.utn.dds.k3003.Fachada;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.DonacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.EstadoDonacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.TipoNecesidadMaterialEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsginacionEnum;
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
    DepositoDTO creado =
        fachada.agregarDeposito(new DepositoDTO(null, null, "Deposito A", "Direccion A", 100, null));
    fachada.setAlgoritmoMM(creado.id(), TipoAlgoritmoEnum.SUB_ATENDIDOS);

    assertNotNull(creado.id());
    assertEquals("Deposito A", creado.nombre());

    DepositoDTO buscado = fachada.buscarDepositoPorID(creado.id());
    assertEquals(creado.id(), buscado.id());
    assertEquals("Direccion A", buscado.direccion());
  }

  @Test
  void ejecutarMatchmakingGuardaAsignacion() {
    DepositoDTO deposito =
        fachada.agregarDeposito(new DepositoDTO(null, null, "Deposito A", "Direccion A", 100, null));
    fachada.setAlgoritmoMM(deposito.id(), TipoAlgoritmoEnum.SUB_ATENDIDOS);

    PaqueteDTO paquete = new PaqueteDTO("paquete1", "donacion1", "producto1", 10);
    List<NecesidadMaterialDTO> necesidades =
        List.of(
            new NecesidadMaterialDTO(
                "necesidad1",
                "entidad1",
                5,
                "desc",
                20,
                "producto1",
                TipoNecesidadMaterialEnum.EXTRAORDINARIA));

    AsignacionDTO asignacion = fachada.ejecutarMatchmaking(deposito.id(), paquete, necesidades);

    assertNotNull(asignacion);
    assertEquals("paquete1", asignacion.paqueteID());
    assertEquals("necesidad1", asignacion.necesidadID());
    assertEquals(EstadoAsginacionEnum.ASIGNADA, asignacion.estado());

    AsignacionDTO buscada = fachada.buscarAsignacionPorPaqueteID("paquete1");
    assertEquals(asignacion.id(), buscada.id());
  }

  @Test
  void gestionarDonacionInvocaDependenciasYDevuelveDeposito() {
    DepositoDTO deposito =
        fachada.agregarDeposito(new DepositoDTO(null, null, "Deposito A", "Direccion A", 100, null));
    fachada.setAlgoritmoMM(deposito.id(), TipoAlgoritmoEnum.SUB_ATENDIDOS);

    when(fachadaDonadoresYEntidades.obtenerNecesidadesInsatisfechasDe("producto1"))
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

    DepositoDTO resultado = fachada.gestionarDonacion(deposito.id(), "donacion1", "producto1", 10);

    assertEquals(deposito.id(), resultado.id());
    verify(fachadaDonadoresYEntidades, times(1)).obtenerNecesidadesInsatisfechasDe("producto1");
    verify(fachadaDonadoresYEntidades, never()).satisfacerNecesidad(anyString(), anyInt());
  }

  @Test
  void reportarEntregaMarcaAsignacionComoCompletada() {
    DepositoDTO deposito =
        fachada.agregarDeposito(new DepositoDTO(null, null, "Deposito A", "Direccion A", 100, null));
    fachada.setAlgoritmoMM(deposito.id(), TipoAlgoritmoEnum.SUB_ATENDIDOS);

    PaqueteDTO paquete = new PaqueteDTO("paquete1", "donacion1", "producto1", 10);

    when(fachadaDonadoresYEntidades.satisfacerNecesidad("necesidad1", paquete.cantidad()))
        .thenReturn(
            new NecesidadMaterialDTO(
                "necesidad1",
                "entidad1",
                5,
                "desc",
                0,
                "producto1",
                TipoNecesidadMaterialEnum.EXTRAORDINARIA));

    when(fachadaDonaciones.cambiarEstadoDeDonacion(
            paquete.donacionID(), EstadoDonacionEnum.ACEPTADA))
        .thenReturn(
            new DonacionDTO(
                paquete.donacionID(),
                "donador1",
                "deposito1",
                "descripcion1",
                paquete.producto(),
                paquete.cantidad(),
                EstadoDonacionEnum.ACEPTADA));

    fachada.ejecutarMatchmaking(
        deposito.id(),
        paquete,
        List.of(
            new NecesidadMaterialDTO(
                "necesidad1",
                "entidad1",
                5,
                "desc",
                20,
                "producto1",
                TipoNecesidadMaterialEnum.EXTRAORDINARIA)));

    fachada.reportarEntrega(paquete);

    AsignacionDTO asignacion = fachada.buscarAsignacionPorPaqueteID("paquete1");
    assertEquals(EstadoAsginacionEnum.COMPLETADA, asignacion.estado());

    verify(fachadaDonadoresYEntidades, times(1))
        .satisfacerNecesidad("necesidad1", paquete.cantidad());
    verify(fachadaDonaciones, times(1))
        .cambiarEstadoDeDonacion("donacion1", EstadoDonacionEnum.ACEPTADA);
  }
}
