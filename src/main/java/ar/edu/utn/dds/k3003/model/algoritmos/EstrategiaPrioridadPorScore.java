package ar.edu.utn.dds.k3003.model.algoritmos;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Prioriza la necesidad con mayor SCORE. El score considera su urgencia y cantidad objetivo
public class EstrategiaPrioridadPorScore implements EstrategiaMatchMaking {

    @Override
    public Optional<NecesidadMaterialDTO> elegir(List<NecesidadMaterialDTO> necesidadesElegibles) {
        if (necesidadesElegibles == null || necesidadesElegibles.isEmpty())
            return Optional.empty();

        return necesidadesElegibles.stream()
            .max(Comparator.comparingLong(this::calcularScore));
    }

    private long calcularScore(NecesidadMaterialDTO necesidad) {
        return (long) necesidad.nivelDeUrgencia() * necesidad.cantidadObjetivo();
    }
}