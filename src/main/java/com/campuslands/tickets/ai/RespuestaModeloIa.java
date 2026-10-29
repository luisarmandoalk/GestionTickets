package com.campuslands.tickets.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** JSON tal como lo devuelve el modelo, antes de validarlo. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RespuestaModeloIa(
        String categoria,
        String prioridad,
        String resumen,
        List<String> etiquetas
) {
}
