package ar.edu.utn.dds.k3003.catedra.logistica;

import ar.edu.utn.dds.k3003.Fachada;
import ar.edu.utn.dds.k3003.catedra.ClassFinder;
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
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@EnabledIf("ar.edu.utn.dds.k3003.catedra.logistica.LogisticaTest#condicion")
public class LogisticaTest {

  FachadaLogistica instancia;
  @Mock FachadaDonadoresYEntidades fachadaDonadoresYEntidades;
  @Mock FachadaDonaciones fachadaDonaciones;

  @SneakyThrows
  @BeforeEach
  void setUp() {
    var clazz = ClassFinder.findClass();
    instancia = (FachadaLogistica) clazz.getDeclaredConstructor().newInstance();
    instancia.setFachadaDonadoresYEntidades(fachadaDonadoresYEntidades);
    instancia.setFachadaDonaciones(fachadaDonaciones);
  }

  static boolean condicion() {
    return FachadaLogistica.class.isAssignableFrom(Fachada.class);
  }

  @Test
  void testAgregarDeposito() {
    DepositoDTO retorno =
        instancia.agregarDeposito(
            new DepositoDTO(null, null, "deposito1", "direccion1", 1000, null));

    instancia.setAlgoritmoMM(retorno.id(), TipoAlgoritmoEnum.SUB_ATENDIDOS);

    Assertions.assertNotNull(retorno.id());
    Assertions.assertEquals("deposito1", retorno.nombre());
  }

  @Test
  void testBuscarDepositoPorID() {
    DepositoDTO retorno =
        instancia.agregarDeposito(
            new DepositoDTO(null, null, "deposito1", "direccion1", 1000, null));

    Assertions.assertEquals(retorno.id(), instancia.buscarDepositoPorID(retorno.id()).id());
  }

  @Test
  void testGestionarDonacionDejaPaquetePendiente() {
    DepositoDTO deposito =
        instancia.agregarDeposito(
            new DepositoDTO(null, null, "deposito1", "direccion1", 1000, null));
    instancia.setAlgoritmoMM(deposito.id(), TipoAlgoritmoEnum.SUB_ATENDIDOS);

    DepositoDTO actualizado =
        instancia.gestionarDonacion(deposito.id(), "donacion1", "producto1", 10);

    Assertions.assertEquals(1, actualizado.stockActual().size());
    Assertions.assertEquals("donacion1", actualizado.stockActual().getFirst().donacionID());
  }

  @Test
  void testEjecutarMatchmaking() {
    DepositoDTO deposito =
        instancia.agregarDeposito(
            new DepositoDTO(null, null, "deposito1", "direccion1", 1000, null));
    instancia.setAlgoritmoMM(deposito.id(), TipoAlgoritmoEnum.SUB_ATENDIDOS);

    DepositoDTO conPaquete =
        instancia.gestionarDonacion(deposito.id(), "donacion1", "producto1", 10);
    PaqueteDTO paquete = conPaquete.stockActual().getFirst();

    List<NecesidadMaterialDTO> necesidades =
        List.of(
            new NecesidadMaterialDTO(
                "necesidad1",
                "entidad1",
                5,
                "descripcion1",
                5,
                "producto1",
                TipoNecesidadMaterialEnum.EXTRAORDINARIA));

    AsignacionDTO asignacion = instancia.ejecutarMatchmaking(deposito.id(), paquete, necesidades);

    Assertions.assertNotNull(asignacion);
    Assertions.assertEquals(paquete.id(), asignacion.paqueteID());
    Assertions.assertEquals("necesidad1", asignacion.necesidadID());
    Assertions.assertEquals(EstadoAsignacionEnum.ASIGNADA, asignacion.estado());
  }

  @Test
  void testReportarEntrega() {
    DepositoDTO deposito =
        instancia.agregarDeposito(
            new DepositoDTO(null, null, "deposito1", "direccion1", 1000, null));
    instancia.setAlgoritmoMM(deposito.id(), TipoAlgoritmoEnum.SUB_ATENDIDOS);

    DepositoDTO conPaquete =
        instancia.gestionarDonacion(deposito.id(), "donacion1", "producto1", 10);
    PaqueteDTO paquete = conPaquete.stockActual().getFirst();

    ((Fachada) instancia).registrarResultadoMatchmaking(
        new ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest(
            deposito.id(), paquete.id(), "necesidad1", 10, 0));

    Mockito.when(fachadaDonadoresYEntidades.satisfacerNecesidad("necesidad1", 10))
        .thenReturn(
            new NecesidadMaterialDTO(
                "necesidad1",
                "entidad1",
                5,
                "descripcion1",
                10,
                "producto1",
                TipoNecesidadMaterialEnum.EXTRAORDINARIA));
    Mockito.when(
            fachadaDonaciones.cambiarEstadoDeDonacion(
                "donacion1", EstadoDonacionEnum.ACEPTADA))
        .thenReturn(
            new DonacionDTO(
                "donacion1",
                "donador1",
                deposito.id(),
                "descripcion1",
                "producto1",
                10,
                EstadoDonacionEnum.ACEPTADA));

    instancia.reportarEntrega(paquete);

    Assertions.assertEquals(
        EstadoAsignacionEnum.COMPLETADA,
        instancia.buscarAsignacionPorPaqueteID(paquete.id()).estado());
  }
}
