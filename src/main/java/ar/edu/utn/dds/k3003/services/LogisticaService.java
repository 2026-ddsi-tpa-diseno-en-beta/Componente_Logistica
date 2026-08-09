package ar.edu.utn.dds.k3003.services;

import ar.edu.utn.dds.k3003.Fachada;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.*;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LogisticaService {

  private final Fachada fachada;

  public LogisticaService(Fachada fachada) {
    this.fachada = fachada;
  }

  public DepositoDTO crearDeposito(String nombre, String direccion, Integer capacidadMaxima) {
    return fachada.agregarDeposito(new DepositoDTO(null, null, nombre, direccion, capacidadMaxima, null));
  }

  public List<DepositoDTO> listarDepositos() {
    return fachada.buscarDepositos();
  }

  public DepositoDTO buscarDeposito(String id) {
    return fachada.buscarDepositoPorID(id);
  }

  public void eliminarDeposito(String id) {
    fachada.eliminarDeposito(id);
  }

  public void cambiarAlgoritmo(String depositoId, TipoAlgoritmoEnum algoritmo) {
    fachada.setAlgoritmoMM(depositoId, algoritmo);
  }

  public DepositoDTO gestionarDonacion(String depositoID, String donacionID, String productoID, Integer cantidad) {
    return fachada.gestionarDonacion(depositoID, donacionID, productoID, cantidad);
  }

  public AsignacionDTO buscarAsignacion(String id) {
    return fachada.buscarAsignacionPorID(id);
  }

  public void reportarEntrega(PaqueteDTO paqueteDTO) {
    fachada.reportarEntrega(paqueteDTO);
  }

  public StockDTO consultarStock(String productoId) {
      return fachada.consultarStock(productoId);
  }

  public List<AsignacionDTO> asignarDesdeStock(
      String necesidadId,
      String productoId,
      Integer cantidad
  ) {
      return fachada.asignarDesdeStock(
          necesidadId,
          productoId,
          cantidad
      );
  }

  public AsignacionDTO registrarResultadoMatchmaking(ResultadoMatchmakingRequest request) {
      return fachada.registrarResultadoMatchmaking(request);
  }
}
