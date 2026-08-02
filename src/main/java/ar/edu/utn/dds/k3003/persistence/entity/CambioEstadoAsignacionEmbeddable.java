package ar.edu.utn.dds.k3003.persistence.entity;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsignacionEnum;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;

@Embeddable
public class CambioEstadoAsignacionEmbeddable {

  @Enumerated(EnumType.STRING)
  private EstadoAsignacionEnum estado;

  private LocalDateTime fecha;

  public CambioEstadoAsignacionEmbeddable() {}

  public CambioEstadoAsignacionEmbeddable(EstadoAsignacionEnum estado, LocalDateTime fecha) {
    this.estado = estado;
    this.fecha = fecha;
  }

  public EstadoAsignacionEnum getEstado() { return estado; }
  public void setEstado(EstadoAsignacionEnum estado) { this.estado = estado; }

  public LocalDateTime getFecha() { return fecha; }
  public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
