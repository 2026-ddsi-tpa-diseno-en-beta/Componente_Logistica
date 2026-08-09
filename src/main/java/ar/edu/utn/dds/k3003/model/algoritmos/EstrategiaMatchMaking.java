package ar.edu.utn.dds.k3003.model.algoritmos;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;

import java.util.List;
import java.util.Optional;

// Solo define el criterio utilizado por un deposito para priorizar una necesidad entre aquellas que ya cumplen las reglas de asignacion
public interface EstrategiaMatchMaking {
    Optional<NecesidadMaterialDTO> elegir(List<NecesidadMaterialDTO> necesidadesElegibles);
}
