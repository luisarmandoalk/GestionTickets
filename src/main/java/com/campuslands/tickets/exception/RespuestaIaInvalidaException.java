package com.campuslands.tickets.exception;

/** El modelo respondió, pero su contenido no es un JSON válido o no cumple las reglas del análisis. */
public class RespuestaIaInvalidaException extends AnalisisIaException {

    public RespuestaIaInvalidaException(String mensaje) {
        super(mensaje, true, null);
    }

    public RespuestaIaInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, true, causa);
    }
}
