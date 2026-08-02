package ar.edu.utn.dds.k3003.controllers.requests.logistica;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResultadoMatchmakingRequest(
    @NotBlank String depositoId,
    @NotBlank String paqueteId,
    String necesidadId,
    @NotNull @Min(0) Integer cantidadAsignada,
    @NotNull @Min(0) Integer cantidadSobrante
) {
    public boolean tieneAsignacion() {
        return necesidadId != null
            && !necesidadId.isBlank()
            && cantidadAsignada > 0;
    }
}
