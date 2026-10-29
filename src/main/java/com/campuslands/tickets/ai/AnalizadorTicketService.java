package com.campuslands.tickets.ai;

/**
 * Analiza el contenido de un ticket y sugiere su clasificación. La lógica de negocio depende solo
 * de esta interfaz, por lo que el proveedor (modelo de lenguaje, reglas u otro) es intercambiable.
 */
public interface AnalizadorTicketService {

    AnalisisTicketResponse analizar(String titulo, String descripcion);
}
