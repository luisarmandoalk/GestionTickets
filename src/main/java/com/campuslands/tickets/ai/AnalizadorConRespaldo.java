package com.campuslands.tickets.ai;

import com.campuslands.tickets.exception.AnalisisIaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Usa el analizador principal y, si falla (proveedor caído o respuesta inválida), recurre al de respaldo. */
public class AnalizadorConRespaldo implements AnalizadorTicketService {

    private static final Logger log = LoggerFactory.getLogger(AnalizadorConRespaldo.class);

    private final AnalizadorTicketService principal;
    private final AnalizadorTicketService respaldo;

    public AnalizadorConRespaldo(AnalizadorTicketService principal, AnalizadorTicketService respaldo) {
        this.principal = principal;
        this.respaldo = respaldo;
    }

    @Override
    public AnalisisTicketResponse analizar(String titulo, String descripcion) {
        try {
            return principal.analizar(titulo, descripcion);
        } catch (AnalisisIaException e) {
            log.warn("El analizador principal falló ({}); se usa el analizador de respaldo", e.getMessage());
            return respaldo.analizar(titulo, descripcion);
        }
    }
}
