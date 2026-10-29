package com.campuslands.tickets.exception;

public class TicketNoEncontradoException extends RecursoNoEncontradoException {

    public TicketNoEncontradoException(Long id) {
        super("No se encontró el ticket con id " + id);
    }
}
