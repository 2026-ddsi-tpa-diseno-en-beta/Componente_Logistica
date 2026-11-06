package ar.edu.utn.dds.k3003.integration;

/** Optional capability, preserving the original facade contract for existing integrations. */
public interface SatisfaccionIdempotente {
  void satisfacerEntrega(String necesidadID, Integer cantidad, String entregaID);
}
