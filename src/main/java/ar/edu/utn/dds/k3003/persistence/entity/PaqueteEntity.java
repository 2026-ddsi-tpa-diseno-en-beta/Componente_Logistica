package ar.edu.utn.dds.k3003.persistence.entity;

import ar.edu.utn.dds.k3003.model.EstadoPaquete;

import jakarta.persistence.*;

@Entity
@Table(name = "paquetes")
public class PaqueteEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "deposito_id", nullable = false)
  private DepositoEntity deposito;

  @Column(name = "donacion_id", nullable = false)
  private String donacionId;

  @Column(name = "producto_id", nullable = false)
  private String productoId;

  @Column(nullable = false)
  private Integer cantidad;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoPaquete estado;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public DepositoEntity getDeposito() { return deposito; }
  public void setDeposito(DepositoEntity deposito) { this.deposito = deposito; }

  public String getDonacionId() { return donacionId; }
  public void setDonacionId(String donacionId) { this.donacionId = donacionId; }

  public String getProductoId() { return productoId; }
  public void setProductoId(String productoId) { this.productoId = productoId; }

  public Integer getCantidad() { return cantidad; }
  public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

  public EstadoPaquete getEstadoPaquete() { return estado; }
  public void setEstadoPaquete(EstadoPaquete estado) { this.estado = estado; }
}
