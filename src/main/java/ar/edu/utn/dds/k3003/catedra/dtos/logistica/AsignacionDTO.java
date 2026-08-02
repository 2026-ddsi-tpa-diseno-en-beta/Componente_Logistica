package ar.edu.utn.dds.k3003.catedra.dtos.logistica;

import ar.edu.utn.dds.k3003.model.OrigenAsignacion;
import java.time.LocalDateTime;

public record AsignacionDTO(
    String id,
    String paqueteID,
    String necesidadID,
    LocalDateTime fecha,
    EstadoAsignacionEnum estado,
    Integer cantidadAsignada,
    OrigenAsignacion origen) {}
