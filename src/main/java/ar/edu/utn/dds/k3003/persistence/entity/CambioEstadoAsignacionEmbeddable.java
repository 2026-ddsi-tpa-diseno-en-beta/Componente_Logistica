package ar.edu.utn.dds.k3003.persistence.entity;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsginacionEnum;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;

@Embeddable
public class CambioEstadoAsignacionEmbeddable {

  @Enumerated(EnumType.STRING)
  private EstadoAsginacionEnum estado;

  private LocalDateTime fecha;

  public CambioEstadoAsignacionEmbeddable() {}

  public CambioEstadoAsignacionEmbeddable(EstadoAsginacionEnum estado, LocalDateTime fecha) {
    this.estado = estado;
    this.fecha = fecha;
  }

  public EstadoAsginacionEnum getEstado() { return estado; }
  public void setEstado(EstadoAsginacionEnum estado) { this.estado = estado; }

  public LocalDateTime getFecha() { return fecha; }
  public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
