package ar.edu.utn.dds.k3003;
import ar.edu.utn.dds.k3003.catedra.dtos.logistica.*;
import ar.edu.utn.dds.k3003.catedra.fachadas.*;
import ar.edu.utn.dds.k3003.controllers.requests.logistica.ResultadoMatchmakingRequest;
import ar.edu.utn.dds.k3003.exceptions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class EntregaLoteTest {
  Fachada facade; FachadaDonadoresYEntidades donors; FachadaDonaciones donations; String depot;
  @BeforeEach void setup() {
    facade=new Fachada();donors=mock(FachadaDonadoresYEntidades.class);donations=mock(FachadaDonaciones.class);
    facade.setFachadaDonadoresYEntidades(donors);facade.setFachadaDonaciones(donations);
    depot=facade.agregarDeposito(new DepositoDTO(null,null,"Depósito","Dirección",100,null)).id();
    facade.setAlgoritmoMM(depot,TipoAlgoritmoEnum.SUB_ATENDIDOS);
  }
  String pack(String donation,int quantity,String need) {
    var dto=facade.gestionarDonacion(depot,donation,"p",quantity);
    var paquete=dto.stockActual().getLast();
    facade.registrarResultadoMatchmaking(new ResultadoMatchmakingRequest(depot,paquete.id(),need,quantity,0));
    return paquete.id();
  }
  @Test void satisfaceUnaVezConLaSumaDelLote() {
    String a=pack("a",4,"n"),b=pack("b",6,"n");
    facade.reportarEntregaLote(List.of(a,b));
    verify(donors,times(1)).satisfacerNecesidad("n",10);
    assertEquals(EstadoAsignacionEnum.COMPLETADA,facade.buscarAsignacionPorPaqueteID(a).estado());
    assertEquals(EstadoAsignacionEnum.COMPLETADA,facade.buscarAsignacionPorPaqueteID(b).estado());
    assertEquals(0,facade.ocupacionTotal());
  }
  @Test void loteNoPuedeMezclarNecesidades() {
    String a=pack("a",4,"n"),b=pack("b",6,"otra");
    assertThrows(ConflictException.class,()->facade.reportarEntregaLote(List.of(a,b)));verifyNoInteractions(donors);
  }
  @Test void paqueteRepetidoNoSumaDosVeces() {
    String a=pack("a",4,"n");
    assertThrows(BusinessRuleException.class,()->facade.reportarEntregaLote(List.of(a,a)));verifyNoInteractions(donors);
  }
  @Test void reentregaNoVuelveASatisfacerLaNecesidad() {
    String a=pack("a",4,"n");facade.reportarEntregaLote(List.of(a));
    assertThrows(ConflictException.class,()->facade.reportarEntregaLote(List.of(a)));verify(donors,times(1)).satisfacerNecesidad("n",4);
  }
  @Test void redeliveryDelWorkerDespuesDeEntregaEsIdempotente() {
    String a=pack("a",4,"n");facade.reportarEntregaLote(List.of(a));
    var resultado=facade.registrarResultadoMatchmakingDetallado(new ResultadoMatchmakingRequest(depot,a,"n",4,0));
    assertFalse(resultado.nuevo());assertEquals(1,facade.listarAsignaciones().size());
  }
}
