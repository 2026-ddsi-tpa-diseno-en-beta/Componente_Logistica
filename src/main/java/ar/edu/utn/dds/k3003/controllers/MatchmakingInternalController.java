package ar.edu.utn.dds.k3003.controllers;

import ar.edu.utn.dds.k3003.Fachada;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/matchmaking")
public class MatchmakingInternalController {

    private final Fachada fachada;

    public MatchmakingInternalController(Fachada fachada) {
        this.fachada = fachada;
    }

    @PostMapping("/resultados")
    public ResponseEntity<AsignacionDTO> registrarResultado(
        @Valid @RequestBody ResultadoMatchmakingRequest request
    ) {
        AsignacionDTO resultado =
            fachada.registrarResultadoMatchmaking(request);

        return resultado == null
            ? ResponseEntity.noContent().build()
            : ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
