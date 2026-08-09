package ar.edu.utn.dds.k3003.worker;

import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LogisticaInternalClient {

    private final RestClient client;

    public LogisticaInternalClient(
        @Value("${logistica.api-url}") String baseUrl
    ) {
        this.client = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    public void registrarResultado(ResultadoMatchmakingRequest request) {
        client.post()
            .uri("/internal/matchmaking/resultados")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity();
    }
}
