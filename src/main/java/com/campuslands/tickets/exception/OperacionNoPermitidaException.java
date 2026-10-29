package com.campuslands.tickets.exception;

/** Operación válida en su forma pero no permitida por el estado actual del recurso. */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
