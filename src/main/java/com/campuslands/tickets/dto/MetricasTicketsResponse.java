package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.entity.Prioridad;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public record MetricasTicketsResponse(
        @Schema(example = "12") long total,
        @Schema(example = "{\"HARDWARE\": 2, \"SOFTWARE\": 4, \"CONECTIVIDAD\": 3, \"ACCESOS\": 2, \"OTRO\": 1}")
        Map<Categoria, Long> porCategoria,
        @Schema(example = "{\"BAJA\": 3, \"MEDIA\": 4, \"ALTA\": 3, \"CRITICA\": 2}")
        Map<Prioridad, Long> porPrioridad,
        @Schema(example = "{\"ABIERTO\": 5, \"EN_PROCESO\": 3, \"RESUELTO\": 2, \"CERRADO\": 2}")
        Map<EstadoTicket, Long> porEstado
) {
}
