package ar.edu.utn.dds.k3003.services;

import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;
import ar.edu.utn.dds.k3003.exceptions.BusinessRuleException;
import ar.edu.utn.dds.k3003.model.algoritmos.EstrategiaMatchMaking;
import ar.edu.utn.dds.k3003.model.algoritmos.EstrategiaPrioridadASubAtendidos;
import ar.edu.utn.dds.k3003.model.algoritmos.EstrategiaPrioridadPorScore;
import ar.edu.utn.dds.k3003.model.algoritmos.NecesidadMatchmaking;
import ar.edu.utn.dds.k3003.model.algoritmos.ResultadoMatchmaking;
import ar.edu.utn.dds.k3003.model.algoritmos.TipoNecesidadMatchmaking;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class MatchmakingService {

    public ResultadoMatchmaking procesar(
        TipoAlgoritmoEnum tipoAlgoritmo,
        List<NecesidadMaterialDTO> necesidades,
        String productoId,
        int cantidadDonada
    ) {
        return procesar(
            tipoAlgoritmo,
            necesidades,
            productoId,
            cantidadDonada,
            necesidadId -> 0
        );
    }

    public ResultadoMatchmaking procesar(
        TipoAlgoritmoEnum tipoAlgoritmo,
        List<NecesidadMaterialDTO> necesidades,
        String productoId,
        int cantidadDonada,
        Map<String, Integer> cantidadesSatisfechas
    ) {
        return procesar(
            tipoAlgoritmo,
            necesidades,
            productoId,
            cantidadDonada,
            necesidadId -> cantidadesSatisfechas.getOrDefault(necesidadId, 0)
        );
    }

    public ResultadoMatchmaking procesar(
        TipoAlgoritmoEnum tipoAlgoritmo,
        List<NecesidadMaterialDTO> necesidades,
        String productoId,
        int cantidadDonada,
        Function<String, Integer> cantidadSatisfechaProvider
    ) {
        validarEntrada(tipoAlgoritmo, productoId, cantidadDonada);

        if (necesidades == null || necesidades.isEmpty()) {
            return ResultadoMatchmaking.sinAsignacion(cantidadDonada);
        }

        Function<String, Integer> provider = cantidadSatisfechaProvider == null
            ? necesidadId -> 0
            : cantidadSatisfechaProvider;

        List<NecesidadMatchmaking> elegibles = necesidades.stream()
            .filter(Objects::nonNull)
            .filter(necesidad -> productoId.equals(necesidad.productoSolicitadoID()))
            .map(necesidad -> toDomain(necesidad, provider.apply(necesidad.id())))
            .filter(Objects::nonNull)
            .filter(necesidad -> esElegible(necesidad, cantidadDonada))
            .toList();

        if (elegibles.isEmpty()) {
            return ResultadoMatchmaking.sinAsignacion(cantidadDonada);
        }

        EstrategiaMatchMaking estrategia = crearEstrategia(tipoAlgoritmo);

        return estrategia.elegir(elegibles, cantidadDonada)
            .map(necesidad -> calcularResultado(necesidad, cantidadDonada))
            .orElseGet(() -> ResultadoMatchmaking.sinAsignacion(cantidadDonada));
    }

    private NecesidadMatchmaking toDomain(
        NecesidadMaterialDTO necesidad,
        Integer cantidadSatisfecha
    ) {
        if (necesidad.id() == null || necesidad.id().isBlank()
            || necesidad.productoSolicitadoID() == null
            || necesidad.productoSolicitadoID().isBlank()
            || necesidad.cantidadObjetivo() == null
            || necesidad.cantidadObjetivo() <= 0
            || necesidad.nivelDeUrgencia() == null
            || necesidad.nivelDeUrgencia() < 0
            || necesidad.tipo() == null) {
            return null;
        }

        int satisfecha = Math.max(0, cantidadSatisfecha == null ? 0 : cantidadSatisfecha);

        return new NecesidadMatchmaking(
            necesidad.id(),
            necesidad.entidadID(),
            necesidad.productoSolicitadoID(),
            necesidad.nivelDeUrgencia(),
            necesidad.cantidadObjetivo(),
            satisfecha,
            TipoNecesidadMatchmaking.valueOf(necesidad.tipo().name())
        );
    }

    private boolean esElegible(
        NecesidadMatchmaking necesidad,
        int cantidadDonada
    ) {
        int pendiente = necesidad.cantidadPendiente();

        if (pendiente <= 0) {
            return false;
        }

        return necesidad.tipo() == TipoNecesidadMatchmaking.EXTRAORDINARIA
            || cantidadDonada >= pendiente;
    }

    private ResultadoMatchmaking calcularResultado(
        NecesidadMatchmaking necesidad,
        int cantidadDonada
    ) {
        int cantidadAsignada = Math.min(
            cantidadDonada,
            necesidad.cantidadPendiente()
        );

        int cantidadSobrante = cantidadDonada - cantidadAsignada;

        return ResultadoMatchmaking.conAsignacion(
            necesidad,
            cantidadAsignada,
            cantidadSobrante
        );
    }

    private EstrategiaMatchMaking crearEstrategia(
        TipoAlgoritmoEnum tipoAlgoritmo
    ) {
        return switch (tipoAlgoritmo) {
            case SUB_ATENDIDOS -> new EstrategiaPrioridadASubAtendidos();
            case PRIORIDAD_POR_SCORE -> new EstrategiaPrioridadPorScore();
        };
    }

    private void validarEntrada(
        TipoAlgoritmoEnum tipoAlgoritmo,
        String productoId,
        int cantidadDonada
    ) {
        if (tipoAlgoritmo == null) {
            throw new BusinessRuleException(
                "El depósito no tiene algoritmo de matchmaking configurado"
            );
        }

        if (productoId == null || productoId.isBlank()) {
            throw new BusinessRuleException(
                "El producto de la donación es obligatorio"
            );
        }

        if (cantidadDonada <= 0) {
            throw new BusinessRuleException(
                "La cantidad donada debe ser positiva"
            );
        }
    }
}
