package ar.edu.utn.dds.k3003.persistence.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "logistica_solicitudes_stock")
public class SolicitudStockProcesada {
  @Id @Column(length = 255) public String id;
  @Column(nullable = false) public String necesidadId;
  @Column(nullable = false) public String productoId;
  @Column(nullable = false) public Integer cantidad;
  @ElementCollection
  @CollectionTable(name = "logistica_solicitud_asignaciones", joinColumns = @JoinColumn(name = "solicitud_id"))
  @Column(name = "asignacion_id", nullable = false)
  @OrderColumn(name = "orden")
  public List<String> asignaciones = new ArrayList<>();
}
