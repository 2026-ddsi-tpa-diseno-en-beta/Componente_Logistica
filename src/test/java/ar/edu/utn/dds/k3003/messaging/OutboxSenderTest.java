package ar.edu.utn.dds.k3003.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.utn.dds.k3003.persistence.entity.ReadModelOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class OutboxSenderTest {
  @Test
  void failedBrokerSendLeavesTheSameEventPendingForRetry() {
    EntityManager em = mock(EntityManager.class);
    TypedQuery<ReadModelOutbox> query = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(ReadModelOutbox.class))).thenReturn(query);
    when(query.setMaxResults(anyInt())).thenReturn(query);
    when(query.setLockMode(any())).thenReturn(query);
    ReadModelOutbox row =
        new ReadModelOutbox("stable-id", "{\"eventId\":\"stable-id\",\"version\":1}");
    when(query.getResultList()).thenReturn(List.of(row));
    RabbitTemplate rabbit = mock(RabbitTemplate.class);
    doThrow(new org.springframework.amqp.AmqpException("broker unavailable"))
        .when(rabbit)
        .convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    var sender = new ReadModelOutboxSender(em, new ObjectMapper(), rabbit, "exchange", "key");
    sender.sendPending();
    assertFalse(row.sent);
    doAnswer(
            call -> {
              CorrelationData correlation = call.getArgument(3);
              assertEquals("stable-id", correlation.getId());
              correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
              return null;
            })
        .when(rabbit)
        .convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    sender.sendPending();
    assertTrue(row.sent);
  }

  @Test
  void negativeConfirmationDoesNotMarkTheEventSent() {
    EntityManager em = mock(EntityManager.class);
    TypedQuery<ReadModelOutbox> query = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(ReadModelOutbox.class))).thenReturn(query);
    when(query.setMaxResults(anyInt())).thenReturn(query);
    when(query.setLockMode(any())).thenReturn(query);
    ReadModelOutbox row = new ReadModelOutbox("id", "{}");
    when(query.getResultList()).thenReturn(List.of(row));
    RabbitTemplate rabbit = mock(RabbitTemplate.class);
    doAnswer(
            call -> {
              CorrelationData c = call.getArgument(3);
              c.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
              return null;
            })
        .when(rabbit)
        .convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    new ReadModelOutboxSender(em, new ObjectMapper(), rabbit, "exchange", "key").sendPending();
    assertFalse(row.sent);
  }
}
