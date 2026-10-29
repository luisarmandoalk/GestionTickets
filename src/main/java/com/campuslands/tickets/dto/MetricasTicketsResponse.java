package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.entity.Prioridad;
import java.util.Map;

public record MetricasTicketsResponse(
        long total,
        Map<Categoria, Long> porCategoria,
        Map<Prioridad, Long> porPrioridad,
        Map<EstadoTicket, Long> porEstado
) {
}
