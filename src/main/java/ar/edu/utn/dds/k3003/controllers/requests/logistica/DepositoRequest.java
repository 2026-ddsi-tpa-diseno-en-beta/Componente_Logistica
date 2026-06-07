package ar.edu.utn.dds.k3003.controllers.requests.logistica;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para crear un depósito")
public record DepositoRequest(

    @Schema(example = "Deposito Central")
    @NotBlank String nombre,

    @Schema(example = "Av. Don Bosco 86")
    @NotBlank String direccion,

    @Schema(example = "1000")
    @NotNull @Positive Integer capacidadMaxima

) {}
