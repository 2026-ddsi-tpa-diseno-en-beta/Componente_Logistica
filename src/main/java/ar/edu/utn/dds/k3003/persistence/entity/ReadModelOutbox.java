package ar.edu.utn.dds.k3003.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "read_model_outbox",
    indexes = @Index(name = "idx_readmodel_pending", columnList = "sent,id"))
public class ReadModelOutbox {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true)
  public String eventId;

  @Column(nullable = false, length = 1000000)
  public String content;

  public boolean sent;

  public ReadModelOutbox() {}

  public ReadModelOutbox(String eventId, String content) {
    this.eventId = eventId;
    this.content = content;
  }
}
