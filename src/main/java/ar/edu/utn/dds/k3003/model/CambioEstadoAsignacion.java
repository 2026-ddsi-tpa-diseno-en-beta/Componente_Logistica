package ar.edu.utn.dds.k3003.model;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsignacionEnum;

import java.time.LocalDateTime;

public class CambioEstadoAsignacion {

    private EstadoAsignacionEnum estado;
    private LocalDateTime fecha;

    public CambioEstadoAsignacion(EstadoAsignacionEnum estado, LocalDateTime fecha) {
        this.estado = estado;
        this.fecha = fecha;
    }

    public EstadoAsignacionEnum getEstado() {
        return estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}