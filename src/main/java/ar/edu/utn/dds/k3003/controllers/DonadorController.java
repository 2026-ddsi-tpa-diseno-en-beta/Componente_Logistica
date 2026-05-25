// package ar.edu.utn.dds.k3003.controllers;

// import ar.edu.utn.dds.k3003.Fachada;
// import ar.edu.utn.dds.k3003.catedra.dtos.donadoresYEntidades.DonadorDTO;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// @RestController
// @RequestMapping("/donadores")
// public class DonadorController {

//   private Fachada fachada;

//   public DonadorController(Fachada fachada) {
//     this.fachada = fachada;
//   }

//   // Opcion 1 utilizando @RequestMapping
//   @RequestMapping(method = RequestMethod.POST)
//   public ResponseEntity<DonadorDTO> postDonador(@RequestBody DonadorDTO donadorDTO) {
//     DonadorDTO donadorAgregado = fachada.agregarDonador(donadorDTO);
//     return ResponseEntity.ok(donadorAgregado);
//   }

//   // Opcion 2 utilizando @GetMapping
//   @GetMapping
//   public ResponseEntity<DonadorDTO> getDonadorByID(@RequestParam String donadorID) {
//     return ResponseEntity.status(HttpStatus.OK).body(this.fachada.buscarDonadorPorID(donadorID));
//   }
// }
