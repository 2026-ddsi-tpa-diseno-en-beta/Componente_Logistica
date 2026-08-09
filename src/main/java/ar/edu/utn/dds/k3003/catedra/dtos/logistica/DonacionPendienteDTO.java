package ar.edu.utn.dds.k3003.catedra.dtos.logistica;

public record DonacionPendienteDTO(
    String depositoId,
    String donacionId,
    String productoId,
    Integer cantidad) {}
