package ar.edu.utn.dds.k3003.catedra.dtos.logistica;

import ar.edu.utn.dds.k3003.model.OrigenAsignacion;

public record AltaAsignacionDTO(
    String paqueteId,
    String necesidadId,
    Integer cantidadAsignada,
    OrigenAsignacion origen) {}
