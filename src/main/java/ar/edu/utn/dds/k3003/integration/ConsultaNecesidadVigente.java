package ar.edu.utn.dds.k3003.integration;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import java.time.LocalDate;

public interface ConsultaNecesidadVigente {
  record NecesidadVigente(NecesidadMaterialDTO necesidad, int recibida, LocalDate inicio) {}
  NecesidadVigente consultarNecesidad(String id);
}
