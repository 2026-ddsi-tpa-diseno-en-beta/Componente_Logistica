package ar.edu.utn.dds.k3003.messaging;

import ar.edu.utn.dds.k3003.persistence.entity.ReadModelOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Map;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
@Profile("!worker")
@ConditionalOnProperty(name = "donatrack.read-model.publisher.enabled", havingValue = "true")
public class ReadModelOutboxSender {
  private final EntityManager em;
  private final ObjectMapper mapper;
  private final RabbitTemplate rabbit;
  private final String exchange, key;

  public ReadModelOutboxSender(
      EntityManager em,
      ObjectMapper mapper,
      RabbitTemplate rabbit,
      @Value("${donatrack.read-model.exchange:donatrack.readmodel.exchange}") String exchange,
      @Value("${donatrack.read-model.routing-key:readmodel.changed}") String key) {
    this.em = em;
    this.mapper = mapper;
    this.rabbit = rabbit;
    this.exchange = exchange;
    this.key = key;
  }

  @Scheduled(
      initialDelayString = "${READ_MODEL_SEND_INITIAL_DELAY_MS:1000}",
      fixedDelayString = "${READ_MODEL_SEND_INTERVAL_MS:1000}")
  @Transactional
  public void sendPending() {
    var rows =
        em.createQuery("from ReadModelOutbox where sent=false order by id", ReadModelOutbox.class)
            .setMaxResults(50)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultList();
    for (var row : rows) {
      try {
        CorrelationData correlation = new CorrelationData(row.eventId);
        rabbit.convertAndSend(exchange, key, mapper.readValue(row.content, Map.class), correlation);
        var confirm = correlation.getFuture().get(10, java.util.concurrent.TimeUnit.SECONDS);
        if (!confirm.isAck() || correlation.getReturned() != null)
          throw new IllegalStateException("Evento no enrutado/confirmado");
        row.sent = true;
      } catch (Exception ex) {
        if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn(
                "readmodel.pending eventId={} reason={}",
                row.eventId,
                ex.getClass().getSimpleName());
        break;
      }
    }
  }
}
