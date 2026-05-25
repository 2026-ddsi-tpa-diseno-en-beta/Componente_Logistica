package ar.edu.utn.dds.k3003.controllers.requests.logistica;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para gestionar una donación en un depósito")
public record GestionDonacionRequest(

    @Schema(example = "donacion1")
    String donacionID,

    @Schema(example = "producto1")
    String productoID,

    @Schema(example = "10")
    Integer cantidad

) {}
