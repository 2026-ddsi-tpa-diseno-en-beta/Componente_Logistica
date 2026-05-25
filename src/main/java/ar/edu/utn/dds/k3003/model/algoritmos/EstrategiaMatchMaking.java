package ar.edu.utn.dds.k3003.model.algoritmos;

import ar.edu.utn.dds.k3003.model.Paquete;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;


import java.util.List;
import java.util.Optional;

public interface EstrategiaMatchMaking {
    Optional<NecesidadMaterialDTO> elegir(List<NecesidadMaterialDTO> necesidades, Paquete paquete);
}