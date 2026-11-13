package ar.edu.utn.dds.k3003.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.utn.dds.k3003.Fachada;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;
import ar.edu.utn.dds.k3003.persistence.adapter.AsignacionJpaRepositoryAdapter;
import ar.edu.utn.dds.k3003.persistence.adapter.DepositoJpaRepositoryAdapter;
import ar.edu.utn.dds.k3003.persistence.entity.PaqueteEntity;
import ar.edu.utn.dds.k3003.persistence.entity.TrabajoMatchmaking;
import ar.edu.utn.dds.k3003.persistence.repository.DepositoSpringDataRepository;
import ar.edu.utn.dds.k3003.repositories.LogisticaDataMapper;
import ar.edu.utn.dds.k3003.services.MatchmakingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@org.springframework.test.context.ActiveProfiles("registration-outbox-test")
@ContextConfiguration(classes = MatchmakingRegistrationOutboxTest.Config.class)
@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
class MatchmakingRegistrationOutboxTest {
  @Configuration
  @Profile("registration-outbox-test")
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = TrabajoMatchmaking.class)
  @EnableJpaRepositories(basePackageClasses = DepositoSpringDataRepository.class)
  @Import({Fachada.class, LogisticaDataMapper.class, MatchmakingService.class,
      DepositoJpaRepositoryAdapter.class, AsignacionJpaRepositoryAdapter.class,
      MatchmakingOutbox.class, ar.edu.utn.dds.k3003.persistence.OperacionCoordinada.class,
      ar.edu.utn.dds.k3003.persistence.RegistroOperaciones.class})
  static class Config {
    @Bean ObjectMapper mapper() {
      return new ObjectMapper().findAndRegisterModules();
    }
  }

  @Autowired Fachada facade;
  @Autowired MatchmakingOutbox outbox;
  @Autowired EntityManager em;
  @Autowired ObjectMapper mapper;

  String depot() {
    String id = facade.agregarDeposito(
        new DepositoDTO(null, null, "Depósito", "Medrano", 100, null)).id();
    facade.setAlgoritmoMM(id, TipoAlgoritmoEnum.SUB_ATENDIDOS);
    return id;
  }

  @Test
  void receptionCommitsItsPackageAndRecoverableTaskTogether() throws Exception {
    String depot = depot();
    var packet = facade.gestionarDonacion(depot, "durable-reception", "p", 3)
        .stockActual().getFirst();

    assertNotNull(em.find(PaqueteEntity.class, Long.valueOf(packet.id())));
    assertNotNull(em.find(ar.edu.utn.dds.k3003.persistence.entity.RecepcionDonacion.class, "durable-reception"));
    var task = em.find(TrabajoMatchmaking.class, packet.id());
    assertNotNull(task);
    assertFalse(task.sent);
    var message = mapper.readTree(task.content);
    assertEquals(packet.id(), message.path("paqueteId").asText());
    assertEquals(depot, message.path("depositoId").asText());
    assertEquals("durable-reception", message.path("donacionId").asText());
    assertEquals(3, message.path("cantidad").asInt());
  }

  @Test
  void failureToPersistTheTaskRollsBackTheReception() {
    String depot = depot();
    var failing = mock(MatchmakingOutbox.class);
    doThrow(new IllegalStateException("No se pudo persistir tarea"))
        .when(failing).guardar(any());
    ReflectionTestUtils.setField(facade, "matchmakingOutbox", failing);
    try {
      assertThrows(IllegalStateException.class,
          () -> facade.gestionarDonacion(depot, "failed-reception", "p", 3));
    } finally {
      ReflectionTestUtils.setField(facade, "matchmakingOutbox", outbox);
    }
    assertEquals(0L, em.createQuery(
        "select count(p) from PaqueteEntity p where p.donacionId = 'failed-reception'", Long.class)
        .getSingleResult());
    assertNull(em.find(ar.edu.utn.dds.k3003.persistence.entity.RecepcionDonacion.class, "failed-reception"));
  }
}
