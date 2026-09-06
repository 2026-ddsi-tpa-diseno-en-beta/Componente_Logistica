package ar.edu.utn.dds.k3003.persistence.repository;

import ar.edu.utn.dds.k3003.persistence.entity.DepositoEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepositoSpringDataRepository extends JpaRepository<DepositoEntity, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from DepositoEntity d where d.id = :id")
  Optional<DepositoEntity> findLockedById(@Param("id") Long id);

}