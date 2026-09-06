package ar.edu.utn.dds.k3003.services;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;

public record MatchmakingRegistrationResult(
    AsignacionDTO asignacion,
    boolean nuevo,
    int cantidadSobrante) {

  public boolean tieneAsignacion() {
    return asignacion != null;
  }
}
