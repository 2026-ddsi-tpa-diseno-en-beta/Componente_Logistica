package ar.edu.utn.dds.k3003.controllers;

import ar.edu.utn.dds.k3003.services.LogisticaService;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;
import ar.edu.utn.dds.k3003.metrics.LogisticaMetrics;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/matchmaking")
public class MatchmakingInternalController {

    private final LogisticaService service;
    private final LogisticaMetrics metrics;

    public MatchmakingInternalController(LogisticaService service, LogisticaMetrics metrics) {
        this.service = service;
        this.metrics = metrics;
    }

    @PostMapping("/resultados")
    public ResponseEntity<AsignacionDTO> registrarResultado(
        @Valid @RequestBody ResultadoMatchmakingRequest request
    ) {
        AsignacionDTO resultado =
            service.registrarResultadoMatchmaking(request);

        if (resultado == null) {
            metrics.matchmakingSinAsignacion();
            return ResponseEntity.noContent().build();
        }

        metrics.asignacionMatchmaking();

        return ResponseEntity.ok(resultado);
    }
}
