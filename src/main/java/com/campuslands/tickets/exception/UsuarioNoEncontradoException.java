package com.campuslands.tickets.exception;

public class UsuarioNoEncontradoException extends RecursoNoEncontradoException {

    private UsuarioNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static UsuarioNoEncontradoException usuario(Long id) {
        return new UsuarioNoEncontradoException("No se encontró el usuario con id " + id);
    }

    public static UsuarioNoEncontradoException tecnico(Long id) {
        return new UsuarioNoEncontradoException("No se encontró el técnico con id " + id);
    }
}
