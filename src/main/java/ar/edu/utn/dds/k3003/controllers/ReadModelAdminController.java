package ar.edu.utn.dds.k3003.controllers;

import ar.edu.utn.dds.k3003.Fachada;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("!worker")
@RequestMapping("/admin/read-model")
@ConditionalOnProperty(name = "donatrack.read-model.publisher.enabled", havingValue = "true")
public class ReadModelAdminController {
  private final Fachada fachada;
  private final String adminKey;

  public ReadModelAdminController(
      Fachada fachada, @Value("${READ_MODEL_ADMIN_KEY:}") String adminKey) {
    this.fachada = fachada;
    this.adminKey = adminKey;
  }

  @PostMapping("/republicar")
  public ResponseEntity<Map<String, Object>> republicar(
      @RequestHeader(value = "X-Admin-Key", defaultValue = "") String suppliedKey) {
    if (adminKey.isBlank()
        || !MessageDigest.isEqual(
            adminKey.getBytes(StandardCharsets.UTF_8), suppliedKey.getBytes(StandardCharsets.UTF_8))) {
      return ResponseEntity.status(403)
          .body(Map.of("detalle", "Clave de sincronización requerida"));
    }
    fachada.republicarModeloLectura();
    return ResponseEntity.ok(
        Map.of("estado", "ENCOLADO", "detalle", "Los snapshots de Logística se enviarán por RabbitMQ"));
  }
}
