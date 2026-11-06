package ar.edu.utn.dds.k3003.messaging;

import ar.edu.utn.dds.k3003.persistence.entity.ReadModelOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Transactional outbox: domain writes and pending snapshots commit together. */
@Component
@Profile("!worker")
@ConditionalOnProperty(name = "donatrack.read-model.publisher.enabled", havingValue = "true")
public class ReadModelEventPublisher {
  private final EntityManager em;
  private final ObjectMapper mapper;
  private final String source;

  public ReadModelEventPublisher(
      EntityManager em,
      ObjectMapper mapper,
      @Value("${spring.application.name:Componente_Logistica}") String source) {
    this.em = em;
    this.mapper = mapper;
    this.source = source;
  }

  @Transactional
  public void publicar(String type, String id, String operation, Map<String, Object> payload) {
    if (type == null || id == null || id.isBlank())
      throw new IllegalArgumentException("Recurso requerido");
    try {
      Map<String, Object> event = new LinkedHashMap<>();
      String eventId = UUID.randomUUID().toString();
      event.put("eventId", eventId);
      event.put("schemaVersion", 1);
      event.put("source", source);
      event.put("resourceType", type);
      event.put("resourceId", id);
      event.put("operation", operation);
      event.put("occurredAt", Instant.now().toString());
      event.put("payload", payload);
      ReadModelOutbox row = new ReadModelOutbox(eventId, mapper.writeValueAsString(event));
      em.persist(row);
      em.flush();
      event.put("version", row.id);
      row.content = mapper.writeValueAsString(event);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new IllegalArgumentException("Snapshot no serializable", ex);
    }
  }
}
