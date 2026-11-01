package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.AccionAuditoria;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AuditoriaTicketResponse(
        @Schema(example = "ASIGNACION") AccionAuditoria accion,
        @Schema(example = "Técnico asignado: Sofía Ramírez (id 5)") String detalle,
        @Schema(description = "Usuario que hizo el cambio", example = "{\"id\": 3, \"nombre\": \"Andrea Ruiz\"}")
        UsuarioResumenResponse usuario,
        @JsonFormat(pattern = FormatoFecha.PATRON) LocalDateTime fecha
) {
}
