package com.campuslands.tickets.dto;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.entity.Prioridad;

/** Criterios opcionales para consultar tickets; {@code texto} busca en título y descripción. */
public record FiltroTickets(
        EstadoTicket estado,
        Categoria categoria,
        Prioridad prioridad,
        Long usuarioId,
        Long tecnicoId,
        String texto
) {

    /** El mismo filtro restringido a los tickets de un solicitante. */
    public FiltroTickets soloDelSolicitante(Long solicitanteId) {
        return new FiltroTickets(estado, categoria, prioridad, solicitanteId, tecnicoId, texto);
    }
}
