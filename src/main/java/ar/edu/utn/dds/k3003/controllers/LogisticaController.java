package ar.edu.utn.dds.k3003.controllers;

import ar.edu.utn.dds.k3003.controllers.requests.logistica.DepositoRequest;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.GestionDonacionRequest;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.PaqueteDTO;

import ar.edu.utn.dds.k3003.services.LogisticaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@Tag(name = "Logística", description = "API del componente de Logística")
public class LogisticaController {

  private final LogisticaService service;

  public LogisticaController(LogisticaService service) {
    this.service = service;
  }

  @Operation(summary = "Crear un depósito")
  @ApiResponse(
      responseCode = "201",
      description = "Depósito creado correctamente",
      content = @Content(schema = @Schema(implementation = DepositoDTO.class)))
  @PostMapping("/depositos")
  public ResponseEntity<DepositoDTO> crearDeposito(@RequestBody DepositoRequest request) {
    DepositoDTO deposito =
        service.crearDeposito(request.nombre(), request.direccion(), request.capacidadMaxima());

    return ResponseEntity.status(HttpStatus.CREATED).body(deposito);
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
  public ResponseEntity<?> buscarDeposito(@PathVariable String id) {
    try {
      return ResponseEntity.ok(service.buscarDeposito(id));
    } catch (Exception ex) {
      return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body("Depósito no encontrado");
    }
  }

  @Operation(summary = "Eliminar un depósito")
  @ApiResponse(responseCode = "200", description = "Depósito eliminado correctamente")
  @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
  @DeleteMapping("/depositos/{id}")
  public ResponseEntity<?> eliminarDeposito(@PathVariable String id) {
    try {
      service.eliminarDeposito(id);
      return ResponseEntity.ok("Depósito eliminado correctamente");
    } catch (Exception ex) {
      return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body("Depósito no encontrado");
    }
  }

  @Operation(summary = "Gestionar una donación")
  @ApiResponse(
      responseCode = "201",
      description = "Donación registrada en el depósito",
      content = @Content(schema = @Schema(implementation = DepositoDTO.class)))
  @ApiResponse(responseCode = "400", description = "Datos inválidos para gestionar la donación")
  @ApiResponse(responseCode = "404", description = "Depósito no encontrado")
  @PostMapping("/depositos/{id}/donacion")
  public ResponseEntity<?> gestionarDonacion(
      @PathVariable("id") String depositoID,
      @RequestBody GestionDonacionRequest request) {
    try {
      DepositoDTO deposito =
          service.gestionarDonacion(
              depositoID, 
              request.donacionID(), 
              request.productoID(), 
              request.cantidad());

      return ResponseEntity.status(HttpStatus.CREATED).body(deposito);
    } catch (IllegalArgumentException ex) {

      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(ex.getMessage());
    } catch (Exception ex) {

      return ResponseEntity
          .status(HttpStatus.NOT_FOUND)
          .body("Depósito no encontrado");
    }
  }

  @Operation(summary = "Obtener una asignación por ID")
  @ApiResponse(
      responseCode = "200",
      description = "Asignación encontrada",
      content = @Content(schema = @Schema(implementation = AsignacionDTO.class)))
  @ApiResponse(responseCode = "404", description = "Asignación no encontrada")
  @GetMapping("/asignaciones/{id}")
  public ResponseEntity<?> buscarAsignacion(@PathVariable String id) {
    try {
      return ResponseEntity.ok(service.buscarAsignacion(id));
    } catch (Exception ex) {
      return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body("Asignación no encontrada");
    }
  }

  @Operation(summary = "Registrar la entrega de un paquete")
  @ApiResponse(responseCode = "201", description = "Entrega registrada correctamente")
  @ApiResponse(responseCode = "404", description = "Paquete o asignación no encontrados")
  @PostMapping("/entregas")
  public ResponseEntity<?> reportarEntrega(@RequestBody PaqueteDTO paqueteDTO) {
    try {
      service.reportarEntrega(paqueteDTO);

      return ResponseEntity
          .status(HttpStatus.CREATED)
          .body("Entrega registrada correctamente");
    } catch (Exception ex) {
      return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body("Paquete o asignación no encontrados");
    }
  }
}
