package ar.edu.utn.dds.k3003.controllers.requests.logistica;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para crear un depósito")
public record DepositoRequest(

    @Schema(example = "Deposito Central")
    String nombre,

    @Schema(example = "Av. Don Bosco 86")
    String direccion,

    @Schema(example = "1000")
    Integer capacidadMaxima

) {}
