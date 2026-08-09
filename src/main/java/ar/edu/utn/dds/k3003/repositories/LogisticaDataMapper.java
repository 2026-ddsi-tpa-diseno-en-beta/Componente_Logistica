package ar.edu.utn.dds.k3003.repositories;

// DTOs
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsignacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.PaqueteDTO;

// models
import ar.edu.utn.dds.k3003.model.Asignacion;
import ar.edu.utn.dds.k3003.model.Deposito;
import ar.edu.utn.dds.k3003.model.Paquete;
import ar.edu.utn.dds.k3003.model.CambioEstadoAsignacion;

// entities
import ar.edu.utn.dds.k3003.persistence.entity.AsignacionEntity;
import ar.edu.utn.dds.k3003.persistence.entity.CambioEstadoAsignacionEmbeddable;
import ar.edu.utn.dds.k3003.persistence.entity.DepositoEntity;
import ar.edu.utn.dds.k3003.persistence.entity.PaqueteEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LogisticaDataMapper {

    public Deposito toDeposito(DepositoDTO dto) {
        Deposito deposito = new Deposito(dto.nombre(), dto.direccion(), dto.capacidadMaxima(), new ArrayList<>());
        deposito.setId(dto.id());
        deposito.setTipoAlgoritmo(dto.algoritmo());

        if (dto.stockActual() != null)
            deposito.setStockActual(
                dto.stockActual()
                    .stream()
                    .map(this::toPaquete)
                    .collect(Collectors.toList()));

        return deposito;
    }

    public DepositoDTO toDepositoDTO(Deposito deposito) {
        List<PaqueteDTO> paquetes = deposito.getStockActual() == null 
            ? List.of()
            : deposito.getStockActual().stream().map(this::toPaqueteDTO).toList();

        return new DepositoDTO(
            deposito.getId(),
            deposito.getTipoAlgoritmo(),
            deposito.getNombre(),
            deposito.getDireccion(),
            deposito.getCapacidadMaxima(),
            paquetes
        );
    }

    public Paquete toPaquete(PaqueteDTO dto) {
        Paquete paquete = new Paquete(dto.donacionID(), dto.producto(), dto.cantidad());
        paquete.setId(dto.id());
        return paquete;
    }

    public PaqueteDTO toPaqueteDTO(Paquete paquete) {
        return new PaqueteDTO(paquete.getId(), paquete.getDonacionId(), paquete.getProducto(), paquete.getCantidad());
    }

    public Asignacion toAsignacion(AsignacionEntity entity) {
        Asignacion asignacion = new Asignacion(
            entity.getPaquete() == null ? null : String.valueOf(entity.getPaquete().getId()),
            entity.getNecesidadId(),
            entity.getFecha(),
            entity.getEstado(),
            entity.getCantidadAsignada(),
            entity.getOrigen()
        );
        asignacion.setId(entity.getId() == null ? null : String.valueOf(entity.getId()));

        if (entity.getHistorial() != null) {
            asignacion.getHistorial().clear();
            for (CambioEstadoAsignacionEmbeddable cambio : entity.getHistorial()) {
                asignacion.getHistorial().add(new CambioEstadoAsignacion(cambio.getEstado(), cambio.getFecha()));
            }
        }

        return asignacion;
    }

    public AsignacionDTO toAsignacionDTO(Asignacion asignacion) {
        return new AsignacionDTO(
            asignacion.getId(),
            asignacion.getPaqueteId(),
            asignacion.getNecesidadId(),
            asignacion.getFecha(),
            asignacion.getEstado(),
            asignacion.getCantidadAsignada(),
            asignacion.getOrigen()
        );
    }

    public AsignacionEntity toAsignacionEntity(Asignacion asignacion) {
        AsignacionEntity entity = new AsignacionEntity();

        if (asignacion.getId() != null)
            entity.setId(Long.valueOf(asignacion.getId()));

        PaqueteEntity paquete = new PaqueteEntity();

        if (asignacion.getPaqueteId() != null) 
            paquete.setId(Long.valueOf(asignacion.getPaqueteId()));

        entity.setPaquete(paquete);
        entity.setNecesidadId(asignacion.getNecesidadId());
        entity.setFecha(asignacion.getFecha());
        entity.setEstado(asignacion.getEstado());
        entity.setCantidadAsignada(asignacion.getCantidadAsignada());
        entity.setOrigen(asignacion.getOrigen());

        entity.setHistorial(
            asignacion.getHistorial()
                .stream()
                .map(cambioEstado -> new CambioEstadoAsignacionEmbeddable(cambioEstado.getEstado(), cambioEstado.getFecha()))
                .toList()
        );

        return entity;
    }

    public DepositoEntity toDepositoEntity(Deposito deposito) {
        DepositoEntity entity = new DepositoEntity();

        if (deposito.getId() != null)
            entity.setId(Long.valueOf(deposito.getId()));

        entity.setNombre(deposito.getNombre());
        entity.setDireccion(deposito.getDireccion());
        entity.setCapacidadMaxima(deposito.getCapacidadMaxima());
        entity.setAlgoritmoMm(deposito.getTipoAlgoritmo());

        if (deposito.getStockActual() != null) {
            List<PaqueteEntity> paquetes = new ArrayList<>();

            for (Paquete paquete : deposito.getStockActual()) {
                PaqueteEntity paqueteEntity = new PaqueteEntity();

                if (paquete.getId() != null)
                    paqueteEntity.setId(Long.valueOf(paquete.getId()));

                paqueteEntity.setDonacionId(paquete.getDonacionId());
                paqueteEntity.setProductoId(paquete.getProducto());
                paqueteEntity.setCantidad(paquete.getCantidad());
                paqueteEntity.setEstadoPaquete(paquete.getEstadoPaquete());
                paqueteEntity.setDeposito(entity);

                paquetes.add(paqueteEntity);
            }
            entity.setPaquetes(paquetes);
        }

        return entity;
    }

    public Deposito toDeposito(DepositoEntity entity) {
        List<Paquete> paquetes = entity.getPaquetes() == null 
            ? new ArrayList<>()
            : entity.getPaquetes().stream().map(this::toPaquete).collect(Collectors.toList());

        Deposito deposito = new Deposito(entity.getNombre(), entity.getDireccion(), entity.getCapacidadMaxima(), paquetes);
        deposito.setId(entity.getId() == null 
            ? null 
            : String.valueOf(entity.getId()));
            
        deposito.setTipoAlgoritmo(entity.getAlgoritmoMm());

        return deposito;
    }

    public Paquete toPaquete(PaqueteEntity entity) {
        Paquete paquete = new Paquete(entity.getDonacionId(), entity.getProductoId(), entity.getCantidad(), entity.getEstadoPaquete());
        paquete.setId(entity.getId() == null    
            ? null 
            : String.valueOf(entity.getId()));

        return paquete;
    }
}