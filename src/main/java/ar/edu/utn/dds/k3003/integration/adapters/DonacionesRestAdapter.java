package ar.edu.utn.dds.k3003.integration.adapters;

import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.DonacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.EstadoDonacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.IdentificadorDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.ProductoDTO;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonaciones;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonadoresYEntidades;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaLogistica;
import ar.edu.utn.dds.k3003.integration.donaciones.DonacionesFeignClient;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
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

  @Override
  public DonacionDTO registrarDonacion(DonacionDTO donacionDTO) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DonacionDTO buscarDonacionPorID(String donacionID) throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<DonacionDTO> buscarPorDonadorYFechaInicio(String donadorID, LocalDate fecha)
      throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public DonacionDTO registrarQuejaEnDonacion(String donacionID, String descripcion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ProductoDTO agregarProducto(ProductoDTO productoDTO) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ProductoDTO buscarProductoPorID(String productoID) throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public IdentificadorDTO agregarIdentificador(IdentificadorDTO identificadorDTO) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IdentificadorDTO buscarIdentificadorPorID(String identificadorID)
      throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFachadaDonadoresYEntidades(
      FachadaDonadoresYEntidades fachadaDonadoresYEntidades) {}

  @Override
  public void setFachadaLogistica(FachadaLogistica fachadaLogistica) {}
}
