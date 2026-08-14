package ar.edu.utn.dds.k3003.repositories.inmemory;

import ar.edu.utn.dds.k3003.model.Deposito;
import ar.edu.utn.dds.k3003.model.Paquete;
import ar.edu.utn.dds.k3003.repositories.DepositoRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryDepositoRepository implements DepositoRepository {

    private final Map<String, Deposito> depositos = new LinkedHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final AtomicLong nextPaqueteId = new AtomicLong(1);

    @Override
    public Deposito save(Deposito deposito) {
        if (deposito.getId() == null) {
            deposito.setId(String.valueOf(nextId.getAndIncrement()));
        }

        if (deposito.getStockActual() != null) {
            for (Paquete paquete : deposito.getStockActual()) {
                if (paquete.getId() == null) {
                    paquete.setId(String.valueOf(nextPaqueteId.getAndIncrement()));
                }
            }
        }

        depositos.put(deposito.getId(), deposito);
        return deposito;
    }

    @Override
    public Optional<Deposito> findById(String id) {
        return Optional.ofNullable(id).map(depositos::get);
    }

    @Override
    public List<Deposito> findAll() {
        return new ArrayList<>(depositos.values());
    }

    @Override
    public void deleteById(String id) {
        depositos.remove(id);
    }

    @Override
    public void deleteAll() {
        depositos.clear();
    }

    @Override
    public long count() {
        return depositos.size();
    }
}
