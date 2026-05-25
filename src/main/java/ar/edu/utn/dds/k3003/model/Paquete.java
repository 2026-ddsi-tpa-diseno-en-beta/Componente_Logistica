package ar.edu.utn.dds.k3003.model;

public class Paquete {

    private String id;
    private String donacionId;
    private String producto;
    private Integer cantidad;

    public Paquete
    (
        String donacionId, 
        String producto, 
        Integer cantidad
    ) {
        this.donacionId = donacionId;
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getDonacionId() {
        return donacionId;
    }
    public void setDonacionId(String donacionId) {
        this.donacionId = donacionId;
    } 

    public String getProducto() {
        return producto;
    }
    public void setProducto(String producto) {
        this.producto = producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }
    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}