package ar.edu.utn.dds.k3003.model.algoritmos;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Prioriza la necesidad que requiere una mayor cantidad de unidades
public class EstrategiaPrioridadASubAtendidos implements EstrategiaMatchMaking {

    @Override
    public Optional<NecesidadMaterialDTO> elegir(List<NecesidadMaterialDTO> necesidadesElegibles) {
        if (necesidadesElegibles == null || necesidadesElegibles.isEmpty())
            return Optional.empty();

        return necesidadesElegibles.stream()
            .max(Comparator.comparingInt(NecesidadMaterialDTO::cantidadObjetivo));
    }
}
