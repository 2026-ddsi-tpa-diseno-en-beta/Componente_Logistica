package ar.edu.utn.dds.k3003.repositories;

import ar.edu.utn.dds.k3003.model.Asignacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAsignacionRepo implements AsignacionRepository {
    
    private final List<Asignacion> asignaciones;
    private final AtomicLong idSecuencial = new AtomicLong(1);

    public InMemoryAsignacionRepo() {
        this.asignaciones = new ArrayList<>();
    }

    @Override
    public Optional<Asignacion> findById(String id) {
        return this.asignaciones.stream().filter(asignacion -> asignacion.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<Asignacion> findByPaqueteId(String paqueteId) {
        return this.asignaciones.stream().filter(asignacion -> asignacion.getPaqueteId().equals(paqueteId)).findFirst();
    }

    @Override
    public List<Asignacion> findAll() {
        return new ArrayList<>(this.asignaciones);
    }

    @Override
    public Asignacion save(Asignacion asignacion) {
        if (asignacion.getId() == null) {
            asignacion.setId(String.valueOf(idSecuencial.getAndIncrement()));
            this.asignaciones.add(asignacion);
            return asignacion;
        }

        this.asignaciones.removeIf(asig -> asig.getId().equals(asignacion.getId()));
        this.asignaciones.add(asignacion);
        return asignacion;
    }

    public Asignacion deleteById(String id) {
        var asignacion = this.findById(id);
        this.asignaciones.remove(asignacion.get());
        return asignacion.get();
    }

    @Override
    public void deleteAll() {
        this.asignaciones.clear();
    }

    @Override
    public long count() {
        return this.asignaciones.size();
    }
}