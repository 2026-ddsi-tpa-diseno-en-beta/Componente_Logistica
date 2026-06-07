package ar.edu.utn.dds.k3003.repositories;

import ar.edu.utn.dds.k3003.model.Deposito;
import java.util.Optional;
import java.util.List;

public interface DepositoRepository {
  Deposito save(Deposito deposito);

  Optional<Deposito> findById(String id);

  List<Deposito> findAll();

  void deleteById(String id);

  void deleteAll();

  long count();
}
