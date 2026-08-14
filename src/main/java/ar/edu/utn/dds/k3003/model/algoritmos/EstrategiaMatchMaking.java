package ar.edu.utn.dds.k3003.model.algoritmos;

import java.util.List;
import java.util.Optional;

public interface EstrategiaMatchMaking {
    Optional<NecesidadMatchmaking> elegir(
        List<NecesidadMatchmaking> necesidadesElegibles,
        int cantidadProducto
    );
}
