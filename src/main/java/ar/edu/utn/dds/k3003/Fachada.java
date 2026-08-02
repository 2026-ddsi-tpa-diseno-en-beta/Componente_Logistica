package ar.edu.utn.dds.k3003;

import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.EstadoDonacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.PaqueteDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsignacionEnum;
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
import ar.edu.utn.dds.k3003.model.EstadoPaquete;
import ar.edu.utn.dds.k3003.model.OrigenAsignacion;
import ar.edu.utn.dds.k3003.model.algoritmos.*;

import ar.edu.utn.dds.k3003.persistence.adapter.AsignacionJpaRepositoryAdapter;
import ar.edu.utn.dds.k3003.persistence.adapter.DepositoJpaRepositoryAdapter;

import ar.edu.utn.dds.k3003.repositories.AsignacionRepository;
import ar.edu.utn.dds.k3003.repositories.DepositoRepository;
import ar.edu.utn.dds.k3003.repositories.LogisticaDataMapper;

import ar.edu.utn.dds.k3003.services.MatchmakingService;
import org.springframework.context.ApplicationEventPublisher;

import ar.edu.utn.dds.k3003.messaging.DonacionRegistradaEvent;
import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;

import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

  private final MatchmakingService matchmakingService;

  private final ApplicationEventPublisher eventPublisher;

  @Autowired
  public Fachada(
      DepositoJpaRepositoryAdapter depositoRepo,
      AsignacionJpaRepositoryAdapter asignacionRepo,
      LogisticaDataMapper mapper,
      MatchmakingService matchmakingService,
      ApplicationEventPublisher eventPublisher
  ) {
      this.depositoRepo = depositoRepo;
      this.asignacionRepo = asignacionRepo;
      this.mapper = mapper;
      this.matchmakingService = matchmakingService;
      this.eventPublisher = eventPublisher;
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
  public DepositoDTO gestionarDonacion(
      String depositoID,
      String donacionID,
      String productoID,
      Integer cantidad
  ) {
      if (depositoID == null || depositoID.isBlank()
          || donacionID == null || donacionID.isBlank()
          || productoID == null || productoID.isBlank()
          || cantidad == null || cantidad <= 0) {
          throw new BusinessRuleException(
              "Datos inválidos para gestionar la donación"
          );
      }

      Deposito deposito = depositoRepo.findById(depositoID)
          .orElseThrow(() ->
              new ResourceNotFoundException("Depósito no encontrado")
          );

      if (deposito.getTipoAlgoritmo() == null)
          throw new ConflictException(
              "El depósito no tiene algoritmo configurado"
          );

      if (!deposito.tieneLugar(cantidad))
          throw new ConflictException(
              "El depósito no tiene capacidad suficiente"
          );

      Paquete paquete = new Paquete(
          donacionID,
          productoID,
          cantidad,
          EstadoPaquete.PENDIENTE
      );

      deposito.almacenar(paquete);

      Deposito guardado = depositoRepo.save(deposito);

      Paquete persistido = guardado.getStockActual().stream()
          .filter(paq -> donacionID.equals(paq.getDonacionId()))
          .filter(paq -> productoID.equals(paq.getProducto()))
          .filter(paq -> paq.getEstadoPaquete() == EstadoPaquete.PENDIENTE)
          .reduce((primero, segundo) -> segundo)
          .orElseThrow(() ->
              new IllegalStateException(
                  "No se pudo recuperar el paquete persistido"
              )
          );

      eventPublisher.publishEvent(
          new DonacionRegistradaEvent(
              new DonacionPendienteMessage(
                  guardado.getId(),
                  persistido.getId(),
                  donacionID,
                  productoID,
                  cantidad,
                  guardado.getTipoAlgoritmo()
              )
          )
      );

      return mapper.toDepositoDTO(guardado);
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

    ResultadoMatchmaking resultado = matchmakingService.procesar(
        deposito.getTipoAlgoritmo(),
        necesidadesMateriales,
        paqueteDTO.productoID(),
        paqueteDTO.cantidad()
    );

    if (!resultado.tieneAsignacion()) {
        Paquete paquete = deposito.getStockActual().stream()
            .filter(p -> paqueteDTO.id().equals(p.getId()))
            .findFirst()
            .orElseThrow(() ->
                new ResourceNotFoundException("Paquete no encontrado")
            );

        paquete.marcarEnStock();
        depositoRepo.save(deposito);
        return null;
    }

    Paquete paquete = deposito.getStockActual().stream()
        .filter(p -> paqueteDTO.id().equals(p.getId()))
        .findFirst()
        .orElseThrow(() ->
            new ResourceNotFoundException("Paquete no encontrado")
        );

    paquete.setCantidad(resultado.cantidadAsignada());
    paquete.marcarAsignado();

    NecesidadMaterialDTO necesidad = resultado.necesidad().orElseThrow();

    Asignacion asignacion = new Asignacion(
        paqueteDTO.id(),
        necesidad.id(),
        resultado.cantidadAsignada(),
        OrigenAsignacion.MATCHMAKING,
        LocalDateTime.now(),
        EstadoAsignacionEnum.ASIGNADA
    );

    Asignacion guardada = asignacionRepo.save(asignacion);

    if (resultado.cantidadSobrante() > 0)
        deposito.almacenar(
            new Paquete(
                paqueteDTO.donacionID(),
                paqueteDTO.productoID(),
                resultado.cantidadSobrante(),
                EstadoPaquete.EN_STOCK
            )
        );

    depositoRepo.save(deposito);
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

    if (asignacion.getEstado() != EstadoAsignacionEnum.ASIGNADA)
      throw new ConflictException("La asignación no está en estado ASIGNADA");

    fachadaDonadores.satisfacerNecesidad(asignacion.getNecesidadId(), asignacion.getCantidadAsignada());

    asignacion.cambiarEstado(EstadoAsignacionEnum.COMPLETADA);
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

  @Override
  public void setFachadaDonadoresYEntidades(FachadaDonadoresYEntidades fachadaDonadoresYEntidades) {
    this.fachadaDonadores = fachadaDonadoresYEntidades;
  }

  @Override
  public void setFachadaDonaciones(FachadaDonaciones fachadaDonaciones) {
    this.fachadaDonaciones = fachadaDonaciones;
  }

  private void asegurarIntegraciones() {
    if (fachadaDonadores == null || fachadaDonaciones == null)
      throw new ConflictException("Las fachadas externas no están configuradas");
  }

  @Transactional
  public AsignacionDTO registrarResultadoMatchmaking(
      ResultadoMatchmakingRequest request
  ) {
      Deposito deposito = depositoRepo.findById(request.depositoId())
          .orElseThrow(() ->
              new ResourceNotFoundException("Depósito no encontrado")
          );

      Paquete paquete = deposito.getStockActual().stream()
          .filter(p -> request.paqueteId().equals(p.getId()))
          .findFirst()
          .orElseThrow(() ->
              new ResourceNotFoundException("Paquete no encontrado")
          );

      if (paquete.getEstadoPaquete() == EstadoPaquete.EN_STOCK)
          return null;

      if (paquete.getEstadoPaquete() == EstadoPaquete.ASIGNADO) {
          return asignacionRepo.findByPaqueteId(paquete.getId())
              .map(mapper::toAsignacionDTO)
              .orElseThrow(() ->
                  new IllegalStateException(
                      "El paquete está asignado pero no posee asignación"
                  )
              );
      }

      // Idempotencia ante reintentos o dos workers.
      var existente = asignacionRepo.findByPaqueteId(paquete.getId());

      if (existente.isPresent())
          return mapper.toAsignacionDTO(existente.get());

      if (!request.tieneAsignacion()) {
          paquete.marcarEnStock();
          depositoRepo.save(deposito);
          return null;
      }

      if (request.cantidadAsignada() + request.cantidadSobrante()
          != paquete.getCantidad()) {
          throw new BusinessRuleException(
              "El resultado no coincide con la cantidad original"
          );
      }

      paquete.setCantidad(request.cantidadAsignada());
      paquete.marcarAsignado();

      Asignacion asignacion = new Asignacion(
          paquete.getId(),
          request.necesidadId(),
          request.cantidadAsignada(),
          OrigenAsignacion.MATCHMAKING,
          LocalDateTime.now(),
          EstadoAsignacionEnum.ASIGNADA
      );

      Asignacion guardada = asignacionRepo.save(asignacion);

      if (request.cantidadSobrante() > 0) {
          deposito.almacenar(
              new Paquete(
                  paquete.getDonacionId(),
                  paquete.getProducto(),
                  request.cantidadSobrante(),
                  EstadoPaquete.EN_STOCK
              )
          );
      }

      depositoRepo.save(deposito);

      return mapper.toAsignacionDTO(guardada);
  }
}
