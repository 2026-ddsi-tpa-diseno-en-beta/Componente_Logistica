package ar.edu.utn.dds.k3003.persistence.entity;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsignacionEnum;

import ar.edu.utn.dds.k3003.model.OrigenAsignacion;

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
  private EstadoAsignacionEnum estado;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "asignacion_historial", joinColumns = @JoinColumn(name = "asignacion_id"))
  @OrderColumn(name = "orden")
  private List<CambioEstadoAsignacionEmbeddable> historial = new ArrayList<>();

  @Column(name = "cantidad_asignada")
  private Integer cantidadAsignada;

  @Enumerated(EnumType.STRING)
  private OrigenAsignacion origen;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public PaqueteEntity getPaquete() { return paquete; }
  public void setPaquete(PaqueteEntity paquete) { this.paquete = paquete; }

  public String getNecesidadId() { return necesidadId; }
  public void setNecesidadId(String necesidadId) { this.necesidadId = necesidadId; }

  public LocalDateTime getFecha() { return fecha; }
  public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

  public EstadoAsignacionEnum getEstado() { return estado; }
  public void setEstado(EstadoAsignacionEnum estado) { this.estado = estado; }

  public List<CambioEstadoAsignacionEmbeddable> getHistorial() { return historial; }
  public void setHistorial(List<CambioEstadoAsignacionEmbeddable> historial) { this.historial = historial; }

  public Integer getCantidadAsignada() { return cantidadAsignada; }
  public void setCantidadAsignada(Integer cantidadAsignada) { this.cantidadAsignada = cantidadAsignada; }

  public OrigenAsignacion getOrigen() { return origen; }
  public void setOrigen(OrigenAsignacion origen) { this.origen = origen; }
}
