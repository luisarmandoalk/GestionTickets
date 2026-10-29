package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.EstadoTicket;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record HistorialEstadoResponse(
        @Schema(description = "Nulo en el registro de creación del ticket", example = "ABIERTO")
        EstadoTicket estadoAnterior,
        @Schema(example = "EN_PROCESO") EstadoTicket estadoNuevo,
        @JsonFormat(pattern = FormatoFecha.PATRON) LocalDateTime fechaCambio
) {
}
