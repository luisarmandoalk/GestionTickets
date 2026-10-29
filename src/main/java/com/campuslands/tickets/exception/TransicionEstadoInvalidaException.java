package com.campuslands.tickets.exception;

import com.campuslands.tickets.entity.EstadoTicket;

public class TransicionEstadoInvalidaException extends RuntimeException {

    public TransicionEstadoInvalidaException(EstadoTicket actual, EstadoTicket destino) {
        super(construirMensaje(actual, destino));
    }

    private static String construirMensaje(EstadoTicket actual, EstadoTicket destino) {
        if (actual.transicionesPermitidas().isEmpty()) {
            return "El ticket está en estado %s y no admite más cambios de estado".formatted(actual);
        }
        return "No se puede cambiar el estado de %s a %s. Desde %s solo se permite: %s"
                .formatted(actual, destino, actual, actual.transicionesPermitidas());
    }
}
