package ar.edu.utn.dds.k3003.messaging.dto;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;

public record DonacionPendienteMessage(
    String depositoId,
    String paqueteId,
    String donacionId,
    String productoId,
    Integer cantidad,
    TipoAlgoritmoEnum algoritmo,
    String traceId) {

  public DonacionPendienteMessage(
      String depositoId,
      String paqueteId,
      String donacionId,
      String productoId,
      Integer cantidad,
      TipoAlgoritmoEnum algoritmo) {
    this(depositoId, paqueteId, donacionId, productoId, cantidad, algoritmo, null);
  }
}
