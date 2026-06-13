package ar.edu.utn.dds.k3003.integration;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.DonacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.EstadoDonacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.IdentificadorDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.ProductoDTO;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonaciones;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonadoresYEntidades;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaLogistica;

public class FachadaDonacionesHttp implements FachadaDonaciones{
     private final RestClient restClient;

    public FachadaDonacionesHttp(String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public ProductoDTO buscarProductoPorID(String productoID)
            throws NoSuchElementException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DonacionDTO> buscarPorDonadorYFechaInicio(
            String donadorID,
            LocalDate fecha)
            throws NoSuchElementException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFachadaDonadoresYEntidades(
            FachadaDonadoresYEntidades fachadaDonadoresYEntidades) {}

    @Override
    public void setFachadaLogistica(
            FachadaLogistica fachadaLogistica) {}

    @Override
    public DonacionDTO registrarDonacion(DonacionDTO donacionDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DonacionDTO buscarDonacionPorID(String donacionID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DonacionDTO cambiarEstadoDeDonacion(
        String donacionID,
        EstadoDonacionEnum estado) {

    return restClient.patch()
            .uri(uriBuilder -> uriBuilder
                    .path("/donaciones/{id}/estado")
                    .queryParam("estado", estado)
                    .build(donacionID))
            .retrieve()
            .body(DonacionDTO.class);
    }

    @Override
    public DonacionDTO registrarQuejaEnDonacion(
            String donacionID,
            String descripcion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProductoDTO agregarProducto(ProductoDTO productoDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentificadorDTO agregarIdentificador(
            IdentificadorDTO identificadorDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentificadorDTO buscarIdentificadorPorID(
            String identificadorID) {
        throw new UnsupportedOperationException();
    }
}