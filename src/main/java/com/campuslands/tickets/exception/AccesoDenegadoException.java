package com.campuslands.tickets.exception;

/** El rol permite la operación, pero no sobre este recurso (por ejemplo, un USUARIO con el ticket de otra persona). */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
