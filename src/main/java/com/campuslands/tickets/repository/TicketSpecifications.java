package com.campuslands.tickets.repository;

import com.campuslands.tickets.dto.FiltroTickets;
import com.campuslands.tickets.entity.Ticket;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecifications {

    private static final char ESCAPE = '\\';

    private TicketSpecifications() {
    }

    /** Combina con AND los filtros informados; los filtros nulos o vacíos se ignoran. */
    public static Specification<Ticket> conFiltros(FiltroTickets filtro) {
        return (raiz, consulta, criterios) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (filtro.estado() != null) {
                predicados.add(criterios.equal(raiz.get("estado"), filtro.estado()));
            }
            if (filtro.categoria() != null) {
                predicados.add(criterios.equal(raiz.get("categoria"), filtro.categoria()));
            }
            if (filtro.prioridad() != null) {
                predicados.add(criterios.equal(raiz.get("prioridad"), filtro.prioridad()));
            }
            if (filtro.usuarioId() != null) {
                predicados.add(criterios.equal(raiz.get("usuarioSolicitante").get("id"), filtro.usuarioId()));
            }
            if (filtro.tecnicoId() != null) {
                predicados.add(criterios.equal(raiz.get("tecnicoAsignado").get("id"), filtro.tecnicoId()));
            }
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = "%" + escaparComodines(filtro.texto().trim().toLowerCase(Locale.ROOT)) + "%";
                predicados.add(criterios.or(
                        criterios.like(criterios.lower(raiz.<String>get("titulo")), patron, ESCAPE),
                        criterios.like(criterios.lower(raiz.<String>get("descripcion")), patron, ESCAPE)));
            }
            return criterios.and(predicados.toArray(Predicate[]::new));
        };
    }

    private static String escaparComodines(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
