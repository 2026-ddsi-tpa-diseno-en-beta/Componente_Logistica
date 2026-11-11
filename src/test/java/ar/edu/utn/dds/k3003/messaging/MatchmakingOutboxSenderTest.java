package ar.edu.utn.dds.k3003.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.utn.dds.k3003.persistence.entity.TrabajoMatchmaking;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class MatchmakingOutboxSenderTest {
  @Test
  void lostBrokerConnectionRetainsTaskAndRetryKeepsItsIdentity() {
    var em = mock(EntityManager.class);
    TypedQuery<TrabajoMatchmaking> query = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(TrabajoMatchmaking.class))).thenReturn(query);
    when(query.setMaxResults(anyInt())).thenReturn(query);
    when(query.setLockMode(any())).thenReturn(query);
    var row =
        new TrabajoMatchmaking(
            "p1",
            "{\"depositoId\":\"d\",\"paqueteId\":\"p1\",\"donacionId\":\"x\",\"productoId\":\"p\",\"cantidad\":3,\"algoritmo\":\"SUB_ATENDIDOS\"}");
    when(query.getResultList()).thenReturn(List.of(row));
    var rabbit = mock(RabbitTemplate.class);
    var sender = new MatchmakingOutboxSender(em, new ObjectMapper(), rabbit);
    doThrow(new org.springframework.amqp.AmqpException("offline"))
        .when(rabbit)
        .convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    sender.sendPending();
    assertFalse(row.sent);
    doAnswer(
            call -> {
              CorrelationData c = call.getArgument(3);
              assertEquals("matchmaking:p1", c.getId());
              c.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
              return null;
            })
        .when(rabbit)
        .convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    sender.sendPending();
    assertFalse(row.sent);
    doAnswer(
            call -> {
              CorrelationData c = call.getArgument(3);
              assertEquals("matchmaking:p1", c.getId());
              c.getFuture().complete(new CorrelationData.Confirm(true, null));
              return null;
            })
        .when(rabbit)
        .convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    sender.sendPending();
    assertTrue(row.sent);
  }
}
