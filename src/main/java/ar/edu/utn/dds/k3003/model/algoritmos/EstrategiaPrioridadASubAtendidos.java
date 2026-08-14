package ar.edu.utn.dds.k3003.model.algoritmos;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EstrategiaPrioridadASubAtendidos implements EstrategiaMatchMaking {

    @Override
    public Optional<NecesidadMatchmaking> elegir(
        List<NecesidadMatchmaking> necesidadesElegibles,
        int cantidadProducto
    ) {
        if (necesidadesElegibles == null || necesidadesElegibles.isEmpty())
            return Optional.empty();

        return necesidadesElegibles.stream()
            .max(
                Comparator.comparingInt(NecesidadMatchmaking::cantidadPendiente)
                    .thenComparingInt(NecesidadMatchmaking::nivelUrgencia)
            );
    }
}
