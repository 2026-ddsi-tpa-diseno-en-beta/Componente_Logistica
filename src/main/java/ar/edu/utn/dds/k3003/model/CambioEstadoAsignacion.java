package ar.edu.utn.dds.k3003.model;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsginacionEnum;

import java.time.LocalDateTime;

public class CambioEstadoAsignacion {

    private EstadoAsginacionEnum estado;
    private LocalDateTime fecha;

    public CambioEstadoAsignacion(EstadoAsginacionEnum estado, LocalDateTime fecha) {
        this.estado = estado;
        this.fecha = fecha;
    }

    public EstadoAsginacionEnum getEstado() {
        return estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}