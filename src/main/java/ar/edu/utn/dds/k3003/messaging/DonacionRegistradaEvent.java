package ar.edu.utn.dds.k3003.messaging;

import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;

public record DonacionRegistradaEvent(
    DonacionPendienteMessage message
) {}
