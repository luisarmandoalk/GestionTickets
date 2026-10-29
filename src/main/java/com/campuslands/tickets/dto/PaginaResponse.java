package com.campuslands.tickets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaResponse<T>(
        List<T> contenido,
        @Schema(description = "Número de página, empieza en 0", example = "0") int pagina,
        @Schema(example = "20") int tamano,
        @Schema(example = "1") long totalElementos,
        @Schema(example = "1") int totalPaginas
) {

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
