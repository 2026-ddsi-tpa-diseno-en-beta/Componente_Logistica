package ar.edu.utn.dds.k3003.controllers;

import ar.edu.utn.dds.k3003.exceptions.BusinessRuleException;
import ar.edu.utn.dds.k3003.exceptions.ConflictException;
import ar.edu.utn.dds.k3003.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  public record ErrorResponse(LocalDateTime timestamp, String code, String message, String path) {}

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(LocalDateTime.now(), "NOT_FOUND", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(LocalDateTime.now(), "CONFLICT", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ErrorResponse> handleBusiness(BusinessRuleException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse(LocalDateTime.now(), "BUSINESS_RULE", ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .orElse("Datos inválidos");

    return ResponseEntity.badRequest()
        .body(new ErrorResponse(LocalDateTime.now(), "VALIDATION_ERROR", message, request.getRequestURI()));
  }
}
