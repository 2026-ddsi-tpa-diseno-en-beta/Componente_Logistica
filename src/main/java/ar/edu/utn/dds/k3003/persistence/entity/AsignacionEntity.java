package ar.edu.utn.dds.k3003.persistence.entity;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsginacionEnum;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "asignaciones")
public class AsignacionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "paquete_id", nullable = false, unique = true)
  private PaqueteEntity paquete;

  @Column(name = "necesidad_id", nullable = false)
  private String necesidadId;

  @Column(nullable = false)
  private LocalDateTime fecha;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoAsginacionEnum estado;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "asignacion_historial", joinColumns = @JoinColumn(name = "asignacion_id"))
  @OrderColumn(name = "orden")
  private List<CambioEstadoAsignacionEmbeddable> historial = new ArrayList<>();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public PaqueteEntity getPaquete() { return paquete; }
  public void setPaquete(PaqueteEntity paquete) { this.paquete = paquete; }

  public String getNecesidadId() { return necesidadId; }
  public void setNecesidadId(String necesidadId) { this.necesidadId = necesidadId; }

  public LocalDateTime getFecha() { return fecha; }
  public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

  public EstadoAsginacionEnum getEstado() { return estado; }
  public void setEstado(EstadoAsginacionEnum estado) { this.estado = estado; }

  public List<CambioEstadoAsignacionEmbeddable> getHistorial() { return historial; }
  public void setHistorial(List<CambioEstadoAsignacionEmbeddable> historial) { this.historial = historial; }
}
