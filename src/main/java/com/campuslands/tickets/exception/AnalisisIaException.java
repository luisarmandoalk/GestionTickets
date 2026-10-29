package com.campuslands.tickets.exception;

/** Falla del analizador de inteligencia artificial; indica si vale la pena reintentar. */
public abstract class AnalisisIaException extends RuntimeException {

    private final boolean reintentable;

    protected AnalisisIaException(String mensaje, boolean reintentable, Throwable causa) {
        super(mensaje, causa);
        this.reintentable = reintentable;
    }

    public boolean esReintentable() {
        return reintentable;
    }
}
