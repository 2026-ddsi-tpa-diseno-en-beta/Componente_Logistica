package ar.edu.utn.dds.k3003.repositories;

import ar.edu.utn.dds.k3003.model.Deposito;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import lombok.val;

public class InMemoryDepositoRepo implements DepositoRepository {

    private final List<Deposito> depositos;
    private final AtomicLong idSecuencial = new AtomicLong(1);

    public InMemoryDepositoRepo() {
        this.depositos = new ArrayList<>();
    }

    @Override
    public List<Deposito> findAll() {
        return new ArrayList<>(this.depositos);
    }

    @Override
    public Optional<Deposito> findById(String id) {
        return this.depositos.stream().filter(deposito -> deposito.getId().equals(id)).findFirst();
    }

    @Override
    public Deposito save(Deposito deposito) {
        if (deposito.getId() == null) {
            deposito.setId(String.valueOf(idSecuencial.getAndIncrement()));
            this.depositos.add(deposito);
            return deposito;
        }

        this.depositos.removeIf(dep -> dep.getId().equals(deposito.getId()));
        this.depositos.add(deposito);
        return deposito;
    }

    @Override
    public void deleteById(String id) {
        var deposito = this.findById(id);
        this.depositos.remove(deposito.get());
    }

    @Override
    public void deleteAll() {
        this.depositos.clear();
    }

    @Override
    public long count() {
        return this.depositos.size();
    }
}