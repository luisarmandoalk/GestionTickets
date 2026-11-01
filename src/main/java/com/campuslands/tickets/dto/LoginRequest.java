package com.campuslands.tickets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @Schema(example = "laura.gomez@empresa.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        @Schema(example = "Demo1234*")
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
