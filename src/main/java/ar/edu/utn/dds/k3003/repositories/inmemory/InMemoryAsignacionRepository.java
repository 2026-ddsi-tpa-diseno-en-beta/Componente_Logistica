package ar.edu.utn.dds.k3003.repositories.inmemory;

import ar.edu.utn.dds.k3003.model.Asignacion;
import ar.edu.utn.dds.k3003.repositories.AsignacionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAsignacionRepository implements AsignacionRepository {

    private final Map<String, Asignacion> asignaciones = new LinkedHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public Asignacion save(Asignacion asignacion) {
        if (asignacion.getId() == null) {
            asignacion.setId(String.valueOf(nextId.getAndIncrement()));
        }

        asignaciones.put(asignacion.getId(), asignacion);
        return asignacion;
    }

    @Override
    public Optional<Asignacion> findById(String id) {
        return Optional.ofNullable(id).map(asignaciones::get);
    }

    @Override
    public Optional<Asignacion> findByPaqueteId(String paqueteId) {
        return asignaciones.values().stream()
            .filter(asignacion -> paqueteId != null && paqueteId.equals(asignacion.getPaqueteId()))
            .findFirst();
    }

    @Override
    public List<Asignacion> findAll() {
        return new ArrayList<>(asignaciones.values());
    }

    @Override
    public void deleteAll() {
        asignaciones.clear();
    }

    @Override
    public long count() {
        return asignaciones.size();
    }
}
