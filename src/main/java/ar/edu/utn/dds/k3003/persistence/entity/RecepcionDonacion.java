package ar.edu.utn.dds.k3003.persistence.entity;

import jakarta.persistence.*;

/** One receipt represents the original ingress, independently of later package splits. */
@Entity
@Table(name = "logistica_recepciones")
public class RecepcionDonacion {
  @Id @Column(name = "donacion_id") public String donacionId;
  @Column(nullable = false) public String depositoId;
  @Column(nullable = false) public String productoId;
  @Column(nullable = false) public Integer cantidad;
}
