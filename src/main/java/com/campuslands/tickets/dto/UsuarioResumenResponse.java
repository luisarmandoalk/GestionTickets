package com.campuslands.tickets.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UsuarioResumenResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Laura Gómez") String nombre
) {
}
