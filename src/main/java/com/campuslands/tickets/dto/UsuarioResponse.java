package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.Rol;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        Rol rol,
        @JsonFormat(pattern = FormatoFecha.PATRON) LocalDateTime fechaCreacion
) {
}
