package ar.edu.utn.dds.k3003.model.algoritmos;

import ar.edu.utn.dds.k3003.model.Paquete;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;


import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EstrategiaPrioridadASubAtendidos implements EstrategiaMatchMaking {

    @Override
    public Optional<NecesidadMaterialDTO> elegir(List<NecesidadMaterialDTO> necesidades, Paquete paquete) {
        return necesidades.stream()
            .filter(necesidad -> necesidad.productoSolicitadoID().equals(paquete.getProducto()))
            .max(Comparator.comparingInt(NecesidadMaterialDTO::cantidadObjetivo));
    }
}