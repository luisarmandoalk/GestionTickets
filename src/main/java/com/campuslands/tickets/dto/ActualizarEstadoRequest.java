package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.EstadoTicket;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoRequest(

        @Schema(example = "EN_PROCESO")
        @NotNull(message = "El estado es obligatorio")
        EstadoTicket estado
) {
}
