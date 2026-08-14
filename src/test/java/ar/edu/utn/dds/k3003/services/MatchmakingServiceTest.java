package ar.edu.utn.dds.k3003.services;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.TipoNecesidadMaterialEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MatchmakingServiceTest {

  private final MatchmakingService service = new MatchmakingService();

  @Test
  void subAtendidosEligeLaNecesidadConMayorFaltante() {
    List<NecesidadMaterialDTO> necesidades =
        List.of(
            necesidadConUrgencia("n1", 100, 90, 2),
            necesidadConUrgencia("n2", 30, 0, 1));

    var resultado =
        service.procesar(
            TipoAlgoritmoEnum.SUB_ATENDIDOS,
            necesidades,
            "producto",
            10,
            Map.of("n1", 90, "n2", 0));

    assertTrue(resultado.tieneAsignacion());
    assertEquals("n2", resultado.necesidad().orElseThrow().id());
    assertEquals(10, resultado.cantidadAsignada());
    assertEquals(0, resultado.cantidadSobrante());
  }

  @Test
  void scoreUsaLaFormulaDeLaConsigna() {
    List<NecesidadMaterialDTO> necesidades =
        List.of(
            necesidadConUrgencia("n1", 100, 100, 2),
            necesidadConUrgencia("n2", 20, 0, 5));

    var resultado =
        service.procesar(
            TipoAlgoritmoEnum.PRIORIDAD_POR_SCORE,
            necesidades,
            "producto",
            10,
            Map.of("n1", 0, "n2", 0));

    assertEquals("n1", resultado.necesidad().orElseThrow().id());
  }

  @Test
  void recurrenteNoSeSeleccionaConCantidadInsuficiente() {
    var resultado =
        service.procesar(
            TipoAlgoritmoEnum.SUB_ATENDIDOS,
            List.of(
                new NecesidadMaterialDTO(
                    "n1", "e1", 5, "desc", 20, "producto", TipoNecesidadMaterialEnum.RECURRENTE)),
            "producto",
            10,
            Map.of("n1", 0));

    assertFalse(resultado.tieneAsignacion());
    assertEquals(10, resultado.cantidadSobrante());
  }

  @Test
  void recurrentePuedeCubrirElPendiente() {
    var resultado =
        service.procesar(
            TipoAlgoritmoEnum.SUB_ATENDIDOS,
            List.of(
                new NecesidadMaterialDTO(
                    "n1", "e1", 5, "desc", 20, "producto", TipoNecesidadMaterialEnum.RECURRENTE)),
            "producto",
            10,
            Map.of("n1", 10));

    assertTrue(resultado.tieneAsignacion());
    assertEquals(10, resultado.cantidadAsignada());
    assertEquals(0, resultado.cantidadSobrante());
  }


  private NecesidadMaterialDTO necesidadConUrgencia(
      String id, int objetivo, int satisfecha, int urgencia) {
    return new NecesidadMaterialDTO(
        id,
        "entidad",
        urgencia,
        "descripcion",
        objetivo,
        "producto",
        TipoNecesidadMaterialEnum.EXTRAORDINARIA);
  }
}
