package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public record TicketResponse(
        Long id,
        String titulo,
        String descripcion,
        Categoria categoria,
        Prioridad prioridad,
        EstadoTicket estado,
        String resumenIA,
        List<String> etiquetasIA,
        OrigenAnalisis origenAnalisis,
        UsuarioResumenResponse solicitante,
        UsuarioResumenResponse tecnicoAsignado,
        @JsonFormat(pattern = FormatoFecha.PATRON) LocalDateTime fechaCreacion,
        @JsonFormat(pattern = FormatoFecha.PATRON) LocalDateTime fechaActualizacion
) {
}
