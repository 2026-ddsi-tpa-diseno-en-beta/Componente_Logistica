package ar.edu.utn.dds.k3003.repositories;

import ar.edu.utn.dds.k3003.model.Asignacion;
import java.util.List;
import java.util.Optional;

public interface AsignacionRepository {
  Asignacion save(Asignacion asignacion);

  Optional<Asignacion> findById(String id);

  Optional<Asignacion> findByPaqueteId(String paqueteId);

  List<Asignacion> findAll();

  void deleteAll();

  long count();
}
