package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record TicketResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Problema con internet") String titulo,
        @Schema(example = "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.")
        String descripcion,
        @Schema(example = "CONECTIVIDAD") Categoria categoria,
        @Schema(example = "ALTA") Prioridad prioridad,
        @Schema(example = "EN_PROCESO") EstadoTicket estado,
        @Schema(example = "Usuario sin conexión a internet antes de una reunión importante.") String resumenIA,
        @Schema(example = "[\"internet\", \"conectividad\", \"reunión\"]") List<String> etiquetasIA,
        @Schema(description = "MODELO_LENGUAJE si clasificó la IA; REGLAS si se usó el clasificador de respaldo",
                example = "MODELO_LENGUAJE")
        OrigenAnalisis origenAnalisis,
        UsuarioResumenResponse solicitante,
        @Schema(description = "Nulo mientras el ticket no tenga técnico asignado",
                example = "{\"id\": 5, \"nombre\": \"Sofía Ramírez\"}")
        UsuarioResumenResponse tecnicoAsignado,
        @JsonFormat(pattern = FormatoFecha.PATRON) LocalDateTime fechaCreacion,
        @JsonFormat(pattern = FormatoFecha.PATRON) LocalDateTime fechaActualizacion
) {
}
