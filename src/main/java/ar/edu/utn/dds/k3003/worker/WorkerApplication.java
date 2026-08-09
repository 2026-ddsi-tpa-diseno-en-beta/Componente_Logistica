package ar.edu.utn.dds.k3003.worker;

import ar.edu.utn.dds.k3003.integration.FachadaDonadoresYEntidadesHttp;
import ar.edu.utn.dds.k3003.services.AdminDbService;
import ar.edu.utn.dds.k3003.services.LogisticaService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
    }
)
@ComponentScan(
    basePackages = {
        "ar.edu.utn.dds.k3003.worker",
        "ar.edu.utn.dds.k3003.messaging",
        "ar.edu.utn.dds.k3003.services"
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                AdminDbService.class,
                LogisticaService.class
            }
        )
    }
)
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            WorkerApplication.class,
            args
        );
    }

    @Bean
    FachadaDonadoresYEntidadesHttp donadoresClient(
        @Value("${integrations.donadores-url}")
        String donadoresUrl
    ) {
        return new FachadaDonadoresYEntidadesHttp(
            donadoresUrl
        );
    }
}
