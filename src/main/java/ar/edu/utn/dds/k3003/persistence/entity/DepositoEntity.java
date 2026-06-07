package ar.edu.utn.dds.k3003.persistence.entity;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "depositos")
public class DepositoEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nombre;

  @Column(nullable = false)
  private String direccion;

  @Column(name = "capacidad_maxima", nullable = false)
  private Integer capacidadMaxima;

  @Enumerated(EnumType.STRING)
  @Column(name = "algoritmo_mm")
  private TipoAlgoritmoEnum algoritmoMm;

  @OneToMany(mappedBy = "deposito", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PaqueteEntity> paquetes = new ArrayList<>();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }

  public String getDireccion() { return direccion; }
  public void setDireccion(String direccion) { this.direccion = direccion; }

  public Integer getCapacidadMaxima() { return capacidadMaxima; }
  public void setCapacidadMaxima(Integer capacidadMaxima) { this.capacidadMaxima = capacidadMaxima; }

  public TipoAlgoritmoEnum getAlgoritmoMm() { return algoritmoMm; }
  public void setAlgoritmoMm(TipoAlgoritmoEnum algoritmoMm) { this.algoritmoMm = algoritmoMm; }

  public List<PaqueteEntity> getPaquetes() { return paquetes; }
  public void setPaquetes(List<PaqueteEntity> paquetes) { this.paquetes = paquetes; }
}
