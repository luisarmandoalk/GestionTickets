package com.campuslands.tickets.exception;

public class CorreoDuplicadoException extends RuntimeException {

    public CorreoDuplicadoException(String correo) {
        super("Ya existe un usuario registrado con el correo " + correo);
    }
}
