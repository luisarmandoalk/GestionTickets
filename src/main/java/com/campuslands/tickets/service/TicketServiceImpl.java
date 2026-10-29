package com.campuslands.tickets.service;

import com.campuslands.tickets.ai.AnalisisTicketResponse;
import com.campuslands.tickets.ai.AnalizadorTicketService;
import com.campuslands.tickets.dto.CrearTicketRequest;
import com.campuslands.tickets.dto.FiltroTickets;
import com.campuslands.tickets.dto.HistorialEstadoResponse;
import com.campuslands.tickets.dto.MetricasTicketsResponse;
import com.campuslands.tickets.dto.PaginaResponse;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.entity.HistorialEstadoTicket;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.entity.Ticket;
import com.campuslands.tickets.entity.Usuario;
import com.campuslands.tickets.exception.OperacionNoPermitidaException;
import com.campuslands.tickets.exception.TecnicoInvalidoException;
import com.campuslands.tickets.exception.TicketNoEncontradoException;
import com.campuslands.tickets.exception.TransicionEstadoInvalidaException;
import com.campuslands.tickets.exception.UsuarioNoEncontradoException;
import com.campuslands.tickets.mapper.TicketMapper;
import com.campuslands.tickets.repository.ConteoAgrupado;
import com.campuslands.tickets.repository.HistorialEstadoTicketRepository;
import com.campuslands.tickets.repository.TicketRepository;
import com.campuslands.tickets.repository.TicketSpecifications;
import com.campuslands.tickets.repository.UsuarioRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialEstadoTicketRepository historialRepository;
    private final AnalizadorTicketService analizador;
    private final TicketMapper ticketMapper;
    private final TransactionTemplate transacciones;

    public TicketServiceImpl(TicketRepository ticketRepository, UsuarioRepository usuarioRepository,
                             HistorialEstadoTicketRepository historialRepository, AnalizadorTicketService analizador,
                             TicketMapper ticketMapper, TransactionTemplate transacciones) {
        this.ticketRepository = ticketRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialRepository = historialRepository;
        this.analizador = analizador;
        this.ticketMapper = ticketMapper;
        this.transacciones = transacciones;
    }

    /**
     * El análisis se ejecuta antes de abrir la transacción: así la conexión a la base de datos no queda
     * retenida mientras responde el modelo, y nunca se guarda un ticket sin clasificación.
     */
    @Override
    public TicketResponse crear(CrearTicketRequest solicitud) {
        Long usuarioId = solicitud.usuarioId();
        if (!usuarioRepository.existsById(usuarioId)) {
            throw UsuarioNoEncontradoException.usuario(usuarioId);
        }
        AnalisisTicketResponse analisis = analizador.analizar(solicitud.titulo(), solicitud.descripcion());

        return transacciones.execute(estadoTransaccion -> {
            Usuario solicitante = usuarioRepository.getReferenceById(usuarioId);
            Ticket ticket = new Ticket(solicitud.titulo(), solicitud.descripcion(), solicitante);
            ticket.aplicarAnalisis(analisis.categoria(), analisis.prioridad(), analisis.resumen(),
                    analisis.etiquetas(), analisis.origen());
            Ticket guardado = ticketRepository.save(ticket);
            historialRepository.save(new HistorialEstadoTicket(guardado, null, guardado.getEstado()));
            return ticketMapper.aResponse(guardado);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<TicketResponse> listar(FiltroTickets filtro, Pageable paginacion) {
        return PaginaResponse.de(ticketRepository
                .findAll(TicketSpecifications.conFiltros(filtro), paginacion)
                .map(ticketMapper::aResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse obtener(Long id) {
        return ticketMapper.aResponse(buscarTicket(id));
    }

    @Override
    @Transactional
    public TicketResponse actualizarEstado(Long id, EstadoTicket nuevoEstado) {
        Ticket ticket = buscarTicket(id);
        EstadoTicket estadoActual = ticket.getEstado();
        if (!estadoActual.permiteTransicionA(nuevoEstado)) {
            throw new TransicionEstadoInvalidaException(estadoActual, nuevoEstado);
        }
        ticket.cambiarEstado(nuevoEstado);
        ticketRepository.saveAndFlush(ticket);
        historialRepository.save(new HistorialEstadoTicket(ticket, estadoActual, nuevoEstado));
        return ticketMapper.aResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse asignarTecnico(Long id, Long tecnicoId) {
        Ticket ticket = buscarTicket(id);
        Usuario tecnico = usuarioRepository.findById(tecnicoId)
                .orElseThrow(() -> UsuarioNoEncontradoException.tecnico(tecnicoId));
        if (!tecnico.esTecnicoDeSoporte()) {
            throw new TecnicoInvalidoException(tecnico.getId(), tecnico.getRol());
        }
        if (!ticket.getEstado().admiteAsignacionDeTecnico()) {
            throw new OperacionNoPermitidaException(
                    "No se puede asignar un técnico a un ticket en estado " + ticket.getEstado());
        }
        ticket.asignarTecnico(tecnico);
        ticketRepository.saveAndFlush(ticket);
        return ticketMapper.aResponse(ticket);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Ticket ticket = buscarTicket(id);
        ticket.marcarEliminado();
        ticketRepository.save(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoResponse> historial(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new TicketNoEncontradoException(id);
        }
        return historialRepository.findByTicketIdOrderByFechaCambioAscIdAsc(id).stream()
                .map(ticketMapper::aResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MetricasTicketsResponse metricas() {
        return new MetricasTicketsResponse(
                ticketRepository.count(),
                completarConteos(Categoria.class, ticketRepository.contarPorCategoria()),
                completarConteos(Prioridad.class, ticketRepository.contarPorPrioridad()),
                completarConteos(EstadoTicket.class, ticketRepository.contarPorEstado()));
    }

    private Ticket buscarTicket(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNoEncontradoException(id));
    }

    /** Devuelve todos los valores del enum, en su orden de declaración, con cero para los que no tienen tickets. */
    private static <E extends Enum<E>> Map<E, Long> completarConteos(Class<E> tipo, List<ConteoAgrupado<E>> conteos) {
        Map<E, Long> resultado = new EnumMap<>(tipo);
        for (E valor : tipo.getEnumConstants()) {
            resultado.put(valor, 0L);
        }
        conteos.forEach(conteo -> resultado.put(conteo.getValor(), conteo.getTotal()));
        return resultado;
    }
}
