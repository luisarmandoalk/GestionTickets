package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.EstadoTicket;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record HistorialEstadoResponse(
        EstadoTicket estadoAnterior,
        EstadoTicket estadoNuevo,
        @JsonFormat(pattern = FormatoFecha.PATRON) LocalDateTime fechaCambio
) {
}
