package ar.edu.utn.dds.k3003.persistence.adapter;

import ar.edu.utn.dds.k3003.model.Asignacion;
import ar.edu.utn.dds.k3003.persistence.repository.AsignacionSpringDataRepository;
import ar.edu.utn.dds.k3003.repositories.AsignacionRepository;
import ar.edu.utn.dds.k3003.repositories.LogisticaDataMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class AsignacionJpaRepositoryAdapter implements AsignacionRepository {

  private final AsignacionSpringDataRepository jpaRepository;
  private final LogisticaDataMapper mapper;

  public AsignacionJpaRepositoryAdapter(AsignacionSpringDataRepository jpaRepository, LogisticaDataMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Asignacion save(Asignacion asignacion) {
    return mapper.toAsignacion(jpaRepository.save(mapper.toAsignacionEntity(asignacion)));
  }

  @Override
  public Optional<Asignacion> findById(String id) {
    return jpaRepository.findById(Long.valueOf(id)).map(mapper::toAsignacion);
  }

  @Override
  public Optional<Asignacion> findByPaqueteId(String paqueteId) {
    return jpaRepository
        .findByPaquete_Id(Long.valueOf(paqueteId))
        .map(mapper::toAsignacion);
  }

  @Override
  public List<Asignacion> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toAsignacion).collect(Collectors.toList());
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
