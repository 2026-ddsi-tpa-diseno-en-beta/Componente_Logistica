package ar.edu.utn.dds.k3003.app;

import ar.edu.utn.dds.k3003.Fachada;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication(scanBasePackages = "ar.edu.utn.dds.k3003")
@EnableFeignClients(basePackages = "ar.edu.utn.dds.k3003.integration")
@EnableJpaRepositories(basePackages = "ar.edu.utn.dds.k3003.persistence.repository")
@EntityScan(basePackages = "ar.edu.utn.dds.k3003.persistence.entity")
@OpenAPIDefinition(
  info = @Info(
    title = "API | DonaTrack | LOGÍSTICA",
    description = "API del componente de Logística"
  )
)
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}