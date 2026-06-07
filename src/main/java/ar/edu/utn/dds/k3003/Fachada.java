package ar.edu.utn.dds.k3003;

import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.EstadoDonacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.TipoNecesidadMaterialEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.PaqueteDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsginacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;

import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaLogistica;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonaciones;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonadoresYEntidades;

import ar.edu.utn.dds.k3003.exceptions.BusinessRuleException;
import ar.edu.utn.dds.k3003.exceptions.ConflictException;
import ar.edu.utn.dds.k3003.exceptions.ResourceNotFoundException;

import ar.edu.utn.dds.k3003.model.Asignacion;
import ar.edu.utn.dds.k3003.model.Deposito;
import ar.edu.utn.dds.k3003.model.Paquete;
import ar.edu.utn.dds.k3003.model.algoritmos.*;

import ar.edu.utn.dds.k3003.persistence.adapter.AsignacionJpaRepositoryAdapter;
import ar.edu.utn.dds.k3003.persistence.adapter.DepositoJpaRepositoryAdapter;

import ar.edu.utn.dds.k3003.repositories.AsignacionRepository;
import ar.edu.utn.dds.k3003.repositories.DepositoRepository;
import ar.edu.utn.dds.k3003.repositories.LogisticaDataMapper;

import ar.edu.utn.dds.k3003.repositories.InMemoryAsignacionRepo;
import ar.edu.utn.dds.k3003.repositories.InMemoryDepositoRepo;
import ar.edu.utn.dds.k3003.repositories.InMemoryDonadoresRepo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class Fachada implements FachadaLogistica {

  private final DepositoRepository depositoRepo;
  private final AsignacionRepository asignacionRepo;
  private final LogisticaDataMapper mapper;

  private FachadaDonadoresYEntidades fachadaDonadores;
  private FachadaDonaciones fachadaDonaciones;

  public Fachada() {
    this.depositoRepo = new InMemoryDepositoRepo();
    this.asignacionRepo = new InMemoryAsignacionRepo();
    this.mapper = new LogisticaDataMapper();
  }

  @Autowired
  public Fachada(
      DepositoJpaRepositoryAdapter depositoRepo,
      AsignacionJpaRepositoryAdapter asignacionRepo,
      LogisticaDataMapper mapper
  ) {
    this.depositoRepo = depositoRepo;
    this.asignacionRepo = asignacionRepo;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public DepositoDTO agregarDeposito(DepositoDTO depositoDto) {
    if (depositoDto == null || depositoDto.id() != null)
      throw new BusinessRuleException("Depósito inválido");

    Deposito deposito = mapper.toDeposito(depositoDto);
    Deposito guardado = depositoRepo.save(deposito);
    return mapper.toDepositoDTO(guardado);
  }

  @Override
  @Transactional(readOnly = true)
  public DepositoDTO buscarDepositoPorID(String depositoID) throws NoSuchElementException {
    Deposito deposito = depositoRepo.findById(depositoID)
        .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

    return mapper.toDepositoDTO(deposito);
  }

  @Transactional(readOnly = true)
  public List<DepositoDTO> buscarDepositos() {
    return depositoRepo.findAll().stream().map(mapper::toDepositoDTO).toList();
  }

  @Transactional
  public void eliminarDeposito(String depositoID) {
    if (depositoID == null || depositoID.isBlank())
      throw new BusinessRuleException("ID inválido");

    depositoRepo.findById(depositoID)
        .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

    depositoRepo.deleteById(depositoID);
  }

  @Override
  @Transactional
  public void setAlgoritmoMM(String depositoID, TipoAlgoritmoEnum tipoAlgoritmo) {
    Deposito deposito = depositoRepo.findById(depositoID)
        .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

    deposito.setTipoAlgoritmo(tipoAlgoritmo);
    depositoRepo.save(deposito);
  }

  @Override
  @Transactional
  public DepositoDTO gestionarDonacion(String depositoID, String donacionID, String productoID, Integer cantidad) {
    Deposito deposito = depositoRepo.findById(depositoID)
        .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

    if (donacionID == null || productoID == null || cantidad == null || cantidad <= 0)
      throw new BusinessRuleException("Datos inválidos para gestionar la donación");

    asegurarIntegraciones();
    List<NecesidadMaterialDTO> necesidades = fachadaDonadores.obtenerNecesidadesInsatisfechasDe(productoID);
    if (necesidades == null || necesidades.isEmpty())
      throw new ConflictException("No hay necesidades insatisfechas para ese producto");

    if (deposito.capacidadDisponible() < cantidad)
      throw new ConflictException("El depósito no tiene capacidad suficiente");

    Paquete paquete = new Paquete(donacionID, productoID, cantidad);
    deposito.agregarPaquete(paquete);

    Deposito depositoGuardado = depositoRepo.save(deposito);
    Paquete paquetePersistido = depositoGuardado.getStockActual().get(depositoGuardado.getStockActual().size() - 1);

    ejecutarMatchmaking(
        depositoID,
        mapper.toPaqueteDTO(paquetePersistido),
        necesidades
    );

    return mapper.toDepositoDTO(depositoGuardado);
  }

  @Override
  @Transactional
  public AsignacionDTO ejecutarMatchmaking(String depositoID, PaqueteDTO paqueteDTO, List<NecesidadMaterialDTO> necesidadesMateriales) {
    if (depositoID == null || paqueteDTO == null)
      throw new BusinessRuleException("Datos inválidos");

    Deposito deposito = depositoRepo.findById(depositoID)
        .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

    if (deposito.getTipoAlgoritmo() == null) 
      throw new ConflictException("El depósito no tiene algoritmo configurado");

    EstrategiaMatchMaking estrategia = switch (deposito.getTipoAlgoritmo()) {
      case SUB_ATENDIDOS -> new EstrategiaPrioridadASubAtendidos();
      case PRIORIDAD_POR_SCORE -> new EstrategiaPrioridadPorScore();
    };

    Paquete paquete = mapper.toPaquete(paqueteDTO);
    NecesidadMaterialDTO necesidadElegida = estrategia.elegir(necesidadesMateriales, paquete)
        .orElseThrow(() -> new ConflictException("No se encontró una necesidad compatible"));

    if (
        necesidadElegida.tipo() == TipoNecesidadMaterialEnum.RECURRENTE
        && paquete.getCantidad() < necesidadElegida.cantidadObjetivo()
    ) 
      throw new BusinessRuleException("No se permiten donaciones parciales para necesidades recurrentes");

    Asignacion asignacion = new Asignacion(
        paquete.getId(),
        necesidadElegida.id(),
        LocalDateTime.now(),
        EstadoAsginacionEnum.ASIGNADA
    );

    Asignacion guardada = asignacionRepo.save(asignacion);
    return mapper.toAsignacionDTO(guardada);
  }

  @Override
  @Transactional
  public void reportarEntrega(PaqueteDTO paqueteDTO) {
    if (paqueteDTO == null || paqueteDTO.id() == null)
      throw new BusinessRuleException("Paquete inválido");

    asegurarIntegraciones();

    Asignacion asignacion = asignacionRepo.findByPaqueteId(paqueteDTO.id())
        .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

    if (asignacion.getEstado() != EstadoAsginacionEnum.ASIGNADA)
      throw new ConflictException("La asignación no está en estado ASIGNADA");

    fachadaDonadores.satisfacerNecesidad(asignacion.getNecesidadId(), paqueteDTO.cantidad());

    asignacion.cambiarEstado(EstadoAsginacionEnum.COMPLETADA);
    asignacionRepo.save(asignacion);

    fachadaDonaciones.cambiarEstadoDeDonacion(paqueteDTO.donacionID(), EstadoDonacionEnum.ACEPTADA);
  }

  @Override
  @Transactional(readOnly = true)
  public AsignacionDTO buscarAsignacionPorPaqueteID(String paqueteID) throws NoSuchElementException {
    Asignacion asignacion = asignacionRepo.findByPaqueteId(paqueteID)
        .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

    return mapper.toAsignacionDTO(asignacion);
  }

  @Transactional(readOnly = true)
  public AsignacionDTO buscarAsignacionPorID(String asignacionID) {
    Asignacion asignacion = asignacionRepo.findById(asignacionID)
        .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

    return mapper.toAsignacionDTO(asignacion);
  }

  @Autowired(required = false)
  public void setFachadaDonadoresYEntidades(FachadaDonadoresYEntidades fachadaDonadoresYEntidades) {
    this.fachadaDonadores = fachadaDonadoresYEntidades;
  }

  @Autowired(required = false)
  public void setFachadaDonaciones(FachadaDonaciones fachadaDonaciones) {
    this.fachadaDonaciones = fachadaDonaciones;
  }

  private void asegurarIntegraciones() {
    if (fachadaDonadores == null || fachadaDonaciones == null)
      throw new ConflictException("Las fachadas externas no están configuradas");
  }
}
