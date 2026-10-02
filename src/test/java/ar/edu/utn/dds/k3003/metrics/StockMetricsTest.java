package ar.edu.utn.dds.k3003.metrics;
import ar.edu.utn.dds.k3003.Fachada;
import ar.edu.utn.dds.k3003.model.*;
import ar.edu.utn.dds.k3003.repositories.*;
import ar.edu.utn.dds.k3003.repositories.inmemory.*;
import ar.edu.utn.dds.k3003.services.MatchmakingService;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsignacionEnum;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StockMetricsTest {
  @Test void exposesFullIndividualDepotAndPhysicalReservationsWithoutMutatingStock() {
    var deposits = new InMemoryDepositoRepository();
    var assignments = new InMemoryAsignacionRepository();
    var packet = new Paquete("d", "p", 10, EstadoPaquete.ASIGNADO);
    packet.setId("packet");
    var full = deposits.save(new Deposito("lleno", "dirección", 10, List.of(packet)));
    var empty = deposits.save(new Deposito("vacío", "dirección", 100, List.of()));
    assignments.save(new Asignacion("packet", "need", LocalDateTime.now().minusHours(25),
        EstadoAsignacionEnum.ASIGNADA, 10, OrigenAsignacion.MATCHMAKING));
    var fachada = new Fachada(deposits, assignments, new LogisticaDataMapper(), new MatchmakingService(), event -> {});
    var registry = new SimpleMeterRegistry();
    new StockMetrics(registry, fachada);
    assertEquals(10, registry.get("logistica.asignaciones.unidades.reservadas").gauge().value());
    assertTrue(registry.get("logistica.asignaciones.pendientes.antiguedad").gauge().value() >= 25*3600);
    assertEquals(10, registry.get("logistica.depositos.ocupacion").tag("deposito", full.getId()).gauge().value());
    assertEquals(0, registry.get("logistica.depositos.ocupacion").tag("deposito", empty.getId()).gauge().value());
    assertEquals(EstadoPaquete.ASIGNADO, packet.getEstadoPaquete());
  }
}
