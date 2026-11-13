package ar.edu.utn.dds.k3003.worker;

import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;

// Logging
import ar.edu.utn.dds.k3003.observability.TracePropagationInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Component
@Profile("worker")
public class LogisticaInternalClient {

    private final RestClient client;

    public LogisticaInternalClient(
        @Value("${logistica.api-url}") String baseUrl
    ) {
        this.client = RestClient.builder()
            .requestFactory(ar.edu.utn.dds.k3003.observability.HttpClients.requestFactory())
            .baseUrl(baseUrl)
            // El worker ya tiene el traceId en su MDC (lo colocó AsignacionWorker al
            // recibir el mensaje de Rabbit). Con este interceptor, la API recibe 
            // el mismo traceId que el worker y todo
            // el flujo (Rabbit -> worker -> API) queda correlacionado.
            .requestInterceptor(new TracePropagationInterceptor())
            .build();
    }

    public int cantidadAsignada(String necesidadId) {
        Integer cantidad = client.get()
            .uri("/internal/matchmaking/necesidades/{id}/cantidad-asignada", necesidadId)
            .retrieve()
            .body(Integer.class);

        return cantidad == null ? 0 : cantidad;
    }

    public ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO registrarResultado(ResultadoMatchmakingRequest request) {
        var response = client.post()
            .uri("/internal/matchmaking/resultados")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toEntity(ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO.class);
        if (response.getStatusCode().value() == 204) return null;
        var assignment = response.getBody();
        if (assignment == null || assignment.id() == null || assignment.necesidadID() == null
            || assignment.cantidadAsignada() == null || assignment.cantidadAsignada() <= 0)
            throw new IllegalStateException("El callback devolvió una asignación inválida");
        return assignment;
    }
}
