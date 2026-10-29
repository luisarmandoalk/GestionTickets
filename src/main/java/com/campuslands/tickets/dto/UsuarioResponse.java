package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.Rol;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record UsuarioResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Laura Gómez") String nombre,
        @Schema(example = "laura.gomez@empresa.com") String correo,
        @Schema(example = "USUARIO") Rol rol,
        @JsonFormat(pattern = FormatoFecha.PATRON) LocalDateTime fechaCreacion
) {
}
