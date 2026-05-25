package ar.edu.utn.dds.k3003.repositories;

// DTOs
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsginacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.PaqueteDTO;

// models
import ar.edu.utn.dds.k3003.model.Asignacion;
import ar.edu.utn.dds.k3003.model.Deposito;
import ar.edu.utn.dds.k3003.model.Paquete;

import java.util.ArrayList;
import java.util.List;

public class LogisticaDataMapper {

    public DepositoDTO toDepositoDTO(Deposito deposito) {
        List<PaqueteDTO> stock = deposito.getStockActual() == null
                                 ? new ArrayList<>()
                                 : deposito.getStockActual().stream().map(this::toPaqueteDTO).toList();

        return new DepositoDTO
                (
                    deposito.getId(),
                    deposito.getTipoAlgoritmo(),
                    deposito.getNombre(),
                    deposito.getDireccion(),
                    deposito.getCapacidadMaxima(),
                    stock
                );
    }
    public Deposito toDeposito(DepositoDTO dto) {
        List<Paquete> stock = dto.stockActual() == null
                              ? new ArrayList<>()
                              : dto.stockActual().stream().map(this::toPaquete).toList();

        return new Deposito
                (
                    dto.nombre(), 
                    dto.direccion(), 
                    dto.capacidadMaxima(), 
                    stock
                );
    }

    public PaqueteDTO toPaqueteDTO(Paquete paquete) {
        return new PaqueteDTO
                (
                    paquete.getId(),
                    paquete.getDonacionId(),
                    paquete.getProducto(),
                    paquete.getCantidad()
                );
    }
    public Paquete toPaquete(PaqueteDTO dto) {
        Paquete paquete = new Paquete
                            (
                                dto.donacionID(), 
                                dto.producto(), 
                                dto.cantidad()
                            );
        paquete.setId(dto.id());
        return paquete;
    }

    public AsignacionDTO toAsignacionDTO(Asignacion asignacion) {
        return new AsignacionDTO
                (
                    asignacion.getId(),
                    asignacion.getPaqueteId(),
                    asignacion.getNecesidadId(),
                    asignacion.getFecha(),
                    EstadoAsginacionEnum.valueOf(asignacion.getEstado().name())
                );
    }
    public Asignacion toAsignacion(AsignacionDTO dto) {
        return new Asignacion
                (
                    dto.paqueteID(),
                    dto.necesidadID(),
                    dto.fecha(),
                    EstadoAsginacionEnum.valueOf(dto.estado().name())
                );
    }
}