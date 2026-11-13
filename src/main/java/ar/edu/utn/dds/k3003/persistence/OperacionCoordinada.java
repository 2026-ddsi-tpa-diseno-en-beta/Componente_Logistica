package ar.edu.utn.dds.k3003.persistence;

import ar.edu.utn.dds.k3003.persistence.entity.ClaveOperacion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Provision before entering the business transaction, then lock only the requested identity. */
@Component
public class OperacionCoordinada {
  @PersistenceContext private EntityManager em;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate provision;
  private final TransactionTemplate transaction;

  public OperacionCoordinada(PlatformTransactionManager manager, JdbcTemplate jdbc) {
    this.jdbc = jdbc;
    provision = new TransactionTemplate(manager);
    provision.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public <T> T ejecutar(String clave, Supplier<T> operation) {
    return ejecutar(java.util.List.of(clave), operation);
  }

  public <T> T ejecutar(java.util.List<String> claves, Supplier<T> operation) {
    var ordered = claves.stream().distinct().sorted().toList();
    for (String clave : ordered) provisionar(clave);
    return transaction.execute(status -> {
      // OSIV or a caller may have read a deposit before waiting for this lock.
      em.flush();
      em.clear();
      for (String clave : ordered) {
        var row = em.find(ClaveOperacion.class, clave, LockModeType.PESSIMISTIC_WRITE);
        if (row == null) throw new IllegalStateException("Falta la clave de coordinación");
      }
      return operation.get();
    });
  }

  private void provisionar(String clave) {
    try {
      provision.executeWithoutResult(status -> {
        if (jdbc.queryForObject("select count(*) from logistica_claves_operacion where id = ?", Integer.class, clave) == 0)
          jdbc.update("insert into logistica_claves_operacion (id) values (?)", clave);
      });
    } catch (DataIntegrityViolationException race) {
      // This exception escapes REQUIRES_NEW: its rollback cannot poison the business transaction.
      Boolean exists = provision.execute(status -> jdbc.queryForObject(
          "select count(*) from logistica_claves_operacion where id = ?", Integer.class, clave) == 1);
      if (!Boolean.TRUE.equals(exists)) throw race;
    }
  }
}
