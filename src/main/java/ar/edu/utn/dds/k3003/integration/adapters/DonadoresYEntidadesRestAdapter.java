package ar.edu.utn.dds.k3003.integration.adapters;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.*;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonadoresYEntidades;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaIncentivos;
import ar.edu.utn.dds.k3003.integration.donadoresYEntidades.DonadoresYEntidadesFeignClient;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Component;

@Component
public class DonadoresYEntidadesRestAdapter implements FachadaDonadoresYEntidades {

  private final DonadoresYEntidadesFeignClient client;


  public DonadoresYEntidadesRestAdapter(DonadoresYEntidadesFeignClient client) {
    this.client = client;
  }

  @Override
  public List<NecesidadMaterialDTO> obtenerNecesidadesInsatisfechasDe(String productoID) {
    return client.obtenerNecesidadesInsatisfechasDe(productoID);
  }

  @Override
  public NecesidadMaterialDTO satisfacerNecesidad(String necesidadID, Integer cantidad) {
    client.satisfacerNecesidad(necesidadID, cantidad);
    return null;
  }

  @Override
  public DonadorDTO modifcarCategoria(String donadorID, String categoria) throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public DonadorDTO agregarDonador(DonadorDTO donadorDTO) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DonadorDTO buscarDonadorPorID(String donadorID) throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public EntidadBeneficaDTO agregarEntidad(EntidadBeneficaDTO entidadBeneficaDTO) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EntidadBeneficaDTO buscarEntidadPorID(String entidadID) throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public NecesidadMaterialDTO registrarNecesidad(NecesidadMaterialDTO necesidadMaterialDTO) {
    throw new UnsupportedOperationException();
  }

  @Override
  public QuejaDTO agregarQueja(QuejaDTO quejaDTO) throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean puedeDonar(String donadorID) throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<QuejaDTO> obtenerQuejasDe(String donadorID) throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public DonadorDTO modificarEstado(String donadorID, EstadoDonadorEnum estado) throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public DonadorStatsDTO estadisticasDonador(String donadorID) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFachadaIncentivos(FachadaIncentivos fachadaIncentivos) { }
}
