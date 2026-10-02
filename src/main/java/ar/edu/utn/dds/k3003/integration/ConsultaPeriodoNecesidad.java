package ar.edu.utn.dds.k3003.integration;
import java.time.LocalDate;
/** Additional integration contract; the facade DTOs stay unchanged. */
public interface ConsultaPeriodoNecesidad {
  LocalDate inicioPeriodo(String necesidadId);
}
