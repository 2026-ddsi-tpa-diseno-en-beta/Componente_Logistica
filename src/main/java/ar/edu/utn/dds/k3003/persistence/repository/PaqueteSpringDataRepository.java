package ar.edu.utn.dds.k3003.persistence.repository;

import ar.edu.utn.dds.k3003.persistence.entity.PaqueteEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaqueteSpringDataRepository 
    extends JpaRepository<PaqueteEntity, Long> {
  Optional<PaqueteEntity> findByDonacionId(String donacionId);
}