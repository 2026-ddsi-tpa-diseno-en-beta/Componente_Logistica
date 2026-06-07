package ar.edu.utn.dds.k3003.persistence.repository;

import ar.edu.utn.dds.k3003.persistence.entity.DepositoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositoSpringDataRepository 
    extends JpaRepository<DepositoEntity, Long> {

}