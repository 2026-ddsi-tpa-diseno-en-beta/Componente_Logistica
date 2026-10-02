package ar.edu.utn.dds.k3003.worker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.ApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("worker")
@SpringBootTest(classes=WorkerApplication.class,properties={
  "spring.config.location=classpath:application-integration.properties",
  "spring.main.web-application-type=none"})
class WorkerArranqueTest {
  @Autowired ApplicationContext context;
  @Test void iniciaSinBaseDeDatosNiFachadaYRegistraConsumidor() {
    assertNotNull(context.getBean(AsignacionWorker.class));
    assertNotNull(context.getBean(LogisticaInternalClient.class));
    assertTrue(context.getBeansOfType(javax.sql.DataSource.class).isEmpty());
    assertTrue(context.getBeansOfType(ar.edu.utn.dds.k3003.Fachada.class).isEmpty());
  }
}
