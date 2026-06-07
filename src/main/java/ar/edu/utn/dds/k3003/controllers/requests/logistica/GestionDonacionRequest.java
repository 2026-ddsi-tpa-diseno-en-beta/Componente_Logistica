package ar.edu.utn.dds.k3003.controllers.requests.logistica;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para gestionar una donación en un depósito")
public record GestionDonacionRequest(

    @Schema(example = "donacion1")
    @NotBlank String donacionID,

    @Schema(example = "producto1")
    @NotBlank String productoID,

    @Schema(example = "10")
    @NotNull @Positive Integer cantidad

) {}
