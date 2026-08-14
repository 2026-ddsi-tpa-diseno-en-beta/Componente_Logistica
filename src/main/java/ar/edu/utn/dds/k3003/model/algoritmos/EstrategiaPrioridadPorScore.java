package ar.edu.utn.dds.k3003.model.algoritmos;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EstrategiaPrioridadPorScore implements EstrategiaMatchMaking {

    @Override
    public Optional<NecesidadMatchmaking> elegir(
        List<NecesidadMatchmaking> necesidadesElegibles,
        int cantidadProducto
    ) {
        if (necesidadesElegibles == null || necesidadesElegibles.isEmpty()) {
            return Optional.empty();
        }

        if (cantidadProducto <= 0) {
            return Optional.empty();
        }

        return necesidadesElegibles.stream()
            .max(Comparator.comparingDouble(necesidad -> calcularScore(necesidad, cantidadProducto)));
    }

    private double calcularScore(
        NecesidadMatchmaking necesidad,
        int cantidadProducto
    ) {
        return necesidad.nivelUrgencia()
            / (cantidadProducto / (double) necesidad.cantidadObjetivo());
    }
}
