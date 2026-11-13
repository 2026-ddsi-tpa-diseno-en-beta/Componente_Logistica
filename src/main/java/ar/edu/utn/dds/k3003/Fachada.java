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
import ar.edu.utn.dds.k3003.messaging.MatchmakingOutbox;
import ar.edu.utn.dds.k3003.messaging.ReadModelEventPublisher;
import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;
import ar.edu.utn.dds.k3003.model.Asignacion;
import ar.edu.utn.dds.k3003.model.Deposito;
import ar.edu.utn.dds.k3003.model.EstadoPaquete;
import ar.edu.utn.dds.k3003.model.OrigenAsignacion;
import ar.edu.utn.dds.k3003.model.Paquete;
import ar.edu.utn.dds.k3003.model.algoritmos.ResultadoMatchmaking;
import ar.edu.utn.dds.k3003.repositories.AsignacionRepository;
import ar.edu.utn.dds.k3003.repositories.DepositoRepository;
import ar.edu.utn.dds.k3003.repositories.LogisticaDataMapper;
import ar.edu.utn.dds.k3003.repositories.inmemory.InMemoryAsignacionRepository;
import ar.edu.utn.dds.k3003.repositories.inmemory.InMemoryDepositoRepository;
import ar.edu.utn.dds.k3003.services.MatchmakingService;
import ar.edu.utn.dds.k3003.services.MatchmakingRegistrationResult;
import ar.edu.utn.dds.k3003.observability.TraceContext;
import ar.edu.utn.dds.k3003.observability.DomainEvents;
import ar.edu.utn.dds.k3003.integration.ConsultaPeriodoNecesidad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Fachada implements FachadaLogistica {

    private static final Logger log = LoggerFactory.getLogger(Fachada.class);

    private final DepositoRepository depositoRepo;
    private final AsignacionRepository asignacionRepo;
    private final LogisticaDataMapper mapper;
    private final MatchmakingService matchmakingService;

    @Autowired(required = false)
    private ReadModelEventPublisher readModelEventPublisher;

    @Autowired(required = false)
    private MatchmakingOutbox matchmakingOutbox;

    @Autowired(required = false)
    private ar.edu.utn.dds.k3003.persistence.OperacionCoordinada coordinacion;

    @Autowired(required = false)
    private ar.edu.utn.dds.k3003.persistence.RegistroOperaciones registros;

    private FachadaDonadoresYEntidades fachadaDonadores;
    private FachadaDonaciones fachadaDonaciones;

    public Fachada() {
        this(
            new InMemoryDepositoRepository(),
            new InMemoryAsignacionRepository(),
            new LogisticaDataMapper(),
            new MatchmakingService()
        );
    }

    @Autowired
    public Fachada(
        DepositoRepository depositoRepo,
        AsignacionRepository asignacionRepo,
        LogisticaDataMapper mapper,
        MatchmakingService matchmakingService
    ) {
        this.depositoRepo = Objects.requireNonNull(depositoRepo);
        this.asignacionRepo = Objects.requireNonNull(asignacionRepo);
        this.mapper = Objects.requireNonNull(mapper);
        this.matchmakingService = Objects.requireNonNull(matchmakingService);
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

        if (depositoDto.algoritmo() != null
            || depositoDto.stockActual() != null && !depositoDto.stockActual().isEmpty()) {
            throw new BusinessRuleException(
                "El depósito debe crearse sin stock y sin algoritmo; registre donaciones y configure el algoritmo después"
            );
        }

        Deposito deposito = mapper.toDeposito(depositoDto);
        Deposito guardado = depositoRepo.save(deposito);
        publicarEvento("deposito", guardado.getId(), "UPSERT", snapshotDeposito(guardado));
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

        Deposito deposito = depositoRepo.findByIdForUpdate(depositoID)
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));
        if (!deposito.getStockActual().isEmpty()) throw new ConflictException("El depósito tiene paquetes; no se puede borrar su trazabilidad");
        depositoRepo.deleteById(depositoID);
        publicarEvento("deposito", depositoID, "DELETE", Map.of());
    }

    @Override
    @Transactional
    public void setAlgoritmoMM(String depositoID, TipoAlgoritmoEnum tipoAlgoritmo) {
        validarId(depositoID, "ID de depósito inválido");

        if (tipoAlgoritmo == null) {
            throw new BusinessRuleException("El algoritmo es obligatorio");
        }

        Deposito deposito = depositoRepo.findByIdForUpdate(depositoID)
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

        deposito.setTipoAlgoritmo(tipoAlgoritmo);
        Deposito guardado = depositoRepo.save(deposito);
        publicarEvento("deposito", guardado.getId(), "UPSERT", snapshotDeposito(guardado));
    }

    @Override
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

        return enOperacion("recepcion", donacionID,
            () -> recibirDonacion(depositoID, donacionID, productoID, cantidad));
    }

    private DepositoDTO recibirDonacion(String depositoID, String donacionID, String productoID, int cantidad) {
        if (registros != null) {
            var receipt = registros.recepcion(donacionID);
            if (receipt != null) {
                validarRecepcion(receipt, depositoID, productoID, cantidad);
                return buscarDepositoPorID(depositoID);
            }
        }
        // Reconcile pre-receipt deployments without mistaking legitimate splits for duplicate ingress.
        var anteriores = depositoRepo.findAll().stream().filter(d -> d.getStockActual().stream()
            .anyMatch(p -> donacionID.equals(p.getDonacionId()))).toList();
        if (!anteriores.isEmpty()) {
            var receipt = new ar.edu.utn.dds.k3003.persistence.entity.RecepcionDonacion();
            receipt.donacionId = donacionID;
            receipt.depositoId = anteriores.getFirst().getId();
            var paquetes = anteriores.stream().flatMap(d -> d.getStockActual().stream())
                .filter(p -> donacionID.equals(p.getDonacionId())).toList();
            receipt.productoId = paquetes.getFirst().getProducto();
            receipt.cantidad = paquetes.stream().mapToInt(Paquete::getCantidad).sum();
            if (anteriores.size() != 1 || paquetes.stream().anyMatch(p -> !receipt.productoId.equals(p.getProducto())))
                throw new ConflictException("La donación tiene una recepción histórica inconsistente");
            validarRecepcion(receipt, depositoID, productoID, cantidad);
            if (registros != null) registros.guardar(receipt);
            return buscarDepositoPorID(depositoID);
        }

        Deposito deposito = depositoRepo.findByIdForUpdate(depositoID)
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

        if (registros != null) {
            var receipt = new ar.edu.utn.dds.k3003.persistence.entity.RecepcionDonacion();
            receipt.donacionId = donacionID; receipt.depositoId = depositoID;
            receipt.productoId = productoID; receipt.cantidad = cantidad;
            registros.guardar(receipt);
        }

        if (paquete.getId() == null) {
            throw new IllegalStateException("No se pudo identificar el paquete persistido");
        }

        publicarEvento("paquete", paquete.getId(), "UPSERT", snapshotPaquete(guardado.getId(), paquete));
        publicarEvento("deposito", guardado.getId(), "UPSERT", snapshotDeposito(guardado));

        if (matchmakingOutbox != null) {
            matchmakingOutbox.guardar(
                new DonacionPendienteMessage(
                    guardado.getId(),
                    paquete.getId(),
                    donacionID,
                    productoID,
                    cantidad,
                    guardado.getTipoAlgoritmo(),
                    TraceContext.currentTraceId()
                )
            );
        }

        DomainEvents.info(log,
            "donacion.registrada deposito={} paquete={} donacion={} producto={} cantidad={} algoritmo={}",
            guardado.getId(),
            paquete.getId(),
            donacionID,
            productoID,
            cantidad,
            guardado.getTipoAlgoritmo()
        );

        return mapper.toDepositoDTO(guardado);
    }

    @Override
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

        String need = resultado.necesidad().map(n -> n.id()).orElse(null);
        return enOperacion(need == null ? "paquete" : "necesidad", need == null ? paqueteDTO.id() : need, () -> {
            var locked = depositoRepo.findByIdForUpdate(depositoID).orElseThrow();
            var paquete = buscarPaquete(locked, paqueteDTO.id());
            var existente = asignacionRepo.findByPaqueteId(paquete.getId());
            if (existente.isPresent()) return mapper.toAsignacionDTO(existente.get());
            if (paquete.getEstadoPaquete() != EstadoPaquete.PENDIENTE) return null;
            var validado = resultado;
            if (need != null) {
                var vigente = fachadaDonadores instanceof ar.edu.utn.dds.k3003.integration.ConsultaNecesidadVigente
                    ? consultarNecesidad(need, paquete.getProducto())
                    : new ar.edu.utn.dds.k3003.integration.ConsultaNecesidadVigente.NecesidadVigente(
                        necesidadesMateriales.stream().filter(n -> need.equals(n.id())).findFirst().orElseThrow(),
                        0, inicioPeriodo(need));
                validado = matchmakingService.procesar(locked.getTipoAlgoritmo(), List.of(vigente.necesidad()),
                    paquete.getProducto(), paquete.getCantidad(), id -> comprometida(vigente));
            }
            return registrarResultadoInternamente(locked, paquete, validado);
        });
    }

    @Override
    public void reportarEntrega(PaqueteDTO paqueteDTO) {
        if (paqueteDTO == null || paqueteDTO.id() == null) {
            throw new BusinessRuleException("Paquete inválido");
        }

        asegurarIntegraciones();

        var referencia = asignacionRepo.findByPaqueteId(paqueteDTO.id())
            .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));
        enOperacion("necesidad", referencia.getNecesidadId(), () -> {
            entregarPaquete(paqueteDTO);
            return null;
        });
    }

    private void entregarPaquete(PaqueteDTO paqueteDTO) {

        Deposito deposito = encontrarDepositoPorPaquete(paqueteDTO.id());
        deposito = depositoRepo.findByIdForUpdate(deposito.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

        Asignacion asignacion = asignacionRepo.findByPaqueteId(paqueteDTO.id())
            .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        if (asignacion.getEstado() != EstadoAsignacionEnum.ASIGNADA) {
            throw new ConflictException("La asignación no está en estado ASIGNADA");
        }

        Paquete paquete = buscarPaquete(deposito, paqueteDTO.id());

        asignacion.cambiarEstado(EstadoAsignacionEnum.COMPLETADA);
        asignacionRepo.save(asignacion);

        satisfacerEntrega(
            asignacion.getNecesidadId(),
            asignacion.getCantidadAsignada(),
            List.of(paqueteDTO.id())
        );

        paquete.marcarEntregado();
        depositoRepo.save(deposito);

        aceptarDonacion(paquete.getDonacionId());
        publicarEvento("paquete", paquete.getId(), "UPSERT", snapshotPaquete(deposito.getId(), paquete));
        publicarEvento("asignacion", asignacion.getId(), "UPSERT", snapshotAsignacion(deposito, asignacion));
        publicarEvento("deposito", deposito.getId(), "UPSERT", snapshotDeposito(deposito));

        registrarDuracionEntrega(asignacion);

        DomainEvents.info(log,
            "entrega.reportada paquete={} deposito={} necesidad={} cantidad={} donacion={}",
            paquete.getId(),
            deposito.getId(),
            asignacion.getNecesidadId(),
            asignacion.getCantidadAsignada(),
            paquete.getDonacionId()
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

    public AsignacionDTO registrarResultadoMatchmaking(
        ResultadoMatchmakingRequest request
    ) {
        return registrarResultadoMatchmakingDetallado(request).asignacion();
    }

    public MatchmakingRegistrationResult registrarResultadoMatchmakingDetallado(
        ResultadoMatchmakingRequest request
    ) {
        validarResultado(request);

        var referencia = depositoRepo.findById(request.depositoId())
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));
        var paqueteOriginal = buscarPaquete(referencia, request.paqueteId());
        if (paqueteOriginal.getEstadoPaquete() == EstadoPaquete.PENDIENTE
            && (long) request.cantidadAsignada() + request.cantidadSobrante() != paqueteOriginal.getCantidad())
            throw new BusinessRuleException("El resultado no coincide con la cantidad original");

        ResultadoMatchmakingRequest propuesta = request.tieneAsignacion() ? request : recalcularPropuesta(request);
        // Release the former need lock before trying another candidate: never nest need locks.
        for (int intento = 0; intento < 4; intento++) {
            var actual = propuesta;
            var resultado = enOperacion(actual.tieneAsignacion() ? "necesidad" : "paquete",
                actual.tieneAsignacion() ? actual.necesidadId() : actual.paqueteId(),
                () -> registrarResultadoBajoLock(actual));
            if (resultado != null) return resultado;
            propuesta = recalcularPropuesta(request);
        }
        // Contentions do not turn a valid donation into a poison message; stock remains reusable.
        var sinAsignacion = new ResultadoMatchmakingRequest(request.depositoId(), request.paqueteId(), null,
            0, cantidadPaquete(request));
        return enOperacion("paquete", request.paqueteId(), () -> registrarResultadoBajoLock(sinAsignacion));
    }

    private MatchmakingRegistrationResult registrarResultadoBajoLock(ResultadoMatchmakingRequest request) {
        ar.edu.utn.dds.k3003.integration.ConsultaNecesidadVigente.NecesidadVigente vigente = null;
        if (request.tieneAsignacion()) {
            try { vigente = consultarNecesidad(request.necesidadId(), null); }
            catch (ResourceNotFoundException obsoleta) { /* The worker's decision can legitimately be obsolete. */ }
        }

        Deposito deposito = depositoRepo.findByIdForUpdate(request.depositoId())
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

        Paquete paquete = buscarPaquete(deposito, request.paqueteId());

        if (paquete.getEstadoPaquete() == EstadoPaquete.EN_STOCK) {
            DomainEvents.info(log,
                "matchmaking.resultado_duplicado paquete={} estado=EN_STOCK",
                paquete.getId()
            );
            return new MatchmakingRegistrationResult(null, false, 0);
        }

        if (paquete.getEstadoPaquete() == EstadoPaquete.ASIGNADO || paquete.getEstadoPaquete() == EstadoPaquete.ENTREGADO) {
            Asignacion existente = asignacionRepo.findByPaqueteId(paquete.getId())
                .orElseThrow(() -> new IllegalStateException("Paquete asignado sin asignación"));

            DomainEvents.info(log,
                "matchmaking.resultado_duplicado paquete={} asignacion={}",
                paquete.getId(),
                existente.getId()
            );
            return new MatchmakingRegistrationResult(
                mapper.toAsignacionDTO(existente),
                false,
                0
            );
        }

        if (paquete.getEstadoPaquete() != EstadoPaquete.PENDIENTE) {
            throw new ConflictException(
                "El paquete no puede procesar un resultado de matchmaking"
            );
        }

        int cantidadOriginal = paquete.getCantidad();

        if ((long) request.cantidadAsignada() + request.cantidadSobrante() != cantidadOriginal) {
            throw new BusinessRuleException(
                "El resultado no coincide con la cantidad original"
            );
        }

        if (!request.tieneAsignacion()) {
            paquete.marcarEnStock();
            deposito = depositoRepo.save(deposito);
            publicarEvento("paquete", paquete.getId(), "UPSERT", snapshotPaquete(deposito.getId(), paquete));
            publicarEvento("deposito", deposito.getId(), "UPSERT", snapshotDeposito(deposito));

            DomainEvents.info(log,
                "matchmaking.sin_asignacion paquete={} cantidad={} -> EN_STOCK",
                paquete.getId(),
                cantidadOriginal
            );
            return new MatchmakingRegistrationResult(null, true, cantidadOriginal);
        }

        if (request.cantidadAsignada() <= 0 || request.necesidadId() == null) {
            throw new BusinessRuleException("Resultado de asignación inválido");
        }

        if (vigente == null || !paquete.getProducto().equals(vigente.necesidad().productoSolicitadoID()))
            return null;
        int saldo = saldoNecesidad(vigente);
        int asignable = Math.min(request.cantidadAsignada(), saldo);
        if (vigente.necesidad().tipo() == ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.TipoNecesidadMaterialEnum.RECURRENTE)
            asignable = cantidadOriginal >= saldo ? saldo : 0;
        if (asignable <= 0) return null;
        request = new ResultadoMatchmakingRequest(request.depositoId(), request.paqueteId(), request.necesidadId(),
            asignable, cantidadOriginal - asignable);

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

        deposito = depositoRepo.save(deposito);
        publicarEvento("paquete", paquete.getId(), "UPSERT", snapshotPaquete(deposito.getId(), paquete));
        publicarEvento("asignacion", guardada.getId(), "UPSERT", snapshotAsignacion(deposito, guardada));
        publicarEvento("deposito", deposito.getId(), "UPSERT", snapshotDeposito(deposito));
        publicarPaquetes(deposito);

        DomainEvents.info(log,
            "matchmaking.asignado paquete={} asignacion={} necesidad={} cantidad={} sobrante={}",
            paquete.getId(),
            guardada.getId(),
            request.necesidadId(),
            request.cantidadAsignada(),
            request.cantidadSobrante()
        );

        return new MatchmakingRegistrationResult(
            mapper.toAsignacionDTO(guardada),
            true,
            request.cantidadSobrante()
        );
    }

    @Transactional(readOnly = true)
    public int cantidadAsignadaPorNecesidad(String necesidadId) {
        validarId(necesidadId, "ID de necesidad inválido");
        java.time.LocalDate inicio = inicioPeriodo(necesidadId);

        return asignacionRepo.findAll().stream()
            .filter(asignacion -> necesidadId.equals(asignacion.getNecesidadId()))
            .filter(asignacion -> inicio == null
                || asignacion.getEstado() == EstadoAsignacionEnum.ASIGNADA
                || asignacion.getHistorial().stream().anyMatch(cambio ->
                    cambio.getEstado() == EstadoAsignacionEnum.COMPLETADA
                        && !cambio.getFecha().toLocalDate().isBefore(inicio)))
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

    public List<AsignacionDTO> asignarDesdeStock(
        String necesidadId,
        String productoId,
        Integer cantidadSolicitada
    ) {
        return asignarDesdeStock(necesidadId, productoId, cantidadSolicitada, null);
    }

    public List<AsignacionDTO> asignarDesdeStock(String necesidadId, String productoId,
        Integer cantidadSolicitada, String solicitudId) {
        validarId(necesidadId, "ID de necesidad inválido");
        validarId(productoId, "ID de producto inválido");

        if (cantidadSolicitada == null || cantidadSolicitada <= 0) {
            throw new BusinessRuleException("La cantidad solicitada debe ser positiva");
        }
        if (solicitudId != null && (solicitudId.isBlank() || solicitudId.length() > 200))
            throw new BusinessRuleException("Idempotency-Key inválida");
        var claves = new ArrayList<String>();
        claves.add(claveOperacion("necesidad", necesidadId));
        if (solicitudId != null) claves.add(claveOperacion("solicitud-stock", solicitudId));
        java.util.function.Supplier<List<AsignacionDTO>> operation = () -> {
            if (solicitudId != null && registros != null) {
                var recibo = registros.solicitud(solicitudId);
                if (recibo != null) {
                    if (!necesidadId.equals(recibo.necesidadId) || !productoId.equals(recibo.productoId)
                        || !cantidadSolicitada.equals(recibo.cantidad))
                        throw new ConflictException("La clave de stock ya fue utilizada con otros datos");
                    return recibo.asignaciones.stream().map(id -> mapper.toAsignacionDTO(asignacionRepo.findById(id)
                        .orElseThrow(() -> new ConflictException("La asignación del recibo ya no existe")))).toList();
                }
            }
            var asignaciones = reservarDesdeStock(necesidadId, productoId, cantidadSolicitada);
            if (solicitudId != null && registros != null) {
                var recibo = new ar.edu.utn.dds.k3003.persistence.entity.SolicitudStockProcesada();
                recibo.id = solicitudId; recibo.necesidadId = necesidadId; recibo.productoId = productoId;
                recibo.cantidad = cantidadSolicitada;
                recibo.asignaciones.addAll(asignaciones.stream().map(AsignacionDTO::id).toList());
                registros.guardar(recibo);
            }
            return asignaciones;
        };
        return coordinacion == null ? operation.get() : coordinacion.ejecutar(claves, operation);
    }

    private List<AsignacionDTO> reservarDesdeStock(String necesidadId, String productoId, int cantidadSolicitada) {
        var vigente = consultarNecesidad(necesidadId, productoId);
        if (!productoId.equals(vigente.necesidad().productoSolicitadoID()))
            throw new BusinessRuleException("El producto no coincide con la necesidad");
        int saldo = saldoNecesidad(vigente);
        if (saldo <= 0) return List.of();
        boolean recurrente = vigente.necesidad().tipo()
            == ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.TipoNecesidadMaterialEnum.RECURRENTE;
        if (recurrente && cantidadSolicitada < saldo) return List.of();

        List<Deposito> referencias = depositoRepo.findAll().stream()
            .sorted(Comparator.comparing(Deposito::getId))
            .toList();

        List<Deposito> depositos = new ArrayList<>();
        for (Deposito referencia : referencias) {
            depositos.add(
                depositoRepo.findByIdForUpdate(referencia.getId())
                    .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "Depósito no encontrado durante la asignación de stock"
                        ))
            );
        }

        int stockDisponible = depositos.stream()
            .mapToInt(deposito -> deposito.stockDisponible(productoId))
            .sum();

        if (stockDisponible <= 0) {
            return List.of();
        }

        if (recurrente && stockDisponible < saldo) return List.of();

        int cantidadRestante = Math.min(saldo, Math.min(cantidadSolicitada, stockDisponible));
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
                publicarEvento("paquete", paquete.getId(), "UPSERT", snapshotPaquete(deposito.getId(), paquete));
                publicarEvento("asignacion", guardada.getId(), "UPSERT", snapshotAsignacion(deposito, guardada));
                asignaciones.add(mapper.toAsignacionDTO(guardada));
                cantidadRestante -= cantidadAsignar;

                DomainEvents.info(log,
                    "stock.asignado deposito={} paquete={} asignacion={} necesidad={} cantidad={} sobrante={}",
                    deposito.getId(),
                    paquete.getId(),
                    guardada.getId(),
                    necesidadId,
                    cantidadAsignar,
                    sobrante
                );
            }
        }

        depositos.forEach(deposito -> {
            publicarEvento("deposito", deposito.getId(), "UPSERT", snapshotDeposito(deposito));
            publicarPaquetes(deposito);
        });
        return asignaciones;
    }

    private AsignacionDTO registrarResultadoInternamente(
        Deposito deposito,
        Paquete paquete,
        ResultadoMatchmaking resultado
    ) {
        if (!resultado.tieneAsignacion()) {
            paquete.marcarEnStock();
            deposito = depositoRepo.save(deposito);
            publicarEvento("paquete", paquete.getId(), "UPSERT", snapshotPaquete(deposito.getId(), paquete));
            publicarEvento("deposito", deposito.getId(), "UPSERT", snapshotDeposito(deposito));
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

        deposito = depositoRepo.save(deposito);
        publicarEvento("paquete", paquete.getId(), "UPSERT", snapshotPaquete(deposito.getId(), paquete));
        publicarEvento("asignacion", guardada.getId(), "UPSERT", snapshotAsignacion(deposito, guardada));
        publicarEvento("deposito", deposito.getId(), "UPSERT", snapshotDeposito(deposito));
        publicarPaquetes(deposito);
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

    private <T> T enOperacion(String tipo, String id, java.util.function.Supplier<T> operation) {
        return coordinacion == null ? operation.get() : coordinacion.ejecutar(claveOperacion(tipo, id), operation);
    }

    private String claveOperacion(String tipo, String id) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return tipo + ":" + java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private void validarRecepcion(ar.edu.utn.dds.k3003.persistence.entity.RecepcionDonacion receipt,
        String depositoId, String productoId, int cantidad) {
        if (!depositoId.equals(receipt.depositoId) || !productoId.equals(receipt.productoId)
            || cantidad != receipt.cantidad)
            throw new ConflictException("La donación ya fue recibida con otros datos");
    }

    private ar.edu.utn.dds.k3003.integration.ConsultaNecesidadVigente.NecesidadVigente consultarNecesidad(
        String id, String productoId) {
        if (fachadaDonadores == null) throw new ConflictException("La fachada de necesidades no está configurada");
        if (fachadaDonadores instanceof ar.edu.utn.dds.k3003.integration.ConsultaNecesidadVigente client)
            return client.consultarNecesidad(id);
        var necesidades = fachadaDonadores.obtenerNecesidadesInsatisfechasDe(productoId);
        var need = necesidades == null ? null : necesidades.stream()
            .filter(n -> n != null && id.equals(n.id())).findFirst().orElse(null);
        if (need == null) throw new ResourceNotFoundException("Necesidad no encontrada");
        return new ar.edu.utn.dds.k3003.integration.ConsultaNecesidadVigente.NecesidadVigente(need, 0, inicioPeriodo(id));
    }

    private int comprometida(ar.edu.utn.dds.k3003.integration.ConsultaNecesidadVigente.NecesidadVigente vigente) {
        int pendientes = 0, entregadas = 0;
        for (Asignacion a : asignacionRepo.findAll()) {
            if (!vigente.necesidad().id().equals(a.getNecesidadId())) continue;
            int cantidad = a.getCantidadAsignada() == null ? 0 : a.getCantidadAsignada();
            if (a.getEstado() == EstadoAsignacionEnum.ASIGNADA) pendientes += cantidad;
            else if (a.getEstado() == EstadoAsignacionEnum.COMPLETADA && (vigente.inicio() == null
                || a.getHistorial().stream().anyMatch(c -> c.getEstado() == EstadoAsignacionEnum.COMPLETADA
                    && !c.getFecha().toLocalDate().isBefore(vigente.inicio())))) entregadas += cantidad;
        }
        // DyE's received units and our completed assignments describe the same physical delivery.
        return pendientes + Math.max(Math.max(0, vigente.recibida()), entregadas);
    }

    private int saldoNecesidad(ar.edu.utn.dds.k3003.integration.ConsultaNecesidadVigente.NecesidadVigente vigente) {
        return Math.max(0, vigente.necesidad().cantidadObjetivo() - comprometida(vigente));
    }

    private int cantidadPaquete(ResultadoMatchmakingRequest request) {
        var depot = depositoRepo.findById(request.depositoId())
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));
        return buscarPaquete(depot, request.paqueteId()).getCantidad();
    }

    private ResultadoMatchmakingRequest recalcularPropuesta(ResultadoMatchmakingRequest request) {
        var depot = depositoRepo.findById(request.depositoId())
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));
        var packet = buscarPaquete(depot, request.paqueteId());
        if (packet.getEstadoPaquete() != EstadoPaquete.PENDIENTE)
            return new ResultadoMatchmakingRequest(request.depositoId(), request.paqueteId(), null, 0, packet.getCantidad());
        if (fachadaDonadores == null) throw new ConflictException("La fachada de necesidades no está configurada");
        var candidatas = fachadaDonadores.obtenerNecesidadesInsatisfechasDe(packet.getProducto());
        var actuales = new ArrayList<NecesidadMaterialDTO>();
        var comprometidas = new java.util.HashMap<String, Integer>();
        for (var n : candidatas == null ? List.<NecesidadMaterialDTO>of() : candidatas) {
            if (n == null || n.id() == null) continue;
            try {
                var vigente = consultarNecesidad(n.id(), packet.getProducto());
                actuales.add(vigente.necesidad());
                comprometidas.put(n.id(), comprometida(vigente));
            } catch (ResourceNotFoundException removed) { /* Changed since the list was read. */ }
        }
        var result = matchmakingService.procesar(depot.getTipoAlgoritmo(), actuales, packet.getProducto(),
            packet.getCantidad(), comprometidas);
        return new ResultadoMatchmakingRequest(request.depositoId(), request.paqueteId(),
            result.necesidad().map(n -> n.id()).orElse(null), result.cantidadAsignada(), result.cantidadSobrante());
    }

    @Transactional
    public DepositoDTO modificarDeposito(String id, DepositoDTO dto) {
        if (dto == null || dto.nombre() == null || dto.nombre().isBlank()
            || dto.direccion() == null || dto.direccion().isBlank()
            || dto.capacidadMaxima() == null || dto.capacidadMaxima() <= 0)
            throw new BusinessRuleException("Datos de depósito inválidos");
        Deposito deposito = depositoRepo.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));
        if (dto.capacidadMaxima() < deposito.ocupacionActual()) throw new ConflictException("La capacidad no puede ser menor que la ocupación actual");
        deposito.setNombre(dto.nombre()); deposito.setDireccion(dto.direccion()); deposito.setCapacidadMaxima(dto.capacidadMaxima());
        Deposito guardado = depositoRepo.save(deposito);
        publicarEvento("deposito", guardado.getId(), "UPSERT", snapshotDeposito(guardado));
        return mapper.toDepositoDTO(guardado);
    }
    @Transactional(readOnly = true)
    public List<AsignacionDTO> listarAsignaciones() {
        return asignacionRepo.findAll().stream().map(mapper::toAsignacionDTO).toList();
    }

    @Transactional(readOnly = true)
    public double paquetesPendientes() {
        return depositoRepo.findAll().stream().flatMap(d -> d.getStockActual().stream())
            .filter(p -> p.getEstadoPaquete() == EstadoPaquete.PENDIENTE).count();
    }
    @Transactional(readOnly = true)
    public double ocupacionTotal() {
        return depositoRepo.findAll().stream().mapToInt(Deposito::ocupacionActual).sum();
    }
    @Transactional(readOnly = true)
    public double capacidadTotal() {
        return depositoRepo.findAll().stream().mapToInt(Deposito::getCapacidadMaxima).sum();
    }

    public record DepositoSnapshot(String id, double ocupacion, double capacidad) {}
    public record OperacionSnapshot(List<DepositoSnapshot> depositos, double reservadas, LocalDateTime oldest) {}

    @Transactional(readOnly = true)
    public OperacionSnapshot observabilitySnapshot() {
        List<DepositoSnapshot> deposits = depositoRepo.findAll().stream().map(d ->
            new DepositoSnapshot(d.getId(), d.ocupacionActual(), d.getCapacidadMaxima())).toList();
        var pending = asignacionRepo.findAll().stream()
            .filter(a -> a.getEstado() == EstadoAsignacionEnum.ASIGNADA).toList();
        double reserved = pending.stream().mapToInt(Asignacion::getCantidadAsignada).sum();
        LocalDateTime oldest = pending.stream().map(Asignacion::getFecha).filter(Objects::nonNull)
            .min(LocalDateTime::compareTo).orElse(null);
        return new OperacionSnapshot(deposits, reserved, oldest);
    }

    private java.time.LocalDate inicioPeriodo(String necesidadId) {
        return fachadaDonadores instanceof ConsultaPeriodoNecesidad consulta
            ? consulta.inicioPeriodo(necesidadId) : null;
    }

    public void reportarEntregaLote(List<String> paqueteIds) {
        if (paqueteIds == null || paqueteIds.isEmpty() || paqueteIds.stream().anyMatch(id -> id == null || id.isBlank())
            || paqueteIds.stream().distinct().count() != paqueteIds.size()) throw new BusinessRuleException("Paquetes del lote inválidos o repetidos");
        asegurarIntegraciones();
        List<Asignacion> asignaciones = paqueteIds.stream().map(id -> asignacionRepo.findByPaqueteId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"))).toList();
        String necesidad = asignaciones.getFirst().getNecesidadId();
        if (asignaciones.stream().anyMatch(a -> !necesidad.equals(a.getNecesidadId()) || a.getEstado() != EstadoAsignacionEnum.ASIGNADA))
            throw new ConflictException("El lote debe contener asignaciones pendientes de una misma necesidad");
        enOperacion("necesidad", necesidad, () -> {
            entregarLote(paqueteIds, necesidad);
            return null;
        });
    }

    private void entregarLote(List<String> paqueteIds, String necesidad) {
        java.util.Map<String, Deposito> depositos = new java.util.TreeMap<>();
        for (String id : paqueteIds) { Deposito ref = encontrarDepositoPorPaquete(id); depositos.put(ref.getId(), ref); }
        for (String id : new ArrayList<>(depositos.keySet())) depositos.put(id, depositoRepo.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado")));
        List<Asignacion> confirmadas = paqueteIds.stream().map(id -> asignacionRepo.findByPaqueteId(id).orElseThrow()).toList();
        if (confirmadas.stream().anyMatch(a -> a.getEstado() != EstadoAsignacionEnum.ASIGNADA))
            throw new ConflictException("Una asignación del lote ya fue entregada");
        int cantidad = confirmadas.stream().mapToInt(Asignacion::getCantidadAsignada).sum();
        satisfacerEntrega(necesidad, cantidad, paqueteIds);
        java.util.Set<String> donaciones = new java.util.HashSet<>();
        for (Asignacion asignacion : confirmadas) {
            Paquete paquete = depositos.values().stream().flatMap(d -> d.getStockActual().stream())
                .filter(p -> p.getId().equals(asignacion.getPaqueteId())).findFirst().orElseThrow();
            asignacion.cambiarEstado(EstadoAsignacionEnum.COMPLETADA); asignacionRepo.save(asignacion);
            paquete.marcarEntregado(); donaciones.add(paquete.getDonacionId());
        }
        depositos.values().forEach(depositoRepo::save);
        for (Asignacion asignacion : confirmadas) {
            Deposito depositoAsignacion = depositos.values().stream()
                .filter(d -> d.getStockActual().stream().anyMatch(p -> p.getId().equals(asignacion.getPaqueteId())))
                .findFirst().orElseThrow();
            Paquete paquete = buscarPaquete(depositoAsignacion, asignacion.getPaqueteId());
            publicarEvento("paquete", paquete.getId(), "UPSERT", snapshotPaquete(depositoAsignacion.getId(), paquete));
            publicarEvento("asignacion", asignacion.getId(), "UPSERT", snapshotAsignacion(depositoAsignacion, asignacion));
        }
        depositos.values().forEach(deposito -> publicarEvento(
            "deposito", deposito.getId(), "UPSERT", snapshotDeposito(deposito)));
        for (String id : donaciones) aceptarDonacion(id);
        confirmadas.forEach(this::registrarDuracionEntrega);
        DomainEvents.info(log, "entrega.lote_reportada necesidad={} paquetes={} cantidad={}", necesidad, paqueteIds.size(), cantidad);
    }

    private void aceptarDonacion(String id) {
        var donacion = fachadaDonaciones.buscarDonacionPorID(id);
        if (donacion == null || donacion.estado() != EstadoDonacionEnum.CONQUEJA)
            fachadaDonaciones.cambiarEstadoDeDonacion(id, EstadoDonacionEnum.ACEPTADA);
    }

    /** The identity survives local rollback and ignores the order of packages in a batch. */
    private void satisfacerEntrega(String necesidadId, int cantidad, List<String> paqueteIds) {
        if (fachadaDonadores instanceof ar.edu.utn.dds.k3003.integration.SatisfaccionIdempotente client) {
            String paquetes = String.join(",", paqueteIds.stream().sorted().toList());
            String entregaId = "entrega:" + necesidadId + ":"
                + java.util.UUID.nameUUIDFromBytes(paquetes.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            client.satisfacerEntrega(necesidadId, cantidad, entregaId);
        } else {
            fachadaDonadores.satisfacerNecesidad(necesidadId, cantidad);
        }
    }

    private void registrarDuracionEntrega(Asignacion asignacion) {
        if (asignacion.getFecha() == null) return;
        java.time.Duration elapsed = java.time.Duration.between(asignacion.getFecha(), LocalDateTime.now());
        if (elapsed.isNegative()) return;
        io.micrometer.core.instrument.Timer.builder("logistica.entregas.duracion")
            .description("Desde asignación hasta entrega física, por paquete")
            .tag("origen", asignacion.getOrigen() == null ? "DESCONOCIDO" : asignacion.getOrigen().name())
            .minimumExpectedValue(java.time.Duration.ofSeconds(1))
            .maximumExpectedValue(java.time.Duration.ofDays(7))
            .publishPercentileHistogram().register(io.micrometer.core.instrument.Metrics.globalRegistry).record(elapsed);
}

    /** Reencola el estado completo de Logística para reconstruir el modelo de lectura. */
    @Transactional
    public void republicarModeloLectura() {
        for (Deposito deposito : depositoRepo.findAll()) {
            publicarEvento("deposito", deposito.getId(), "UPSERT", snapshotDeposito(deposito));
            for (Paquete paquete : deposito.getStockActual()) {
                publicarEvento(
                    "paquete", paquete.getId(), "UPSERT", snapshotPaquete(deposito.getId(), paquete));
                asignacionRepo.findByPaqueteId(paquete.getId()).ifPresent(asignacion ->
                    publicarEvento(
                        "asignacion", asignacion.getId(), "UPSERT", snapshotAsignacion(deposito, asignacion)));
            }
        }
    }

    @Transactional
    public int recuperarMatchmakingPendiente() {
        if (matchmakingOutbox == null) {
            throw new ConflictException("La mensajería de matchmaking está deshabilitada");
        }
        int recuperados = 0;

        for (Deposito referencia : depositoRepo.findAll().stream()
            .sorted(Comparator.comparing(Deposito::getId)).toList()) {
            Deposito deposito = depositoRepo.findByIdForUpdate(referencia.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));
            if (deposito.getTipoAlgoritmo() == null) {
                boolean tienePendientes = deposito.getStockActual().stream()
                    .anyMatch(paquete -> paquete.getEstadoPaquete() == EstadoPaquete.PENDIENTE);
                if (tienePendientes) {
                    log.warn(
                        "matchmaking.recuperacion_omitida deposito={} motivo=algoritmo_no_configurado",
                        deposito.getId()
                    );
                }
                continue;
            }

            for (Paquete paquete : deposito.getStockActual()) {
                if (paquete.getEstadoPaquete() != EstadoPaquete.PENDIENTE) {
                    continue;
                }

                DonacionPendienteMessage mensaje = new DonacionPendienteMessage(
                    deposito.getId(),
                    paquete.getId(),
                    paquete.getDonacionId(),
                    paquete.getProducto(),
                    paquete.getCantidad(),
                    deposito.getTipoAlgoritmo(),
                    TraceContext.currentTraceId()
                );
                matchmakingOutbox.recuperar(mensaje);
                recuperados++;
            }
        }

        log.info("matchmaking.recuperacion_finalizada paquetes_reencolados={}", recuperados);
        return recuperados;
    }

    private void publicarEvento(
        String tipo, String id, String operacion, Map<String, Object> payload
    ) {
        if (readModelEventPublisher != null) {
            readModelEventPublisher.publicar(tipo, id, operacion, payload);
        }
    }

    private void publicarPaquetes(Deposito deposito) {
        deposito.getStockActual().forEach(paquete -> publicarEvento(
            "paquete", paquete.getId(), "UPSERT", snapshotPaquete(deposito.getId(), paquete)));
    }

    private Map<String, Object> snapshotDeposito(Deposito deposito) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", deposito.getId());
        snapshot.put("nombre", deposito.getNombre());
        snapshot.put("direccion", deposito.getDireccion());
        snapshot.put("capacidadMaxima", deposito.getCapacidadMaxima());
        snapshot.put("algoritmo", deposito.getTipoAlgoritmo() == null ? null : deposito.getTipoAlgoritmo().name());
        snapshot.put("ocupacionActual", deposito.ocupacionActual());
        snapshot.put("stockActual", deposito.getStockActual().stream()
            .map(paquete -> snapshotPaquete(deposito.getId(), paquete)).toList());
        return snapshot;
    }

    private Map<String, Object> snapshotPaquete(String depositoId, Paquete paquete) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", paquete.getId());
        snapshot.put("depositoID", depositoId);
        snapshot.put("donacionID", paquete.getDonacionId());
        snapshot.put("productoID", paquete.getProducto());
        snapshot.put("cantidad", paquete.getCantidad());
        snapshot.put("estado", paquete.getEstadoPaquete() == null ? null : paquete.getEstadoPaquete().name());
        return snapshot;
    }

    private Map<String, Object> snapshotAsignacion(Deposito deposito, Asignacion asignacion) {
        Paquete paquete = buscarPaquete(deposito, asignacion.getPaqueteId());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", asignacion.getId());
        snapshot.put("depositoID", deposito.getId());
        snapshot.put("paqueteID", asignacion.getPaqueteId());
        snapshot.put("necesidadID", asignacion.getNecesidadId());
        snapshot.put("donacionID", paquete.getDonacionId());
        snapshot.put("productoID", paquete.getProducto());
        snapshot.put("cantidadAsignada", asignacion.getCantidadAsignada());
        snapshot.put("fecha", asignacion.getFecha());
        snapshot.put("estado", asignacion.getEstado() == null ? null : asignacion.getEstado().name());
        snapshot.put("origen", asignacion.getOrigen() == null ? null : asignacion.getOrigen().name());
        snapshot.put("historial", asignacion.getHistorial().stream()
            .map(cambio -> Map.of("estado", cambio.getEstado().name(), "fecha", cambio.getFecha()))
            .toList());
        return snapshot;
    }

}
