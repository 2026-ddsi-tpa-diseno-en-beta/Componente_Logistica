package ar.edu.utn.dds.k3003.app;

import ar.edu.utn.dds.k3003.Fachada;
import ar.edu.utn.dds.k3003.integration.FachadaDonacionesHttp;
import ar.edu.utn.dds.k3003.integration.FachadaDonadoresYEntidadesHttp;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
  scanBasePackages = "ar.edu.utn.dds.k3003",
  excludeName = {
    "ar.edu.utn.dds.k3003.worker.WorkerApplication"
  }
)
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

  @Bean
  FachadaDonadoresYEntidadesHttp donadoresClient(
      @Value("${integrations.donadores-url}") String donadoresUrl
  ) {
      return new FachadaDonadoresYEntidadesHttp(donadoresUrl);
  }

  @Bean
  CommandLineRunner configurarIntegraciones(
      Fachada fachada,
      FachadaDonadoresYEntidadesHttp donadoresClient,
      @Value("${integrations.donaciones-url}") String donacionesUrl) {

    return args -> {
      fachada.setFachadaDonaciones(
          new FachadaDonacionesHttp(donacionesUrl));

      fachada.setFachadaDonadoresYEntidades(donadoresClient);
    };
  }
}