package ar.edu.utn.dds.k3003.integration.adapters;

import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.DonacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.EstadoDonacionEnum;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonaciones;
import ar.edu.utn.dds.k3003.integration.donaciones.DonacionesFeignClient;
import org.springframework.stereotype.Component;

@Component
public class DonacionesRestAdapter implements FachadaDonaciones {

  private final DonacionesFeignClient client;

  public DonacionesRestAdapter(DonacionesFeignClient client) {
    this.client = client;
  }

  @Override
  public DonacionDTO cambiarEstadoDeDonacion(String donacionID, EstadoDonacionEnum estado) {
    return client.cambiarEstadoDeDonacion(donacionID, estado);
  }
}