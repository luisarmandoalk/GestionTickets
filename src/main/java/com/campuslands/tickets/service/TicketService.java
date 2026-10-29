package com.campuslands.tickets.service;

import com.campuslands.tickets.dto.CrearTicketRequest;
import com.campuslands.tickets.dto.FiltroTickets;
import com.campuslands.tickets.dto.HistorialEstadoResponse;
import com.campuslands.tickets.dto.MetricasTicketsResponse;
import com.campuslands.tickets.dto.PaginaResponse;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.entity.EstadoTicket;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface TicketService {

    TicketResponse crear(CrearTicketRequest solicitud);

    PaginaResponse<TicketResponse> listar(FiltroTickets filtro, Pageable paginacion);

    TicketResponse obtener(Long id);

    TicketResponse actualizarEstado(Long id, EstadoTicket nuevoEstado);

    TicketResponse asignarTecnico(Long id, Long tecnicoId);

    void eliminar(Long id);

    List<HistorialEstadoResponse> historial(Long id);

    MetricasTicketsResponse metricas();
}
