package com.campuslands.tickets.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida del ticket: ABIERTO → EN_PROCESO → RESUELTO → CERRADO.
 * Un ticket RESUELTO puede volver a EN_PROCESO (reapertura); CERRADO es un estado final.
 */
public enum EstadoTicket {
    ABIERTO,
    EN_PROCESO,
    RESUELTO,
    CERRADO;

    public Set<EstadoTicket> transicionesPermitidas() {
        return switch (this) {
            case ABIERTO -> EnumSet.of(EN_PROCESO);
            case EN_PROCESO -> EnumSet.of(RESUELTO);
            case RESUELTO -> EnumSet.of(CERRADO, EN_PROCESO);
            case CERRADO -> EnumSet.noneOf(EstadoTicket.class);
        };
    }

    public boolean permiteTransicionA(EstadoTicket destino) {
        return transicionesPermitidas().contains(destino);
    }

    public boolean admiteAsignacionDeTecnico() {
        return this == ABIERTO || this == EN_PROCESO;
    }
}
