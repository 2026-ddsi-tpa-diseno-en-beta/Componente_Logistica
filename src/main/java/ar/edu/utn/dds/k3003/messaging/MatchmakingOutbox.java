package ar.edu.utn.dds.k3003.messaging;

import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;
import ar.edu.utn.dds.k3003.persistence.entity.TrabajoMatchmaking;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!worker")
@ConditionalOnProperty(
    name = "logistica.messaging.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class MatchmakingOutbox {
  private final EntityManager em;
  private final ObjectMapper mapper;

  public MatchmakingOutbox(EntityManager em, ObjectMapper mapper) {
    this.em = em;
    this.mapper = mapper;
  }

  /** Called inside the same transaction that persists the PENDIENTE package. */
  @Transactional
  public void guardar(DonacionPendienteMessage message) {
    guardar(message, false);
  }

  @Transactional
  public void recuperar(DonacionPendienteMessage message) {
    guardar(message, true);
  }

  private void guardar(DonacionPendienteMessage message, boolean recover) {
    try {
      var row = em.find(TrabajoMatchmaking.class, message.paqueteId());
      if (row == null)
        em.persist(new TrabajoMatchmaking(message.paqueteId(), mapper.writeValueAsString(message)));
      else if (recover) {
        row.content = mapper.writeValueAsString(message);
        row.sent = false;
      }
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new IllegalArgumentException("No se pudo guardar la tarea de matchmaking", ex);
    }
  }
}
