package com.campuslands.tickets.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
        @Schema(description = "JWT para la cabecera Authorization: Bearer <token>",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sIjoiVVNVQVJJTyJ9.firma")
        String token,
        @Schema(example = "Bearer") String tipo,
        @Schema(description = "Segundos de vigencia del token", example = "3600") long expiraEn,
        UsuarioResponse usuario
) {
}
