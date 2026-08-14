package ar.edu.utn.dds.k3003.model.algoritmos;

import java.util.Objects;
import java.util.Optional;

public final class ResultadoMatchmaking {

    private final NecesidadMatchmaking necesidad;
    private final int cantidadAsignada;
    private final int cantidadSobrante;

    private ResultadoMatchmaking(
        NecesidadMatchmaking necesidad,
        int cantidadAsignada,
        int cantidadSobrante
    ) {
        if (cantidadAsignada < 0 || cantidadSobrante < 0)
            throw new IllegalArgumentException("Las cantidades no pueden ser negativas");

        this.necesidad = necesidad;
        this.cantidadAsignada = cantidadAsignada;
        this.cantidadSobrante = cantidadSobrante;
    }

    public static ResultadoMatchmaking sinAsignacion(int cantidadDonada) {
        if (cantidadDonada <= 0)
            throw new IllegalArgumentException("La cantidad donada debe ser positiva");

        return new ResultadoMatchmaking(null, 0, cantidadDonada);
    }

    public static ResultadoMatchmaking conAsignacion(
        NecesidadMatchmaking necesidad,
        int cantidadAsignada,
        int cantidadSobrante
    ) {
        Objects.requireNonNull(necesidad, "La necesidad asignada no puede ser nula");

        if (cantidadAsignada <= 0)
            throw new IllegalArgumentException("La cantidad asignada debe ser positiva");

        return new ResultadoMatchmaking(necesidad, cantidadAsignada, cantidadSobrante);
    }

    public Optional<NecesidadMatchmaking> necesidad() {
        return Optional.ofNullable(necesidad);
    }

    public int cantidadAsignada() {
        return cantidadAsignada;
    }

    public int cantidadSobrante() {
        return cantidadSobrante;
    }

    public boolean tieneAsignacion() {
        return necesidad != null;
    }
}
