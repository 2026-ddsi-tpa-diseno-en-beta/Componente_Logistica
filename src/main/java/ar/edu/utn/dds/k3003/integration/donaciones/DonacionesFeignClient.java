package ar.edu.utn.dds.k3003.integration.donaciones;

import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.DonacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.EstadoDonacionEnum;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "donacionesClient",
    url = "${integrations.donaciones-url}"
)
public interface DonacionesFeignClient {

  @PatchMapping("/donaciones/{id}/estado")
  DonacionDTO cambiarEstadoDeDonacion(
      @PathVariable("id") String donacionID,
      @RequestParam("estado") EstadoDonacionEnum estado
  );
}
