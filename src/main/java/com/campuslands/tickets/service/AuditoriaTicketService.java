package com.campuslands.tickets.service;

import com.campuslands.tickets.dto.AuditoriaTicketResponse;
import com.campuslands.tickets.entity.AccionAuditoria;
import com.campuslands.tickets.security.UsuarioAutenticado;
import java.util.List;

public interface AuditoriaTicketService {

    void registrar(Long ticketId, AccionAuditoria accion, String detalle, UsuarioAutenticado responsable);

    List<AuditoriaTicketResponse> consultar(Long ticketId);
}
