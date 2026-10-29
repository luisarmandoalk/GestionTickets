package com.campuslands.tickets.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/** Estructura única para todas las respuestas de error de la API. */
public record ErrorResponse(
        @JsonFormat(pattern = FormatoFecha.PATRON) LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ErrorCampo> errores
) {

    public record ErrorCampo(String campo, String mensaje) {
    }
}
