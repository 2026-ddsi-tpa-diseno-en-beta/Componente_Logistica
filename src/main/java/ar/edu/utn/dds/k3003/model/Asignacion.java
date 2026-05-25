package ar.edu.utn.dds.k3003.model;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsginacionEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class Asignacion {

    private String id;
    private String paqueteId;
    private String necesidadId;
    private LocalDateTime fecha;
    private EstadoAsginacionEnum estado;
    private List<CambioEstadoAsignacion> historial = new ArrayList<>();

    public Asignacion
    (
        String paqueteId,
        String necesidadId,
        LocalDateTime fecha,
        EstadoAsginacionEnum estado
    ) {
        this.paqueteId = paqueteId;
        this.necesidadId = necesidadId;
        this.fecha = fecha;
        this.estado = estado;
        this.historial.add(new CambioEstadoAsignacion(estado, fecha));
    }

    public void cambiarEstado(EstadoAsginacionEnum nuevoEstado) {
        this.estado = nuevoEstado;
        this.historial.add(new CambioEstadoAsignacion(nuevoEstado, LocalDateTime.now()));
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getPaqueteId() {
        return paqueteId;
    }
    public void setPaqueteId(String paqueteId) {
        this.paqueteId = paqueteId;
    }

    public String getNecesidadId() {
        return necesidadId;
    }
    public void setNecesidadId(String necesidadId) {
        this.necesidadId = necesidadId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public EstadoAsginacionEnum getEstado() {
        return estado;
    }
    public void setEstado(EstadoAsginacionEnum estado) {
        this.estado = estado;
    } 

    public List<CambioEstadoAsignacion> getHistorial() {
        return historial;
    }
}