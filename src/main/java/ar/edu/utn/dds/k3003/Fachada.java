package ar.edu.utn.dds.k3003;

import ar.edu.utn.dds.k3003.catedra.dtos.donaciones.EstadoDonacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.NecesidadMaterialDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.EstadoAsignacionEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.PaqueteDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.StockDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonaciones;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaDonadoresYEntidades;
import ar.edu.utn.dds.k3003.catedra.fachadas.FachadaLogistica;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;
import ar.edu.utn.dds.k3003.exceptions.BusinessRuleException;
import ar.edu.utn.dds.k3003.exceptions.ConflictException;
import ar.edu.utn.dds.k3003.exceptions.ResourceNotFoundException;
import ar.edu.utn.dds.k3003.messaging.DonacionRegistradaEvent;
import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;
import ar.edu.utn.dds.k3003.model.Asignacion;
import ar.edu.utn.dds.k3003.model.Deposito;
import ar.edu.utn.dds.k3003.model.EstadoPaquete;
import ar.edu.utn.dds.k3003.model.OrigenAsignacion;
import ar.edu.utn.dds.k3003.model.Paquete;
import ar.edu.utn.dds.k3003.model.algoritmos.ResultadoMatchmaking;
import ar.edu.utn.dds.k3003.persistence.adapter.AsignacionJpaRepositoryAdapter;
import ar.edu.utn.dds.k3003.persistence.adapter.DepositoJpaRepositoryAdapter;
import ar.edu.utn.dds.k3003.repositories.AsignacionRepository;
import ar.edu.utn.dds.k3003.repositories.DepositoRepository;
import ar.edu.utn.dds.k3003.repositories.LogisticaDataMapper;
import ar.edu.utn.dds.k3003.repositories.inmemory.InMemoryAsignacionRepository;
import ar.edu.utn.dds.k3003.repositories.inmemory.InMemoryDepositoRepository;
import ar.edu.utn.dds.k3003.services.MatchmakingService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Fachada implements FachadaLogistica {

    private final DepositoRepository depositoRepo;
    private final AsignacionRepository asignacionRepo;
    private final LogisticaDataMapper mapper;
    private final MatchmakingService matchmakingService;
    private final ApplicationEventPublisher eventPublisher;

    private FachadaDonadoresYEntidades fachadaDonadores;
    private FachadaDonaciones fachadaDonaciones;

    public Fachada() {
        this(
            new InMemoryDepositoRepository(),
            new InMemoryAsignacionRepository(),
            new LogisticaDataMapper(),
            new MatchmakingService(),
            event -> {}
        );
    }

    @Autowired
    public Fachada(
        DepositoRepository depositoRepo,
        AsignacionRepository asignacionRepo,
        LogisticaDataMapper mapper,
        MatchmakingService matchmakingService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.depositoRepo = Objects.requireNonNull(depositoRepo);
        this.asignacionRepo = Objects.requireNonNull(asignacionRepo);
        this.mapper = Objects.requireNonNull(mapper);
        this.matchmakingService = Objects.requireNonNull(matchmakingService);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    @Transactional
    public DepositoDTO agregarDeposito(DepositoDTO depositoDto) {
        if (depositoDto == null || depositoDto.id() != null) {
            throw new BusinessRuleException("Depósito inválido");
        }

        if (depositoDto.nombre() == null || depositoDto.nombre().isBlank()
            || depositoDto.direccion() == null || depositoDto.direccion().isBlank()
            || depositoDto.capacidadMaxima() == null
            || depositoDto.capacidadMaxima() <= 0) {
            throw new BusinessRuleException("Datos inválidos para el depósito");
        }

        Deposito deposito = mapper.toDeposito(depositoDto);
        Deposito guardado = depositoRepo.save(deposito);
        return mapper.toDepositoDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public DepositoDTO buscarDepositoPorID(String depositoID) throws NoSuchElementException {
        validarId(depositoID, "ID de depósito inválido");

        Deposito deposito = depositoRepo.findById(depositoID)
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

        return mapper.toDepositoDTO(deposito);
    }

    @Transactional(readOnly = true)
    public List<DepositoDTO> buscarDepositos() {
        return depositoRepo.findAll().stream()
            .map(mapper::toDepositoDTO)
            .toList();
    }

    @Transactional
    public void eliminarDeposito(String depositoID) {
        validarId(depositoID, "ID de depósito inválido");

        depositoRepo.findById(depositoID)
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

        depositoRepo.deleteById(depositoID);
    }

    @Override
    @Transactional
    public void setAlgoritmoMM(String depositoID, TipoAlgoritmoEnum tipoAlgoritmo) {
        validarId(depositoID, "ID de depósito inválido");

        if (tipoAlgoritmo == null) {
            throw new BusinessRuleException("El algoritmo es obligatorio");
        }

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
        validarId(depositoID, "ID de depósito inválido");
        validarId(donacionID, "ID de donación inválido");
        validarId(productoID, "ID de producto inválido");

        if (cantidad == null || cantidad <= 0) {
            throw new BusinessRuleException("La cantidad donada debe ser positiva");
        }

        Deposito deposito = depositoRepo.findById(depositoID)
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

        if (deposito.getTipoAlgoritmo() == null) {
            throw new ConflictException("El depósito no tiene algoritmo configurado");
        }

        if (!deposito.tieneLugar(cantidad)) {
            throw new ConflictException("El depósito no tiene capacidad suficiente");
        }

        Paquete paquete = new Paquete(
            donacionID,
            productoID,
            cantidad,
            EstadoPaquete.PENDIENTE
        );

        deposito.almacenar(paquete);
        Deposito guardado = depositoRepo.save(deposito);

        if (paquete.getId() == null) {
            throw new IllegalStateException("No se pudo identificar el paquete persistido");
        }

        eventPublisher.publishEvent(
            new DonacionRegistradaEvent(
                new DonacionPendienteMessage(
                    guardado.getId(),
                    paquete.getId(),
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
    public AsignacionDTO ejecutarMatchmaking(
        String depositoID,
        PaqueteDTO paqueteDTO,
        List<NecesidadMaterialDTO> necesidadesMateriales
    ) {
        validarId(depositoID, "ID de depósito inválido");

        if (paqueteDTO == null || paqueteDTO.id() == null) {
            throw new BusinessRuleException("Paquete inválido");
        }

        Deposito deposito = depositoRepo.findById(depositoID)
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

        if (deposito.getTipoAlgoritmo() == null) {
            throw new ConflictException("El depósito no tiene algoritmo configurado");
        }

        ResultadoMatchmaking resultado = matchmakingService.procesar(
            deposito.getTipoAlgoritmo(),
            necesidadesMateriales,
            paqueteDTO.producto(),
            paqueteDTO.cantidad(),
            necesidadId -> cantidadAsignadaPorNecesidad(necesidadId)
        );

        Paquete paquete = buscarPaquete(deposito, paqueteDTO.id());

        return registrarResultadoInternamente(
            deposito,
            paquete,
            resultado
        );
    }

    @Override
    @Transactional
    public void reportarEntrega(PaqueteDTO paqueteDTO) {
        if (paqueteDTO == null || paqueteDTO.id() == null) {
            throw new BusinessRuleException("Paquete inválido");
        }

        asegurarIntegraciones();

        Asignacion asignacion = asignacionRepo.findByPaqueteId(paqueteDTO.id())
            .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        if (asignacion.getEstado() != EstadoAsignacionEnum.ASIGNADA) {
            throw new ConflictException("La asignación no está en estado ASIGNADA");
        }

        Deposito deposito = encontrarDepositoPorPaquete(paqueteDTO.id());
        Paquete paquete = buscarPaquete(deposito, paqueteDTO.id());

        asignacion.cambiarEstado(EstadoAsignacionEnum.COMPLETADA);
        asignacionRepo.save(asignacion);

        fachadaDonadores.satisfacerNecesidad(
            asignacion.getNecesidadId(),
            asignacion.getCantidadAsignada()
        );

        paquete.marcarEntregado();
        depositoRepo.save(deposito);

        fachadaDonaciones.cambiarEstadoDeDonacion(
            paquete.getDonacionId(),
            EstadoDonacionEnum.ACEPTADA
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AsignacionDTO buscarAsignacionPorPaqueteID(String paqueteID) throws NoSuchElementException {
        validarId(paqueteID, "ID de paquete inválido");

        Asignacion asignacion = asignacionRepo.findByPaqueteId(paqueteID)
            .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        return mapper.toAsignacionDTO(asignacion);
    }

    @Transactional(readOnly = true)
    public AsignacionDTO buscarAsignacionPorID(String asignacionID) {
        validarId(asignacionID, "ID de asignación inválido");

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

    @Transactional
    public AsignacionDTO registrarResultadoMatchmaking(
        ResultadoMatchmakingRequest request
    ) {
        validarResultado(request);

        Deposito deposito = depositoRepo.findById(request.depositoId())
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

        Paquete paquete = buscarPaquete(deposito, request.paqueteId());

        if (paquete.getEstadoPaquete() == EstadoPaquete.EN_STOCK) {
            return null;
        }

        if (paquete.getEstadoPaquete() == EstadoPaquete.ASIGNADO) {
            return asignacionRepo.findByPaqueteId(paquete.getId())
                .map(mapper::toAsignacionDTO)
                .orElseThrow(() ->
                    new IllegalStateException("Paquete asignado sin asignación"));
        }

        if (paquete.getEstadoPaquete() != EstadoPaquete.PENDIENTE) {
            throw new ConflictException("El paquete no puede procesar un resultado de matchmaking");
        }

        int cantidadOriginal = paquete.getCantidad();

        if (request.cantidadAsignada() + request.cantidadSobrante() != cantidadOriginal) {
            throw new BusinessRuleException(
                "El resultado no coincide con la cantidad original"
            );
        }

        if (!request.tieneAsignacion()) {
            paquete.marcarEnStock();
            depositoRepo.save(deposito);
            return null;
        }

        if (request.cantidadAsignada() <= 0 || request.necesidadId() == null) {
            throw new BusinessRuleException("Resultado de asignación inválido");
        }

        if (request.cantidadSobrante() < 0) {
            throw new BusinessRuleException("La cantidad sobrante no puede ser negativa");
        }

        paquete.setCantidad(request.cantidadAsignada());
        paquete.marcarAsignado();

        Asignacion asignacion = new Asignacion(
            paquete.getId(),
            request.necesidadId(),
            LocalDateTime.now(),
            EstadoAsignacionEnum.ASIGNADA,
            request.cantidadAsignada(),
            OrigenAsignacion.MATCHMAKING
        );

        Asignacion existente = asignacionRepo.findByPaqueteId(paquete.getId()).orElse(null);
        if (existente != null) {
            return mapper.toAsignacionDTO(existente);
        }

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

    @Transactional(readOnly = true)
    public int cantidadAsignadaPorNecesidad(String necesidadId) {
        validarId(necesidadId, "ID de necesidad inválido");

        return asignacionRepo.findAll().stream()
            .filter(asignacion -> necesidadId.equals(asignacion.getNecesidadId()))
            .mapToInt(asignacion -> asignacion.getCantidadAsignada() == null
                ? 0
                : asignacion.getCantidadAsignada())
            .sum();
    }

    @Transactional(readOnly = true)
    public StockDTO consultarStock(String productoId) {
        validarId(productoId, "Producto inválido");

        int cantidad = depositoRepo.findAll().stream()
            .mapToInt(deposito -> deposito.stockDisponible(productoId))
            .sum();

        return new StockDTO(productoId, cantidad);
    }

    @Transactional
    public List<AsignacionDTO> asignarDesdeStock(
        String necesidadId,
        String productoId,
        Integer cantidadSolicitada
    ) {
        validarId(necesidadId, "ID de necesidad inválido");
        validarId(productoId, "ID de producto inválido");

        if (cantidadSolicitada == null || cantidadSolicitada <= 0) {
            throw new BusinessRuleException("La cantidad solicitada debe ser positiva");
        }

        List<Deposito> depositos = depositoRepo.findAll();
        int stockDisponible = depositos.stream()
            .mapToInt(deposito -> deposito.stockDisponible(productoId))
            .sum();

        if (stockDisponible <= 0) {
            return List.of();
        }

        int cantidadRestante = Math.min(cantidadSolicitada, stockDisponible);
        List<AsignacionDTO> asignaciones = new ArrayList<>();

        for (Deposito deposito : depositos) {
            if (cantidadRestante <= 0) {
                break;
            }

            List<Paquete> paquetes = deposito.paquetesEnStockDe(productoId);

            for (Paquete paquete : paquetes) {
                if (cantidadRestante <= 0) {
                    break;
                }

                int cantidadOriginal = paquete.getCantidad();
                int cantidadAsignar = Math.min(cantidadOriginal, cantidadRestante);

                paquete.setCantidad(cantidadAsignar);
                paquete.marcarAsignado();

                Asignacion asignacion = new Asignacion(
                    paquete.getId(),
                    necesidadId,
                    LocalDateTime.now(),
                    EstadoAsignacionEnum.ASIGNADA,
                    cantidadAsignar,
                    OrigenAsignacion.SOLICITUD_ENTIDAD
                );

                Asignacion guardada = asignacionRepo.save(asignacion);

                int sobrante = cantidadOriginal - cantidadAsignar;
                if (sobrante > 0) {
                    deposito.almacenar(
                        new Paquete(
                            paquete.getDonacionId(),
                            paquete.getProducto(),
                            sobrante,
                            EstadoPaquete.EN_STOCK
                        )
                    );
                }

                depositoRepo.save(deposito);
                asignaciones.add(mapper.toAsignacionDTO(guardada));
                cantidadRestante -= cantidadAsignar;
            }
        }

        return asignaciones;
    }

    private AsignacionDTO registrarResultadoInternamente(
        Deposito deposito,
        Paquete paquete,
        ResultadoMatchmaking resultado
    ) {
        if (!resultado.tieneAsignacion()) {
            paquete.marcarEnStock();
            depositoRepo.save(deposito);
            return null;
        }

        paquete.setCantidad(resultado.cantidadAsignada());
        paquete.marcarAsignado();

        var necesidad = resultado.necesidad().orElseThrow();

        Asignacion existente = asignacionRepo.findByPaqueteId(paquete.getId()).orElse(null);
        if (existente != null) {
            return mapper.toAsignacionDTO(existente);
        }

        Asignacion asignacion = new Asignacion(
            paquete.getId(),
            necesidad.id(),
            LocalDateTime.now(),
            EstadoAsignacionEnum.ASIGNADA,
            resultado.cantidadAsignada(),
            OrigenAsignacion.MATCHMAKING
        );

        Asignacion guardada = asignacionRepo.save(asignacion);

        if (resultado.cantidadSobrante() > 0) {
            deposito.almacenar(
                new Paquete(
                    paquete.getDonacionId(),
                    paquete.getProducto(),
                    resultado.cantidadSobrante(),
                    EstadoPaquete.EN_STOCK
                )
            );
        }

        depositoRepo.save(deposito);
        return mapper.toAsignacionDTO(guardada);
    }

    private Deposito encontrarDepositoPorPaquete(String paqueteId) {
        return depositoRepo.findAll().stream()
            .filter(deposito -> deposito.getStockActual().stream()
                .anyMatch(paquete -> paqueteId.equals(paquete.getId())))
            .findFirst()
            .orElseThrow(() ->
                new ResourceNotFoundException("Depósito del paquete no encontrado"));
    }

    private Paquete buscarPaquete(Deposito deposito, String paqueteId) {
        return deposito.getStockActual().stream()
            .filter(paquete -> paqueteId.equals(paquete.getId()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado"));
    }

    private void validarResultado(ResultadoMatchmakingRequest request) {
        if (request == null) {
            throw new BusinessRuleException("Resultado inválido");
        }

        validarId(request.depositoId(), "ID de depósito inválido");
        validarId(request.paqueteId(), "ID de paquete inválido");

        if (request.cantidadAsignada() == null || request.cantidadAsignada() < 0
            || request.cantidadSobrante() == null || request.cantidadSobrante() < 0) {
            throw new BusinessRuleException("Cantidades del resultado inválidas");
        }
    }

    private void asegurarIntegraciones() {
        if (fachadaDonadores == null || fachadaDonaciones == null) {
            throw new ConflictException("Las fachadas externas no están configuradas");
        }
    }

    private void validarId(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(message);
        }
    }
}
