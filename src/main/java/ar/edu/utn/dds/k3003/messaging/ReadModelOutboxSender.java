package ar.edu.utn.dds.k3003.messaging;

import ar.edu.utn.dds.k3003.persistence.entity.ReadModelOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Retries durable events until RabbitMQ confirms routing and delivery to the exchange. */
@Component
@EnableScheduling
@Profile("!worker")
@ConditionalOnProperty(name = "donatrack.read-model.publisher.enabled", havingValue = "true")
public class ReadModelOutboxSender {
  private static final Logger log = LoggerFactory.getLogger(ReadModelOutboxSender.class);
  private final EntityManager entityManager;
  private final ObjectMapper objectMapper;
  private final RabbitTemplate rabbitTemplate;
  private final String exchange;
  private final String routingKey;

  public ReadModelOutboxSender(
      EntityManager entityManager,
      ObjectMapper objectMapper,
      RabbitTemplate rabbitTemplate,
      @Value("${donatrack.read-model.exchange:donatrack.readmodel.exchange}") String exchange,
      @Value("${donatrack.read-model.routing-key:readmodel.changed}") String routingKey) {
    this.entityManager = entityManager;
    this.objectMapper = objectMapper;
    this.rabbitTemplate = rabbitTemplate;
    this.exchange = exchange;
    this.routingKey = routingKey;
  }

  @Scheduled(
      initialDelayString = "${READ_MODEL_SEND_INITIAL_DELAY_MS:1000}",
      fixedDelayString = "${READ_MODEL_SEND_INTERVAL_MS:1000}")
  @Transactional
  public void sendPending() {
    var pending =
        entityManager
            .createQuery("from ReadModelOutbox where sent=false order by id", ReadModelOutbox.class)
            .setMaxResults(50)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultList();

    for (ReadModelOutbox row : pending) {
      try {
        CorrelationData correlation = new CorrelationData(row.eventId);
        Map<?, ?> event = objectMapper.readValue(row.content, Map.class);
        rabbitTemplate.convertAndSend(exchange, routingKey, event, correlation);
        var confirmation = correlation.getFuture().get(10, TimeUnit.SECONDS);
        if (!confirmation.isAck() || correlation.getReturned() != null) {
          throw new IllegalStateException("RabbitMQ no confirmó el evento o no pudo enrutarlo");
        }
        row.sent = true;
        log.info("readmodel.outbox.sent eventId={} outboxId={}", row.eventId, row.id);
      } catch (Exception exception) {
        if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
        log.warn(
            "readmodel.outbox.pending eventId={} reason={}",
            row.eventId,
            exception.getClass().getSimpleName());
        break;
      }
    }
  }
}
