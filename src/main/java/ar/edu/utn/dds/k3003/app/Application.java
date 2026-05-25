package ar.edu.utn.dds.k3003.app;

import ar.edu.utn.dds.k3003.Fachada;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication(scanBasePackages = "ar.edu.utn.dds.k3003")
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

  @Bean
  public Fachada fachada() {
    return new Fachada();
  }
}