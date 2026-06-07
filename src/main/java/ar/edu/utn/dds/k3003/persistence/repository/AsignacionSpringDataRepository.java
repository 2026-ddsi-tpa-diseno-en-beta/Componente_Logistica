package ar.edu.utn.dds.k3003.persistence.repository;

import ar.edu.utn.dds.k3003.persistence.entity.AsignacionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionSpringDataRepository 
    extends JpaRepository<AsignacionEntity, Long> {
  Optional<AsignacionEntity> findByPaquete_Id(Long paqueteId);
}