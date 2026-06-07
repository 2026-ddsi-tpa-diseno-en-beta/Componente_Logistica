package ar.edu.utn.dds.k3003.services;

import ar.edu.utn.dds.k3003.repositories.AsignacionRepository;
import ar.edu.utn.dds.k3003.repositories.DepositoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDbService {

  private final DepositoRepository depositoRepository;
  private final AsignacionRepository asignacionRepository;

  public AdminDbService(DepositoRepository depositoRepository, AsignacionRepository asignacionRepository) {
    this.depositoRepository = depositoRepository;
    this.asignacionRepository = asignacionRepository;
  }

  public record DbStatusResponse(long depositos, long paquetes, long asignaciones) {}

  public DbStatusResponse status() {
    long depositos = depositoRepository.count();
    long paquetes = depositoRepository.findAll().stream().mapToLong(d -> d.getStockActual().size()).sum();
    long asignaciones = asignacionRepository.count();

    return new DbStatusResponse(depositos, paquetes, asignaciones);
  }

  @Transactional
  public void clear() {
    asignacionRepository.deleteAll();
    depositoRepository.deleteAll();
  }
}
