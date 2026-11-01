package com.campuslands.tickets.exception;

/** Correo o contraseña incorrectos. El mensaje no dice cuál de los dos falló. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }
}
