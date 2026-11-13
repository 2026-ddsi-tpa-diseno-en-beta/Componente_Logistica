package ar.edu.utn.dds.k3003.integration;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.DonadorDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.DonadorStatsDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.EntidadBeneficaDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.EstadoDonadorEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.QuejaDTO;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonadoresYEntidades;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaIncentivos;
import ar.edu.utn.dds.k3003.observability.TracePropagationInterceptor;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.core.ParameterizedTypeReference;
// Logging
import org.springframework.web.client.RestClient;

public class FachadaDonadoresYEntidadesHttp
    implements FachadaDonadoresYEntidades, ConsultaPeriodoNecesidad, SatisfaccionIdempotente, ConsultaNecesidadVigente {
  private final RestClient restClient;

  @Override
  public NecesidadVigente consultarNecesidad(String id) {
    try {
      com.fasterxml.jackson.databind.JsonNode body = restClient.get().uri("/necesidades/{id}", id)
          .retrieve().body(com.fasterxml.jackson.databind.JsonNode.class);
      if (body == null || !body.hasNonNull("id"))
        throw new IllegalStateException("La integración no devolvió una necesidad válida");
      var necesidad = new NecesidadMaterialDTO(body.path("id").asText(), body.path("entidadID").asText(),
          body.path("nivelDeUrgencia").asInt(), body.path("descripcion").asText(),
          body.path("cantidadObjetivo").asInt(), body.path("productoSolicitadoID").asText(),
          ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.TipoNecesidadMaterialEnum.valueOf(body.path("tipo").asText()));
      // Current DyE includes the period in the same snapshot. Keep the old endpoint as compatibility only.
      java.time.LocalDate inicio = body.has("inicioPeriodo")
          ? body.hasNonNull("inicioPeriodo") ? java.time.LocalDate.parse(body.path("inicioPeriodo").asText()) : null
          : inicioPeriodo(id);
      return new NecesidadVigente(necesidad, body.path("cantidadAsignada").asInt(0), inicio);
    } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
      throw new ar.edu.utn.dds.k3003.exceptions.ResourceNotFoundException("Necesidad no encontrada");
    }
  }

  @Override
  public java.time.LocalDate inicioPeriodo(String necesidadId) {
    java.util.Map<String, Object> body =
        restClient
            .get()
            .uri("/necesidades/{id}/periodo", necesidadId)
            .retrieve()
            .body(new ParameterizedTypeReference<java.util.Map<String, Object>>() {});
    return body == null || body.get("inicio") == null
        ? null
        : java.time.LocalDate.parse(body.get("inicio").toString());
  }

  public FachadaDonadoresYEntidadesHttp(String baseUrl) {
    this.restClient =
        RestClient.builder()
            .requestFactory(ar.edu.utn.dds.k3003.observability.HttpClients.requestFactory())
            .baseUrl(baseUrl)
            // Usada tanto por la API como por el worker (vía WorkerApplication) para
            // pedir necesidades insatisfechas durante el matchmaking: con esto ese
            // llamado también viaja con el traceId del mensaje de Rabbit que lo originó.
            .requestInterceptor(new TracePropagationInterceptor())
            .build();
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
  public DonadorDTO modificarEstado(String donadorID, EstadoDonadorEnum estado)
      throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public DonadorDTO modifcarCategoria(String donadorID, String categoria)
      throws NoSuchElementException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<NecesidadMaterialDTO> obtenerNecesidadesInsatisfechasDe(String productoSolicitadoID) {
    return restClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/necesidades")
                    .queryParam("productoSolicitadoID", productoSolicitadoID)
                    .build())
        .retrieve()
        .body(new ParameterizedTypeReference<List<NecesidadMaterialDTO>>() {});
  }

  @Override
  public NecesidadMaterialDTO satisfacerNecesidad(String necesidadID, Integer cantidad)
      throws NoSuchElementException {
    satisfacerEntrega(necesidadID, cantidad, null);
    return null;
  }

  @Override
  public void satisfacerEntrega(String necesidadID, Integer cantidad, String entregaID) {
    var request =
        restClient
            .post()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/necesidades/{id}/satisfaccion")
                        .queryParam("cantidadASatisfacer", cantidad)
                        .build(necesidadID));
    if (entregaID != null) request.header("Idempotency-Key", entregaID);
    request.retrieve().body(String.class);
  }

  @Override
  public DonadorStatsDTO estadisticasDonador(String donadorID) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFachadaIncentivos(FachadaIncentivos fachadaIncentivos) {}
}
