package com.campuslands.tickets.service;

import com.campuslands.tickets.dto.AuditoriaTicketResponse;
import com.campuslands.tickets.entity.AccionAuditoria;
import com.campuslands.tickets.entity.AuditoriaTicket;
import com.campuslands.tickets.exception.TicketNoEncontradoException;
import com.campuslands.tickets.mapper.TicketMapper;
import com.campuslands.tickets.repository.AuditoriaTicketRepository;
import com.campuslands.tickets.repository.UsuarioRepository;
import com.campuslands.tickets.security.UsuarioAutenticado;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaTicketServiceImpl implements AuditoriaTicketService {

    private final AuditoriaTicketRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TicketMapper ticketMapper;

    public AuditoriaTicketServiceImpl(AuditoriaTicketRepository auditoriaRepository,
                                      UsuarioRepository usuarioRepository, TicketMapper ticketMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.ticketMapper = ticketMapper;
    }

    /** Se ejecuta dentro de la transacción del cambio: si el cambio se revierte, su registro también. */
    @Override
    @Transactional
    public void registrar(Long ticketId, AccionAuditoria accion, String detalle, UsuarioAutenticado responsable) {
        auditoriaRepository.save(new AuditoriaTicket(ticketId, accion, detalle,
                usuarioRepository.getReferenceById(responsable.id())));
    }

    /** Todo ticket tiene al menos el registro de creación; si no hay registros, el ticket nunca existió. */
    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaTicketResponse> consultar(Long ticketId) {
        List<AuditoriaTicket> registros = auditoriaRepository.findByTicketIdOrderByFechaAscIdAsc(ticketId);
        if (registros.isEmpty()) {
            throw new TicketNoEncontradoException(ticketId);
        }
        return registros.stream().map(ticketMapper::aResponse).toList();
    }
}
