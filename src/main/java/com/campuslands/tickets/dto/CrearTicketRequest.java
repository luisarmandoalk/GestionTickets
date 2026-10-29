package com.campuslands.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearTicketRequest(

        @NotBlank(message = "El título es obligatorio")
        @Size(min = 5, max = 100, message = "El título debe tener entre 5 y 100 caracteres")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 20, max = 2000, message = "La descripción debe tener entre 20 y 2000 caracteres")
        String descripcion,

        @NotNull(message = "El id del usuario solicitante es obligatorio")
        @Positive(message = "El id del usuario solicitante debe ser positivo")
        Long usuarioId
) {
}
