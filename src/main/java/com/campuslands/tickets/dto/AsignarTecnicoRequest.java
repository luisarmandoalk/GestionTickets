package com.campuslands.tickets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AsignarTecnicoRequest(

        @Schema(description = "Id de un usuario con rol SOPORTE", example = "5")
        @NotNull(message = "El id del técnico es obligatorio")
        @Positive(message = "El id del técnico debe ser positivo")
        Long tecnicoId
) {
}
