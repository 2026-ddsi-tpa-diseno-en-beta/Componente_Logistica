package ar.edu.utn.dds.k3003.model;

public class Paquete {

    private String id;
    private String donacionId;
    private String producto;
    private Integer cantidad;
    private EstadoPaquete estado;

    public Paquete(String donacionId, String producto, Integer cantidad) {
        this(donacionId, producto, cantidad, EstadoPaquete.PENDIENTE);
    }

    public Paquete
    (
        String donacionId, 
        String producto, 
        Integer cantidad,
        EstadoPaquete estado
    ) {
        this.donacionId = donacionId;
        this.producto = producto;
        this.cantidad = cantidad;
        this.estado = estado;
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

    public EstadoPaquete getEstadoPaquete() {
        return estado;
    }

    public void setEstadoPaquete(EstadoPaquete estado) {
        this.estado = estado;
    }

    public void marcarEnStock() {
        this.estado = EstadoPaquete.EN_STOCK;
    }

    public void marcarAsignado() {
        this.estado = EstadoPaquete.ASIGNADO;
    }

    public void marcarEntregado() {
        this.estado = EstadoPaquete.ENTREGADO;
    }
}
