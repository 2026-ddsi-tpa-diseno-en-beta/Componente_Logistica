package ar.edu.utn.dds.k3003.persistence;

import ar.edu.utn.dds.k3003.persistence.entity.RecepcionDonacion;
import ar.edu.utn.dds.k3003.persistence.entity.SolicitudStockProcesada;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/** Receipts join the same transaction as the packages and assignments they describe. */
@Component
public class RegistroOperaciones {
  @PersistenceContext private EntityManager em;
  public RecepcionDonacion recepcion(String id) { return em.find(RecepcionDonacion.class, id); }
  public void guardar(RecepcionDonacion receipt) { em.persist(receipt); }
  public SolicitudStockProcesada solicitud(String id) { return em.find(SolicitudStockProcesada.class, id); }
  public void guardar(SolicitudStockProcesada receipt) { em.persist(receipt); }
}
