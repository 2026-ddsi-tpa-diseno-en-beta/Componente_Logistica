package ar.edu.utn.dds.k3003.persistence.entity;

import jakarta.persistence.*;

/** Durable, narrow lock identity; rows deliberately survive a business rollback. */
@Entity
@Table(name = "logistica_claves_operacion")
public class ClaveOperacion {
  @Id @Column(length = 255) public String id;
}
