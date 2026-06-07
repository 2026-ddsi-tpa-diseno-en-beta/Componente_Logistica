package ar.edu.utn.dds.k3003.integration.donadoresYEntidades;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "donadoresYEntidadesClient",
    url = "${integrations.donadores-url}"
)
public interface DonadoresYEntidadesFeignClient {

  @GetMapping("/necesidades")
  List<NecesidadMaterialDTO> obtenerNecesidadesInsatisfechasDe(
      @RequestParam("productoSolicitadoID") String productoSolicitadoID
  );

  @PostMapping("/necesidades/{necesidadID}/satisfaccion")
  String satisfacerNecesidad(
      @PathVariable("necesidadID") String necesidadID,
      @RequestParam("cantidadASatisfacer") Integer cantidadASatisfacer
  );
}
