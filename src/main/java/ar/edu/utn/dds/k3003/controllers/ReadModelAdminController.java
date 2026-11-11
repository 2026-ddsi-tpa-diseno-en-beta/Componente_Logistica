package ar.edu.utn.dds.k3003.controllers;

import ar.edu.utn.dds.k3003.Fachada;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "donatrack.read-model.publisher.enabled",
    havingValue = "true")
@RestController
@RequestMapping("/admin/read-model")
public class ReadModelAdminController {
  private final Fachada facade;
  private final String key;

  public ReadModelAdminController(Fachada facade, @Value("${READ_MODEL_ADMIN_KEY:}") String key) {
    this.facade = facade;
    this.key = key;
  }

  @PostMapping("/republicar")
  public org.springframework.http.ResponseEntity<Map<String, Object>> replay(
      @RequestHeader(value = "X-Admin-Key", defaultValue = "") String supplied) {
    if (key.isBlank()
        || !java.security.MessageDigest.isEqual(
            key.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
      return org.springframework.http.ResponseEntity.status(403)
          .body(Map.of("detalle", "Clave de sincronización requerida"));
    facade.republicarModeloLectura();
    return org.springframework.http.ResponseEntity.ok(
        Map.of("estado", "ENCOLADO", "detalle", "Los snapshots se enviarán por RabbitMQ"));
  }
}
