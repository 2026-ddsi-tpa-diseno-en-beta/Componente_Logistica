package ar.edu.utn.dds.k3003.integration;

import ar.edu.utn.dds.k3003.app.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@AutoConfigureMockMvc
@SpringBootTest(classes=Application.class, properties={
    "spring.config.location=classpath:application.properties",
    "spring.datasource.url=jdbc:h2:mem:prometheus;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "integrations.donadores-url=http://localhost:18082",
    "integrations.donaciones-url=http://localhost:18081",
    "spring.rabbitmq.addresses=amqp://guest:guest@localhost:5672",
    "spring.rabbitmq.dynamic=false", "logistica.messaging.enabled=false",
    "management.health.rabbit.enabled=false"})
class ExportadorPrometheusTest {
  @Autowired MockMvc mvc;
  @Test void configuracionDeProduccionExportaMetricasSinCredencialesExternas() throws Exception {
    mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
        .andExpect(content().string(containsString("logistica_paquetes_pendientes")));
  }
}
