package ar.edu.utn.dds.k3003.services;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.TipoNecesidadMaterialEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;

import ar.edu.utn.dds.k3003.exceptions.BusinessRuleException;

import ar.edu.utn.dds.k3003.model.algoritmos.EstrategiaMatchMaking;
import ar.edu.utn.dds.k3003.model.algoritmos.EstrategiaPrioridadASubAtendidos;
import ar.edu.utn.dds.k3003.model.algoritmos.EstrategiaPrioridadPorScore;
import ar.edu.utn.dds.k3003.model.algoritmos.ResultadoMatchmaking;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Aplica las reglas de elegibilidad y posteriormente utiliza el algoritmo
 * configurado por el depósito para seleccionar una necesidad.
 */
@Service
public class MatchmakingService {

    public ResultadoMatchmaking procesar(
        TipoAlgoritmoEnum tipoAlgoritmo,
        List<NecesidadMaterialDTO> necesidades,
        String productoId,
        int cantidadDonada
    ) {
        validarEntrada(tipoAlgoritmo, productoId, cantidadDonada);

        if (necesidades == null || necesidades.isEmpty())
            return ResultadoMatchmaking.sinAsignacion(cantidadDonada);

        List<NecesidadMaterialDTO> elegibles = necesidades.stream()
            .filter(Objects::nonNull)
            .filter(necesidad -> productoId.equals(necesidad.productoSolicitadoID()))
            .filter(necesidad -> esElegible(necesidad, cantidadDonada))
            .toList();

        if (elegibles.isEmpty())
            return ResultadoMatchmaking.sinAsignacion(cantidadDonada);

        EstrategiaMatchMaking estrategia = crearEstrategia(tipoAlgoritmo);

        return estrategia.elegir(elegibles)
            .map(necesidad -> calcularResultado(necesidad, cantidadDonada))
            .orElseGet(() -> ResultadoMatchmaking.sinAsignacion(cantidadDonada));
    }

    /**
     * Una necesidad EXTRAORDINARIA puede recibir una cantidad menor que
     * su objetivo.
     * Una necesidad RECURRENTE solamente puede seleccionarse cuando la donación
     * alcanza para cubrir su cantidad objetivo.
     */
    private boolean esElegible(
        NecesidadMaterialDTO necesidad,
        int cantidadDonada
    ) {
        if (necesidad.cantidadObjetivo() == null ||
            necesidad.cantidadObjetivo() <= 0 ||
            necesidad.tipo() == null) 
            return false;

        return necesidad.tipo()
            == TipoNecesidadMaterialEnum.EXTRAORDINARIA
            || cantidadDonada >= necesidad.cantidadObjetivo();
    }

    private ResultadoMatchmaking calcularResultado(
        NecesidadMaterialDTO necesidad,
        int cantidadDonada
    ) {
        int cantidadAsignada = Math.min(
            cantidadDonada,
            necesidad.cantidadObjetivo()
        );

        int cantidadSobrante = cantidadDonada - cantidadAsignada;

        return ResultadoMatchmaking.conAsignacion(
            necesidad,
            cantidadAsignada,
            cantidadSobrante
        );
    }

    private EstrategiaMatchMaking crearEstrategia(TipoAlgoritmoEnum tipoAlgoritmo) {
        return switch (tipoAlgoritmo) {
            case SUB_ATENDIDOS ->
                new EstrategiaPrioridadASubAtendidos();

            case PRIORIDAD_POR_SCORE ->
                new EstrategiaPrioridadPorScore();
        };
    }

    private void validarEntrada(
        TipoAlgoritmoEnum tipoAlgoritmo,
        String productoId,
        int cantidadDonada
    ) {
        if (tipoAlgoritmo == null)
            throw new BusinessRuleException(
                "El depósito no tiene algoritmo de matchmaking configurado"
            );

        if (productoId == null || productoId.isBlank())
            throw new BusinessRuleException(
                "El producto de la donación es obligatorio"
            );

        if (cantidadDonada <= 0)
            throw new BusinessRuleException(
                "La cantidad donada debe ser positiva"
            );
    }
}
