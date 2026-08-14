package ar.edu.utn.dds.k3003.model.algoritmos;

public record NecesidadMatchmaking(
    String id,
    String entidadId,
    String productoId,
    int nivelUrgencia,
    int cantidadObjetivo,
    int cantidadSatisfecha,
    TipoNecesidadMatchmaking tipo
) {

    public int cantidadPendiente() {
        return Math.max(0, cantidadObjetivo - cantidadSatisfecha);
    }
}
