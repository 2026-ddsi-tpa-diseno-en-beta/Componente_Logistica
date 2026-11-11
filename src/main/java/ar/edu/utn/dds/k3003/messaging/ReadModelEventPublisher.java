package ar.edu.utn.dds.k3003.messaging;

import ar.edu.utn.dds.k3003.persistence.entity.ReadModelOutbox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Transactional outbox: a snapshot is durable before RabbitMQ publication is attempted. */
@Component
@Profile("!worker")
@ConditionalOnProperty(name = "donatrack.read-model.publisher.enabled", havingValue = "true")
public class ReadModelEventPublisher {
  private final EntityManager entityManager;
  private final ObjectMapper objectMapper;
  private final String source;

  public ReadModelEventPublisher(
      EntityManager entityManager,
      ObjectMapper objectMapper,
      @Value("${spring.application.name:logistica}") String source) {
    this.entityManager = entityManager;
    this.objectMapper = objectMapper;
    this.source = source;
  }

  @Transactional
  public void publicar(
      String resourceType, String resourceId, String operation, Map<String, Object> payload) {
    if (resourceType == null || resourceType.isBlank()
        || resourceId == null || resourceId.isBlank()
        || operation == null || !operation.matches("(?i)UPSERT|DELETE")) {
      throw new IllegalArgumentException("Datos incompletos para publicar un evento del modelo de lectura");
    }

    try {
      String eventId = UUID.randomUUID().toString();
      Map<String, Object> event = new LinkedHashMap<>();
      event.put("eventId", eventId);
      event.put("schemaVersion", 1);
      event.put("source", source);
      event.put("resourceType", resourceType);
      event.put("resourceId", resourceId);
      event.put("operation", operation.toUpperCase());
      event.put("occurredAt", Instant.now().toString());
      event.put("payload", payload == null ? Map.of() : new LinkedHashMap<>(payload));

      ReadModelOutbox row = new ReadModelOutbox(eventId, objectMapper.writeValueAsString(event));
      entityManager.persist(row);
      entityManager.flush();

      // Outbox IDs are monotonically increasing within this component's source database.
      event.put("version", row.id);
      row.content = objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Snapshot del modelo de lectura no serializable", exception);
    }
  }
}
