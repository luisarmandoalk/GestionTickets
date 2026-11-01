package com.campuslands.tickets.service;

import com.campuslands.tickets.dto.CrearTicketRequest;
import com.campuslands.tickets.dto.FiltroTickets;
import com.campuslands.tickets.dto.HistorialEstadoResponse;
import com.campuslands.tickets.dto.MetricasTicketsResponse;
import com.campuslands.tickets.dto.PaginaResponse;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.security.UsuarioAutenticado;
import java.util.List;
import org.springframework.data.domain.Pageable;

/** Cada operación recibe el usuario autenticado: decide qué tickets puede ver y queda en la auditoría. */
public interface TicketService {

    TicketResponse crear(CrearTicketRequest solicitud, UsuarioAutenticado usuario);

    PaginaResponse<TicketResponse> listar(FiltroTickets filtro, Pageable paginacion, UsuarioAutenticado usuario);

    TicketResponse obtener(Long id, UsuarioAutenticado usuario);

    TicketResponse actualizarEstado(Long id, EstadoTicket nuevoEstado, UsuarioAutenticado usuario);

    TicketResponse asignarTecnico(Long id, Long tecnicoId, UsuarioAutenticado usuario);

    void eliminar(Long id, UsuarioAutenticado usuario);

    List<HistorialEstadoResponse> historial(Long id, UsuarioAutenticado usuario);

    MetricasTicketsResponse metricas();
}
