package com.campuslands.tickets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearTicketRequest(

        @Schema(example = "Problema con internet")
        @NotBlank(message = "El título es obligatorio")
        @Size(min = 5, max = 100, message = "El título debe tener entre 5 y 100 caracteres")
        String titulo,

        @Schema(example = "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.")
        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 20, max = 2000, message = "La descripción debe tener entre 20 y 2000 caracteres")
        String descripcion,

        @Schema(description = "Id del usuario que reporta el problema", example = "1")
        @NotNull(message = "El id del usuario solicitante es obligatorio")
        @Positive(message = "El id del usuario solicitante debe ser positivo")
        Long usuarioId
) {
}
