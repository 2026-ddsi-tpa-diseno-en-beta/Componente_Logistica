package ar.edu.utn.dds.k3003.persistence.adapter;

import ar.edu.utn.dds.k3003.exceptions.BusinessRuleException;

final class IdsPersistidos {
  private IdsPersistidos() {}
  static Long numerico(String id) {
    try {
      long value = Long.parseLong(id);
      if (value <= 0) throw new NumberFormatException();
      return value;
    } catch (NumberFormatException ex) {
      throw new BusinessRuleException("El ID debe ser un número positivo");
    }
  }
}
