package ar.edu.utn.dds.k3003.controllers;

import ar.edu.utn.dds.k3003.services.AdminDbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/db")
public class AdminDbController {

  private final AdminDbService adminDbService;

  public AdminDbController(AdminDbService adminDbService) {
    this.adminDbService = adminDbService;
  }

  @GetMapping("/status")
  public ResponseEntity<AdminDbService.DbStatusResponse> status() {
    return ResponseEntity.ok(adminDbService.status());
  }

  @DeleteMapping
  public ResponseEntity<Void> clear() {
    adminDbService.clear();
    return ResponseEntity.noContent().build();
  }
}
