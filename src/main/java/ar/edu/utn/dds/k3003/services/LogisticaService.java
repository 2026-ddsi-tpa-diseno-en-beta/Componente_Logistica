package ar.edu.utn.dds.k3003.services;

import ar.edu.utn.dds.k3003.Fachada;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.AsignacionDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.DepositoDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.PaqueteDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.StockDTO;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.TipoAlgoritmoEnum;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LogisticaService {

  private final Fachada fachada;

  public LogisticaService(Fachada fachada) {
    this.fachada = fachada;
  }

  public DepositoDTO crearDeposito(DepositoDTO deposito) {
    return fachada.agregarDeposito(deposito);
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

  public DepositoDTO gestionarDonacion(
      String depositoID,
      String donacionID,
      String productoID,
      Integer cantidad) {
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
      Integer cantidad) {
    return fachada.asignarDesdeStock(necesidadId, productoId, cantidad);
  }

  public AsignacionDTO registrarResultadoMatchmaking(ResultadoMatchmakingRequest request) {
    return fachada.registrarResultadoMatchmaking(request);
  }

  public int cantidadAsignadaPorNecesidad(String necesidadId) {
    return fachada.cantidadAsignadaPorNecesidad(necesidadId);
  }
}
