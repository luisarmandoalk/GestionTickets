package com.campuslands.tickets.exception;

public abstract class RecursoNoEncontradoException extends RuntimeException {

    protected RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
