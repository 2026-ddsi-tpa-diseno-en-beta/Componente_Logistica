package ar.edu.utn.dds.k3003.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "matchmaking_outbox",
    indexes = @Index(name = "idx_matchmaking_pending", columnList = "sent,paqueteId"))
public class TrabajoMatchmaking {
  @Id public String paqueteId;

  @Column(nullable = false, length = 100000)
  public String content;

  public boolean sent;

  protected TrabajoMatchmaking() {}

  public TrabajoMatchmaking(String paqueteId, String content) {
    this.paqueteId = paqueteId;
    this.content = content;
  }
}
