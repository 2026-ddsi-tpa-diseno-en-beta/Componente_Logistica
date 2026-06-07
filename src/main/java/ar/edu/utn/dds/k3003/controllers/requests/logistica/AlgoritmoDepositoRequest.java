package ar.edu.utn.dds.k3003.controllers.requests.logistica;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;
import jakarta.validation.constraints.NotNull;

public record AlgoritmoDepositoRequest(
    @NotNull TipoAlgoritmoEnum algoritmo
) {}
