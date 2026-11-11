package ar.edu.utn.dds.k3003.messaging;

import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;
import ar.edu.utn.dds.k3003.persistence.entity.TrabajoMatchmaking;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
@Profile("!worker")
@ConditionalOnProperty(
    name = "logistica.messaging.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class MatchmakingOutboxSender {
  private final EntityManager em;
  private final ObjectMapper mapper;
  private final RabbitTemplate rabbit;

  public MatchmakingOutboxSender(EntityManager em, ObjectMapper mapper, RabbitTemplate rabbit) {
    this.em = em;
    this.mapper = mapper;
    this.rabbit = rabbit;
  }

  @Scheduled(
      initialDelayString = "${MATCHMAKING_SEND_INITIAL_DELAY_MS:1000}",
      fixedDelayString = "${MATCHMAKING_SEND_INTERVAL_MS:1000}")
  @Transactional
  public void sendPending() {
    var rows =
        em.createQuery(
                "from TrabajoMatchmaking where sent=false order by paqueteId",
                TrabajoMatchmaking.class)
            .setMaxResults(50)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultList();
    for (var row : rows) {
      try {
        var correlation = new CorrelationData("matchmaking:" + row.paqueteId);
        rabbit.convertAndSend(
            RabbitConfiguration.EXCHANGE,
            RabbitConfiguration.ROUTING_KEY,
            mapper.readValue(row.content, DonacionPendienteMessage.class),
            correlation);
        var confirm = correlation.getFuture().get(10, java.util.concurrent.TimeUnit.SECONDS);
        if (!confirm.isAck() || correlation.getReturned() != null)
          throw new IllegalStateException("Tarea de matchmaking no enrutada/confirmada");
        row.sent = true;
      } catch (Exception ex) {
        if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn(
                "matchmaking.pending paquete={} reason={}",
                row.paqueteId,
                ex.getClass().getSimpleName());
        break;
      }
    }
  }
}
