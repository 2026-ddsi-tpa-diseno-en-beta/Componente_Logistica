package ar.edu.utn.dds.k3003.controllers.requests.logistica;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SolicitudAsignacionStockRequest(
    @NotBlank String necesidadId,
    @NotBlank String productoId,
    @NotNull @Min(1) Integer cantidad
) {}
