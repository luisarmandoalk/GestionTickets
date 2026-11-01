package com.campuslands.tickets.mapper;

import com.campuslands.tickets.dto.AuditoriaTicketResponse;
import com.campuslands.tickets.dto.HistorialEstadoResponse;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.entity.AuditoriaTicket;
import com.campuslands.tickets.entity.HistorialEstadoTicket;
import com.campuslands.tickets.entity.Ticket;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    private final UsuarioMapper usuarioMapper;

    public TicketMapper(UsuarioMapper usuarioMapper) {
        this.usuarioMapper = usuarioMapper;
    }

    public TicketResponse aResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitulo(),
                ticket.getDescripcion(),
                ticket.getCategoria(),
                ticket.getPrioridad(),
                ticket.getEstado(),
                ticket.getResumenIA(),
                List.copyOf(ticket.getEtiquetasIA()),
                ticket.getOrigenAnalisis(),
                usuarioMapper.aResumen(ticket.getUsuarioSolicitante()),
                usuarioMapper.aResumen(ticket.getTecnicoAsignado()),
                ticket.getFechaCreacion(),
                ticket.getFechaActualizacion());
    }

    public HistorialEstadoResponse aResponse(HistorialEstadoTicket cambio) {
        return new HistorialEstadoResponse(cambio.getEstadoAnterior(), cambio.getEstadoNuevo(), cambio.getFechaCambio());
    }

    public AuditoriaTicketResponse aResponse(AuditoriaTicket registro) {
        return new AuditoriaTicketResponse(registro.getAccion(), registro.getDetalle(),
                usuarioMapper.aResumen(registro.getUsuario()), registro.getFecha());
    }
}
