package com.campuslands.tickets.dto;

/** Formato único para las fechas expuestas por la API: hora local, sin zona horaria ni milisegundos. */
public final class FormatoFecha {

    public static final String PATRON = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String EJEMPLO = "2026-07-30T19:30:00";

    private FormatoFecha() {
    }
}
