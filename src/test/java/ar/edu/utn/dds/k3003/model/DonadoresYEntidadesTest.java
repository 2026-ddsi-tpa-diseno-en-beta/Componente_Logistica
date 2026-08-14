package ar.edu.utn.dds.k3003.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import ar.edu.utn.dds.k3003.Fachada;
import org.junit.jupiter.api.Test;

class DonadoresYEntidadesTest {

  @Test
  void fachadaTieneConstructorSinSpring() {
    assertNotNull(new Fachada());
  }
}
