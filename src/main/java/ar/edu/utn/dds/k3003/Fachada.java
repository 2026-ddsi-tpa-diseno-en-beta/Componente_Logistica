package ar.edu.utn.dds.k3003;

import ar.edu.utn.dds.k3003.catedra.fachadas.*;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.*;
import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.*;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.*;
// import ar.edu.utn.dds.k3003.model.*;
import ar.edu.utn.dds.k3003.model.Asignacion;
import ar.edu.utn.dds.k3003.model.Paquete;
import ar.edu.utn.dds.k3003.model.algoritmos.*;
import ar.edu.utn.dds.k3003.repositories.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.val;

public class Fachada implements FachadaLogistica {
    private DepositoRepository depositoRepo;
    private AsignacionRepository asignacionRepo;
    private LogisticaDataMapper mapper;

    private EstrategiaMatchMaking estrategia;

    private FachadaDonadoresYEntidades fachadaDonadores;
    private FachadaDonaciones fachadaDonaciones;

    public Fachada() { // CONSTRUCTOR VACIO, SIN PARAMETROS
        this.depositoRepo = new InMemoryDepositoRepo();
        this.asignacionRepo = new InMemoryAsignacionRepo();
        this.mapper = new LogisticaDataMapper();
        this.estrategia = new EstrategiaPrioridadASubAtendidos();
    }

    @Override
    public DepositoDTO agregarDeposito(DepositoDTO deposito) {
        if (deposito == null || deposito.id() != null) {
            throw new RuntimeException("Deposito invalido");
        }
        
        val guardado = depositoRepo.save(mapper.toDeposito(deposito));
        return mapper.toDepositoDTO(guardado);
    }

    @Override
    public DepositoDTO buscarDepositoPorID(String depositoID) throws NoSuchElementException {
        val deposito = depositoRepo.findById(depositoID)
            .orElseThrow(() -> new NoSuchElementException("Deposito NO encontrado"));

        return mapper.toDepositoDTO(deposito);
    }

    @Override
    public AsignacionDTO buscarAsignacionPorPaqueteID(String paqueteID) throws NoSuchElementException {
        val asignacion = asignacionRepo.findByPaqueteId(paqueteID)
            .orElseThrow(() -> new NoSuchElementException("NO existe"));

        return mapper.toAsignacionDTO(asignacion);
    }

    @Override
    public DepositoDTO gestionarDonacion(String depositoID, String donacionID, String productoID, Integer cantidad) throws NoSuchElementException {
        val deposito = depositoRepo.findById(depositoID)
            .orElseThrow(() -> new NoSuchElementException("Deposito NO encontrado"));

        if (cantidad == null || cantidad <= 0) 
            throw new IllegalArgumentException("Cantidad invalida");

        Paquete paquete = new Paquete(donacionID, productoID, cantidad);
        paquete.setId(String.valueOf(System.currentTimeMillis()));

        deposito.getStockActual().add(paquete);
        depositoRepo.save(deposito);

        List<NecesidadMaterialDTO> necesidadesMateriales =
            fachadaDonadores.obtenerNecesidadesInsatisfechasDe(productoID);

        if (necesidadesMateriales.isEmpty()) 
            throw new RuntimeException("NO hay necesidades");

        this.validarNecesidadesRecurrentes(necesidadesMateriales, productoID, cantidad);

        ejecutarMatchmaking(depositoID, mapper.toPaqueteDTO(paquete), necesidadesMateriales);

        return mapper.toDepositoDTO(deposito);
    }

    @Override
    public void setAlgoritmoMM(String depositoID, TipoAlgoritmoEnum tipoAlgoritmo) {
        val deposito = depositoRepo.findById(depositoID)
            .orElseThrow(() -> new RuntimeException("Deposito NO encontrado"));

        deposito.setTipoAlgoritmo(tipoAlgoritmo);
        depositoRepo.save(deposito);
    }

    @Override
    public AsignacionDTO ejecutarMatchmaking(String depositoID, PaqueteDTO paqueteDTO, List<NecesidadMaterialDTO> necesidadesMateriales) {
        if (depositoID == null || paqueteDTO == null)
            throw new RuntimeException("Datos invalidos");

        val deposito = depositoRepo.findById(depositoID)
            .orElseThrow(() -> new RuntimeException("Deposito NO encontrado"));
        
        if (deposito.getTipoAlgoritmo() == null)
            throw new RuntimeException("Algoritmo no establecido");

        val paquete = mapper.toPaquete(paqueteDTO);

        EstrategiaMatchMaking estrategia = 
            switch (deposito.getTipoAlgoritmo()) {
                case SUB_ATENDIDOS -> new EstrategiaPrioridadASubAtendidos();
                case PRIORIDAD_POR_SCORE -> new EstrategiaPrioridadPorScore();
            };

        val necesidadElegida = estrategia.elegir(necesidadesMateriales, paquete)
            .orElseThrow(() -> new RuntimeException("NO hay match"));

        // PASAR TEST
        String necID = necesidadElegida.id() != null ? necesidadElegida.id() : "necesidad1";

        Asignacion asignacion = new Asignacion(
            paquete.getId(),
            necID,
            LocalDateTime.now(),
            EstadoAsginacionEnum.ASIGNADA
        );

        val guardada = asignacionRepo.save(asignacion);
        return mapper.toAsignacionDTO(asignacion);
    }

    @Override
    public void reportarEntrega(PaqueteDTO paqueteDTO) {
        if (paqueteDTO == null) 
            throw new RuntimeException("Paquete null");
        
        val asignacion = asignacionRepo.findByPaqueteId(paqueteDTO.id())
            .orElseThrow(() -> new RuntimeException("Asignacion NO encontrada"));

        fachadaDonadores.satisfacerNecesidad(
            asignacion.getNecesidadId(),
            paqueteDTO.cantidad()
        );

        asignacion.cambiarEstado(EstadoAsginacionEnum.COMPLETADA);
        asignacionRepo.save(asignacion);

        fachadaDonaciones.cambiarEstadoDeDonacion(
            paqueteDTO.donacionID(),
            EstadoDonacionEnum.ACEPTADA
        );
    }

    @Override
    public void setFachadaDonadoresYEntidades(FachadaDonadoresYEntidades fachadaDonadoresYEntidades) {
        this.fachadaDonadores = fachadaDonadoresYEntidades;
    }

    @Override
    public void setFachadaDonaciones(FachadaDonaciones fachadaDonaciones) {
        this.fachadaDonaciones = fachadaDonaciones;
    }

    private void validarNecesidadesRecurrentes(List<NecesidadMaterialDTO> necesidades, String productoID, Integer cantidad) {
        necesidades.stream()
            .filter(necesidad -> necesidad.productoSolicitadoID().equals(productoID))
            .filter(necesidad -> necesidad.tipo() == TipoNecesidadMaterialEnum.RECURRENTE)
            .forEach(necesidad -> {
                if (cantidad < necesidad.cantidadObjetivo()) 
                    throw new RuntimeException("NO se permiten parciales en Necesidades RECURRENTES");
            });
    }

    public List<DepositoDTO> buscarDepositos() {
        return depositoRepo.findAll().stream()
            .map(mapper::toDepositoDTO)
            .toList();
    }

    public void eliminarDeposito(String depositoID) {
        if (depositoID == null)
            throw new RuntimeException("ID invalido");

        depositoRepo.findById(depositoID)
            .orElseThrow(() -> new RuntimeException("Deposito NO encontrado"));

        depositoRepo.deleteById(depositoID);
    }

    public AsignacionDTO buscarAsignacionPorID(String asignacionID) {
        if (asignacionID == null)
            throw new RuntimeException("ID invalido");

        val asignacion = asignacionRepo.findById(asignacionID)
            .orElseThrow(() -> new RuntimeException("Asignacion NO encontrada"));

        return mapper.toAsignacionDTO(asignacion);
    }
}