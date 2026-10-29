package com.campuslands.tickets.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsignarTecnicoRequest(

        @NotNull(message = "El id del técnico es obligatorio")
        @Positive(message = "El id del técnico debe ser positivo")
        Long tecnicoId
) {
}
