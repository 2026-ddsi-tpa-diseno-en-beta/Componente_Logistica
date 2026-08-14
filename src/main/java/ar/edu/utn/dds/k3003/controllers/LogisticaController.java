package ar.edu.utn.dds.k3003.controllers;

import ar.edu.utn.dds.k3003.controllers.requests.logistica.GestionDonacionRequest;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.AlgoritmoDepositoRequest;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.SolicitudAsignacionStockRequest;

import ar.edu.utn.dds.k3003.controllers.responses.MensajeResponse;

import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.PaqueteDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.StockDTO;

import ar.edu.utn.dds.k3003.services.LogisticaService;
import ar.edu.utn.dds.k3003.metrics.LogisticaMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@Tag(name = "Logística", description = "API del componente de Logística")
public class LogisticaController {

  private final LogisticaService service;
  private final LogisticaMetrics metrics;

  public LogisticaController(LogisticaService service, LogisticaMetrics metrics) {
    this.service = service;
    this.metrics = metrics;
  }

  @Operation(
      summary = "Crear un depósito",
      description = "Crea un depósito con su nombre, dirección y capacidad máxima. El algoritmo de matchmaking queda sin configurar hasta que sea asignado posteriormente.")
  @ApiResponse(
      responseCode = "200",
      description = "Depósito creado correctamente",
      content = @Content(schema = @Schema(implementation = DepositoDTO.class)))
  @ApiResponse(responseCode = "400", description = "Datos inválidos para crear el depósito")
  @PostMapping("/depositos")
  public ResponseEntity<DepositoDTO> crearDeposito(@Valid @RequestBody DepositoDTO request) {
    DepositoDTO deposito = service.crearDeposito(request);
    metrics.depositoCreado();
    return ResponseEntity.ok(deposito);
  }

  @Operation(summary = "Obtener todos los depósitos")
  @ApiResponse(
      responseCode = "200",
      description = "Lista de depósitos",
      content = @Content(schema = @Schema(implementation = DepositoDTO.class)))
  @GetMapping("/depositos")
  public ResponseEntity<List<DepositoDTO>> listarDepositos() {
    return ResponseEntity.ok(service.listarDepositos());
  }

  @Operation(summary = "Obtener depósito por ID")
  @ApiResponse(
      responseCode = "200",
      description = "Depósito encontrado",
      content = @Content(schema = @Schema(implementation = DepositoDTO.class)))
  @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
  @GetMapping("/depositos/{id}")
  public ResponseEntity<DepositoDTO> buscarDeposito(@PathVariable String id) {
    return ResponseEntity.ok(service.buscarDeposito(id));
  }

  @Operation(summary = "Eliminar un depósito")
  @ApiResponse(responseCode = "200", description = "Depósito eliminado correctamente")
  @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
  @DeleteMapping("/depositos/{id}")
  public ResponseEntity<MensajeResponse> eliminarDeposito(@PathVariable String id) {
    service.eliminarDeposito(id);
    return ResponseEntity.ok(new MensajeResponse("Depósito eliminado correctamente"));
  }

  @Operation(summary = "Cambiar algoritmo de matchmaking de un depósito")
  @ApiResponse(responseCode = "200", description = "Algoritmo actualizado correctamente")
  @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
  @PatchMapping("/depositos/{id}/algoritmo")
  public ResponseEntity<MensajeResponse> cambiarAlgoritmo(
      @PathVariable("id") String depositoID,
      @Valid @RequestBody AlgoritmoDepositoRequest request
  ) {
    service.cambiarAlgoritmo(depositoID, request.algoritmo());
    return ResponseEntity.ok(new MensajeResponse("Algoritmo actualizado correctamente"));
  }

  @Operation(summary = "Gestionar una donación")
  @ApiResponse(
      responseCode = "202",
      description = "Donación registrada y enviada a procesamiento",
      content = @Content(schema = @Schema(implementation = DepositoDTO.class)))
  @ApiResponse(responseCode = "400", description = "Datos inválidos para gestionar la donación")
  @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
  @PostMapping("/depositos/{id}/donacion")
  public ResponseEntity<DepositoDTO> gestionarDonacion(
      @PathVariable("id") String depositoID,
      @Valid @RequestBody GestionDonacionRequest request
  ) {
    DepositoDTO deposito =
        service.gestionarDonacion(depositoID, request.donacionID(), request.productoID(), request.cantidad());

    metrics.donacionGestionada();

    return ResponseEntity.status(HttpStatus.ACCEPTED).body(deposito);
  }

  @Operation(summary = "Obtener una asignación por ID")
  @ApiResponse(
      responseCode = "200",
      description = "Asignación encontrada",
      content = @Content(schema = @Schema(implementation = AsignacionDTO.class)))
  @ApiResponse(responseCode = "404", description = "Asignación no encontrada")
  @GetMapping("/asignaciones/{id}")
  public ResponseEntity<AsignacionDTO> buscarAsignacion(@PathVariable String id) {
    return ResponseEntity.ok(service.buscarAsignacion(id));
  }

  @Operation(summary = "Registrar la entrega de un paquete")
  @ApiResponse(responseCode = "201", description = "Entrega registrada correctamente")
  @ApiResponse(responseCode = "404", description = "Paquete o asignación no encontrados")
  @PostMapping("/entregas")
  public ResponseEntity<MensajeResponse> reportarEntrega(@RequestBody PaqueteDTO paqueteDTO) {    
    service.reportarEntrega(paqueteDTO);
    metrics.entregaReportada();
    return ResponseEntity.ok(new MensajeResponse("Entrega registrada correctamente"));
  }

  @Operation(
      summary = "Consultar stock por producto",
      description = "Obtiene el stock disponible en los depósitos para un producto determinado."
  )
  @ApiResponse(
      responseCode = "200",
      description = "Stock obtenido correctamente",
      content = @Content(
          schema = @Schema(implementation = StockDTO.class)
      )
  )
  @ApiResponse(
      responseCode = "404",
      description = "Producto o recurso no encontrado"
  )
  @GetMapping("/stock/{productoId}")
  public ResponseEntity<StockDTO> consultarStock(@PathVariable String productoId) {
    metrics.consultaStock();

    return ResponseEntity.ok(
        service.consultarStock(productoId)
    );
  }

  @Operation(
      summary = "Solicitar asignaciones desde stock",
      description = "Solicita asignaciones de paquetes disponibles en stock para satisfacer una necesidad material."
  )
  @ApiResponse(
      responseCode = "200",
      description = "Asignaciones realizadas correctamente",
      content = @Content(
          schema = @Schema(implementation = AsignacionDTO.class)
      )
  )
  @ApiResponse(
      responseCode = "204",
      description = "No existen paquetes disponibles para realizar la asignación"
  )
  @ApiResponse(
      responseCode = "400",
      description = "Datos inválidos"
  )
  @ApiResponse(
      responseCode = "404",
      description = "Necesidad o producto no encontrado"
  )
  @PostMapping("/stock/asignaciones")
  public ResponseEntity<List<AsignacionDTO>> asignarDesdeStock(
      @Valid @RequestBody SolicitudAsignacionStockRequest request
  ) {
      List<AsignacionDTO> asignaciones =
          service.asignarDesdeStock(
              request.necesidadId(),
              request.productoId(),
              request.cantidad()
          );

      if (!asignaciones.isEmpty())
          asignaciones.forEach(asig -> metrics.asignacionSolicitudEntidad());

      if (asignaciones.isEmpty())
          return ResponseEntity.noContent().build();

      return ResponseEntity.ok(asignaciones);
  }
}
