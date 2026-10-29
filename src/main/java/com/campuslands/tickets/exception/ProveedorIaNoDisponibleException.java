package com.campuslands.tickets.exception;

/** No fue posible obtener respuesta del proveedor de IA (conexión, timeout, credenciales o error HTTP). */
public class ProveedorIaNoDisponibleException extends AnalisisIaException {

    public ProveedorIaNoDisponibleException(String mensaje, boolean reintentable, Throwable causa) {
        super(mensaje, reintentable, causa);
    }
}
