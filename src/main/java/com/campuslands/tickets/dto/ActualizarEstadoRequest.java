package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.EstadoTicket;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoTicket estado
) {
}
