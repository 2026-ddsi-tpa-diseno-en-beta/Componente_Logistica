package ar.edu.utn.dds.k3003.persistence.adapter;

import ar.edu.utn.dds.k3003.model.Deposito;
import ar.edu.utn.dds.k3003.repositories.DepositoRepository;
import ar.edu.utn.dds.k3003.repositories.LogisticaDataMapper;
import ar.edu.utn.dds.k3003.persistence.repository.DepositoSpringDataRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class DepositoJpaRepositoryAdapter implements DepositoRepository {

  private final DepositoSpringDataRepository jpaRepository;
  private final LogisticaDataMapper mapper;

  public DepositoJpaRepositoryAdapter(DepositoSpringDataRepository jpaRepository, LogisticaDataMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Deposito save(Deposito deposito) {
    return mapper.toDeposito(jpaRepository.save(mapper.toDepositoEntity(deposito)));
  }

  @Override
  public Optional<Deposito> findById(String id) {
    return jpaRepository.findById(Long.valueOf(id)).map(mapper::toDeposito);
  }

  @Override
  public List<Deposito> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDeposito).collect(Collectors.toList());
  }

  @Override
  public void deleteById(String id) {
    jpaRepository.deleteById(Long.valueOf(id));
  }

  @Override
  public void deleteAll() {
    jpaRepository.deleteAll();
  }

  @Override
  public long count() {
    return jpaRepository.count();
  }
}
