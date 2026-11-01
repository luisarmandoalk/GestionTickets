package com.campuslands.tickets.service;

import com.campuslands.tickets.ai.AnalisisTicketResponse;
import com.campuslands.tickets.ai.AnalizadorTicketService;
import com.campuslands.tickets.config.CacheConfig;
import com.campuslands.tickets.dto.CrearTicketRequest;
import com.campuslands.tickets.dto.FiltroTickets;
import com.campuslands.tickets.dto.HistorialEstadoResponse;
import com.campuslands.tickets.dto.MetricasTicketsResponse;
import com.campuslands.tickets.dto.PaginaResponse;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.entity.AccionAuditoria;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.entity.HistorialEstadoTicket;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.entity.Ticket;
import com.campuslands.tickets.entity.Usuario;
import com.campuslands.tickets.exception.AccesoDenegadoException;
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
import com.campuslands.tickets.security.UsuarioAutenticado;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialEstadoTicketRepository historialRepository;
    private final AuditoriaTicketService auditoria;
    private final AnalizadorTicketService analizador;
    private final TicketMapper ticketMapper;
    private final TransactionTemplate transacciones;

    public TicketServiceImpl(TicketRepository ticketRepository, UsuarioRepository usuarioRepository,
                             HistorialEstadoTicketRepository historialRepository, AuditoriaTicketService auditoria,
                             AnalizadorTicketService analizador, TicketMapper ticketMapper,
                             TransactionTemplate transacciones) {
        this.ticketRepository = ticketRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialRepository = historialRepository;
        this.auditoria = auditoria;
        this.analizador = analizador;
        this.ticketMapper = ticketMapper;
        this.transacciones = transacciones;
    }

    /**
     * El análisis se ejecuta antes de abrir la transacción: así la conexión a la base de datos no queda
     * retenida mientras responde el modelo, y nunca se guarda un ticket sin clasificación.
     */
    @Override
    @CacheEvict(cacheNames = CacheConfig.METRICAS, allEntries = true)
    public TicketResponse crear(CrearTicketRequest solicitud, UsuarioAutenticado usuario) {
        Long usuarioId = solicitud.usuarioId();
        if (!usuario.gestionaTickets() && !usuario.id().equals(usuarioId)) {
            throw new AccesoDenegadoException("Un usuario con rol USUARIO solo puede registrar tickets a su nombre");
        }
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
            auditoria.registrar(guardado.getId(), AccionAuditoria.CREACION, "Ticket creado y clasificado como %s / %s (%s)"
                    .formatted(analisis.categoria(), analisis.prioridad(), analisis.origen()), usuario);
            return ticketMapper.aResponse(guardado);
        });
    }

    /** Un USUARIO solo ve sus tickets: el filtro de solicitante se reemplaza por el suyo. */
    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<TicketResponse> listar(FiltroTickets filtro, Pageable paginacion, UsuarioAutenticado usuario) {
        FiltroTickets filtroPermitido = usuario.gestionaTickets() ? filtro : filtro.soloDelSolicitante(usuario.id());
        return PaginaResponse.de(ticketRepository
                .findAll(TicketSpecifications.conFiltros(filtroPermitido), paginacion)
                .map(ticketMapper::aResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse obtener(Long id, UsuarioAutenticado usuario) {
        return ticketMapper.aResponse(buscarTicketVisible(id, usuario));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.METRICAS, allEntries = true)
    public TicketResponse actualizarEstado(Long id, EstadoTicket nuevoEstado, UsuarioAutenticado usuario) {
        Ticket ticket = buscarTicket(id);
        EstadoTicket estadoActual = ticket.getEstado();
        if (!estadoActual.permiteTransicionA(nuevoEstado)) {
            throw new TransicionEstadoInvalidaException(estadoActual, nuevoEstado);
        }
        ticket.cambiarEstado(nuevoEstado);
        ticketRepository.saveAndFlush(ticket);
        historialRepository.save(new HistorialEstadoTicket(ticket, estadoActual, nuevoEstado));
        auditoria.registrar(id, AccionAuditoria.CAMBIO_ESTADO, "Estado: %s → %s".formatted(estadoActual, nuevoEstado),
                usuario);
        return ticketMapper.aResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse asignarTecnico(Long id, Long tecnicoId, UsuarioAutenticado usuario) {
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
        Usuario anterior = ticket.getTecnicoAsignado();
        ticket.asignarTecnico(tecnico);
        ticketRepository.saveAndFlush(ticket);
        String detalle = anterior == null
                ? "Técnico asignado: " + describir(tecnico)
                : "Técnico: %s → %s".formatted(describir(anterior), describir(tecnico));
        auditoria.registrar(id, AccionAuditoria.ASIGNACION, detalle, usuario);
        return ticketMapper.aResponse(ticket);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.METRICAS, allEntries = true)
    public void eliminar(Long id, UsuarioAutenticado usuario) {
        Ticket ticket = buscarTicket(id);
        ticket.marcarEliminado();
        ticketRepository.save(ticket);
        auditoria.registrar(id, AccionAuditoria.ELIMINACION, "Ticket eliminado (eliminación lógica)", usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoResponse> historial(Long id, UsuarioAutenticado usuario) {
        buscarTicketVisible(id, usuario);
        return historialRepository.findByTicketIdOrderByFechaCambioAscIdAsc(id).stream()
                .map(ticketMapper::aResponse)
                .toList();
    }

    /** Se calcula una vez y queda en caché hasta que un ticket se crea, cambia de estado o se elimina. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.METRICAS)
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

    private Ticket buscarTicketVisible(Long id, UsuarioAutenticado usuario) {
        Ticket ticket = buscarTicket(id);
        if (!usuario.gestionaTickets() && !ticket.getUsuarioSolicitante().getId().equals(usuario.id())) {
            throw new AccesoDenegadoException("Solo puede consultar los tickets que usted registró");
        }
        return ticket;
    }

    private static String describir(Usuario usuario) {
        return "%s (id %d)".formatted(usuario.getNombre(), usuario.getId());
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
