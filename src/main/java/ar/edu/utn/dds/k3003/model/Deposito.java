package ar.edu.utn.dds.k3003.model;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;

import java.util.ArrayList;
import java.util.List;

public class Deposito {

    private String id;
    private String nombre;
    private String direccion;
    private Integer capacidadMaxima;
    private List<Paquete> stockActual;
    private TipoAlgoritmoEnum algoritmo;

    public Deposito
    (
        String nombre,
        String direccion,
        Integer capacidadMaxima,
        List<Paquete> stockActual
    ) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.capacidadMaxima = capacidadMaxima;
        this.stockActual = stockActual != null ? stockActual : new ArrayList<>();
    }

    public void agregarPaquete(Paquete paquete) {
        if (paquete == null) throw new IllegalArgumentException("Paquete inválido");
        this.stockActual.add(paquete);
    }

    public int ocupacionActual() {
        return stockActual.stream().mapToInt(Paquete::getCantidad).sum();
    }

    public int capacidadDisponible() {
        return capacidadMaxima - ocupacionActual();
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Integer getCapacidadMaxima() {
        return capacidadMaxima;
    }
    public void setCapacidadMaxima(Integer capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public List<Paquete> getStockActual() {
        return stockActual;
    }
    public void setStockActual(List<Paquete> stockActual) {
        this.stockActual = stockActual;
    }

    public TipoAlgoritmoEnum getTipoAlgoritmo() {
        return algoritmo;
    }
    public void setTipoAlgoritmo(TipoAlgoritmoEnum algoritmo) {
        this.algoritmo = algoritmo;
    }
}