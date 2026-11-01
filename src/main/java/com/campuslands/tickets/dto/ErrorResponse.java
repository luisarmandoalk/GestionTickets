package com.campuslands.tickets.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Estructura única para todas las respuestas de error de la API. */
public record ErrorResponse(
        @JsonFormat(pattern = FormatoFecha.PATRON) LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ErrorCampo> errores
) {

    public static ErrorResponse de(HttpStatus estado, String mensaje, String ruta) {
        return de(estado, mensaje, ruta, List.of());
    }

    public static ErrorResponse de(HttpStatus estado, String mensaje, String ruta, List<ErrorCampo> errores) {
        return new ErrorResponse(LocalDateTime.now(), estado.value(), estado.name(), mensaje, ruta, errores);
    }

    public record ErrorCampo(String campo, String mensaje) {
    }
}
