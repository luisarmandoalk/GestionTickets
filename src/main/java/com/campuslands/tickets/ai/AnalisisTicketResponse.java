package com.campuslands.tickets.ai;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import java.util.List;

/** Resultado validado del análisis: valores dentro de los catálogos, resumen corto y de 1 a 5 etiquetas. */
public record AnalisisTicketResponse(
        Categoria categoria,
        Prioridad prioridad,
        String resumen,
        List<String> etiquetas,
        OrigenAnalisis origen
) {

    public AnalisisTicketResponse {
        etiquetas = List.copyOf(etiquetas);
    }
}
