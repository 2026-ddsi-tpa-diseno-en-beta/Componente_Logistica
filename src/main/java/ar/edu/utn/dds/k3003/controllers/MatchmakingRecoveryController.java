package ar.edu.utn.dds.k3003.controllers;

import ar.edu.utn.dds.k3003.Fachada;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/matchmaking")
public class MatchmakingRecoveryController {
  private final Fachada facade;
  private final String key;

  public MatchmakingRecoveryController(
      Fachada facade, @Value("${READ_MODEL_ADMIN_KEY:}") String key) {
    this.facade = facade;
    this.key = key;
  }

  @PostMapping("/recuperar")
  public ResponseEntity<Map<String, Object>> recover(
      @RequestHeader(value = "X-Admin-Key", defaultValue = "") String supplied) {
    if (key.isBlank()
        || !MessageDigest.isEqual(
            key.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8)))
      return ResponseEntity.status(403).body(Map.of("detalle", "Clave de recuperación requerida"));
    return ResponseEntity.ok(
        Map.of("estado", "ENCOLADO", "paquetes", facade.recuperarMatchmakingPendiente()));
  }
}
