package com.campuslands.tickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campuslands.tickets.ai.AnalisisTicketResponse;
import com.campuslands.tickets.ai.AnalizadorTicketService;
import com.campuslands.tickets.dto.CrearTicketRequest;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.entity.HistorialEstadoTicket;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.entity.Rol;
import com.campuslands.tickets.entity.Ticket;
import com.campuslands.tickets.entity.Usuario;
import com.campuslands.tickets.exception.OperacionNoPermitidaException;
import com.campuslands.tickets.exception.TecnicoInvalidoException;
import com.campuslands.tickets.exception.TicketNoEncontradoException;
import com.campuslands.tickets.exception.TransicionEstadoInvalidaException;
import com.campuslands.tickets.exception.UsuarioNoEncontradoException;
import com.campuslands.tickets.mapper.TicketMapper;
import com.campuslands.tickets.mapper.UsuarioMapper;
import com.campuslands.tickets.repository.HistorialEstadoTicketRepository;
import com.campuslands.tickets.repository.TicketRepository;
import com.campuslands.tickets.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final String TITULO = "Problema con internet";
    private static final String DESCRIPCION =
            "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.";

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private HistorialEstadoTicketRepository historialRepository;
    @Mock
    private AnalizadorTicketService analizador;
    @Mock
    private TransactionTemplate transacciones;

    private TicketServiceImpl servicio;

    @BeforeEach
    void configurar() {
        servicio = new TicketServiceImpl(ticketRepository, usuarioRepository, historialRepository, analizador,
                new TicketMapper(new UsuarioMapper()), transacciones);
    }

    @Test
    void crearTicket_guardaElTicketConElAnalisisDeLaIa() {
        Usuario solicitante = usuario(1L, "Laura Gómez", Rol.USUARIO);
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(usuarioRepository.getReferenceById(1L)).thenReturn(solicitante);
        when(analizador.analizar(TITULO, DESCRIPCION)).thenReturn(new AnalisisTicketResponse(
                Categoria.CONECTIVIDAD, Prioridad.ALTA, "Usuario sin conexión a internet antes de una reunión.",
                List.of("internet", "conectividad", "reunión"), OrigenAnalisis.MODELO_LENGUAJE));
        ejecutarTransaccionesDirectamente();
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocacion -> {
            Ticket ticket = invocacion.getArgument(0);
            ReflectionTestUtils.setField(ticket, "id", 25L);
            return ticket;
        });

        TicketResponse respuesta = servicio.crear(new CrearTicketRequest(TITULO, DESCRIPCION, 1L));

        assertThat(respuesta.id()).isEqualTo(25L);
        assertThat(respuesta.estado()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(respuesta.categoria()).isEqualTo(Categoria.CONECTIVIDAD);
        assertThat(respuesta.prioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(respuesta.resumenIA()).isEqualTo("Usuario sin conexión a internet antes de una reunión.");
        assertThat(respuesta.etiquetasIA()).containsExactly("internet", "conectividad", "reunión");
        assertThat(respuesta.solicitante().nombre()).isEqualTo("Laura Gómez");
        assertThat(respuesta.tecnicoAsignado()).isNull();

        ArgumentCaptor<HistorialEstadoTicket> historial = ArgumentCaptor.forClass(HistorialEstadoTicket.class);
        verify(historialRepository).save(historial.capture());
        assertThat(historial.getValue().getEstadoAnterior()).isNull();
        assertThat(historial.getValue().getEstadoNuevo()).isEqualTo(EstadoTicket.ABIERTO);
    }

    @Test
    void crearTicket_conUsuarioInexistente_noConsultaLaIaNiGuarda() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.crear(new CrearTicketRequest(TITULO, DESCRIPCION, 99L)))
                .isInstanceOf(UsuarioNoEncontradoException.class)
                .hasMessage("No se encontró el usuario con id 99");
        verifyNoInteractions(analizador, ticketRepository);
    }

    @Test
    void obtener_ticketInexistente_lanzaTicketNoEncontrado() {
        when(ticketRepository.findById(15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(15L))
                .isInstanceOf(TicketNoEncontradoException.class)
                .hasMessage("No se encontró el ticket con id 15");
    }

    @Test
    void asignarTecnico_conRolDistintoDeSoporte_lanzaTecnicoInvalido() {
        when(ticketRepository.findById(25L)).thenReturn(Optional.of(ticket(25L, EstadoTicket.ABIERTO)));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario(3L, "Andrea Ruiz", Rol.ADMIN)));

        assertThatThrownBy(() -> servicio.asignarTecnico(25L, 3L))
                .isInstanceOf(TecnicoInvalidoException.class)
                .hasMessageContaining("rol ADMIN");
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void asignarTecnico_inexistente_lanzaUsuarioNoEncontrado() {
        when(ticketRepository.findById(25L)).thenReturn(Optional.of(ticket(25L, EstadoTicket.ABIERTO)));
        when(usuarioRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignarTecnico(25L, 77L))
                .isInstanceOf(UsuarioNoEncontradoException.class)
                .hasMessage("No se encontró el técnico con id 77");
    }

    @Test
    void asignarTecnico_aUnTicketCerrado_lanzaOperacionNoPermitida() {
        when(ticketRepository.findById(25L)).thenReturn(Optional.of(ticket(25L, EstadoTicket.CERRADO)));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario(5L, "Sofía Ramírez", Rol.SOPORTE)));

        assertThatThrownBy(() -> servicio.asignarTecnico(25L, 5L))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void asignarTecnico_conRolSoporte_asignaYGuarda() {
        Ticket ticket = ticket(25L, EstadoTicket.ABIERTO);
        when(ticketRepository.findById(25L)).thenReturn(Optional.of(ticket));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario(5L, "Sofía Ramírez", Rol.SOPORTE)));

        TicketResponse respuesta = servicio.asignarTecnico(25L, 5L);

        assertThat(respuesta.tecnicoAsignado().id()).isEqualTo(5L);
        assertThat(respuesta.tecnicoAsignado().nombre()).isEqualTo("Sofía Ramírez");
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    void actualizarEstado_transicionPermitida_cambiaElEstadoYRegistraElHistorial() {
        Ticket ticket = ticket(25L, EstadoTicket.ABIERTO);
        when(ticketRepository.findById(25L)).thenReturn(Optional.of(ticket));

        TicketResponse respuesta = servicio.actualizarEstado(25L, EstadoTicket.EN_PROCESO);

        assertThat(respuesta.estado()).isEqualTo(EstadoTicket.EN_PROCESO);
        ArgumentCaptor<HistorialEstadoTicket> historial = ArgumentCaptor.forClass(HistorialEstadoTicket.class);
        verify(historialRepository).save(historial.capture());
        assertThat(historial.getValue().getEstadoAnterior()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(historial.getValue().getEstadoNuevo()).isEqualTo(EstadoTicket.EN_PROCESO);
    }

    @Test
    void actualizarEstado_transicionNoPermitida_lanzaExcepcionSinGuardar() {
        when(ticketRepository.findById(25L)).thenReturn(Optional.of(ticket(25L, EstadoTicket.CERRADO)));

        assertThatThrownBy(() -> servicio.actualizarEstado(25L, EstadoTicket.ABIERTO))
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessageContaining("CERRADO");
        verify(ticketRepository, never()).saveAndFlush(any());
        verifyNoInteractions(historialRepository);
    }

    @Test
    void eliminar_marcaElTicketComoEliminado() {
        Ticket ticket = ticket(25L, EstadoTicket.ABIERTO);
        when(ticketRepository.findById(25L)).thenReturn(Optional.of(ticket));

        servicio.eliminar(25L);

        assertThat(ticket.isEliminado()).isTrue();
        assertThat(ticket.getFechaEliminacion()).isNotNull();
        verify(ticketRepository).save(ticket);
    }

    private void ejecutarTransaccionesDirectamente() {
        when(transacciones.execute(any())).thenAnswer(invocacion ->
                invocacion.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    private static Usuario usuario(Long id, String nombre, Rol rol) {
        Usuario usuario = new Usuario(nombre, nombre.toLowerCase().replace(' ', '.') + "@empresa.com", "hash", rol);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private static Ticket ticket(Long id, EstadoTicket estado) {
        Ticket ticket = new Ticket(TITULO, DESCRIPCION, usuario(1L, "Laura Gómez", Rol.USUARIO));
        ticket.aplicarAnalisis(Categoria.CONECTIVIDAD, Prioridad.ALTA, "Sin internet.", List.of("internet"),
                OrigenAnalisis.REGLAS);
        ticket.cambiarEstado(estado);
        ReflectionTestUtils.setField(ticket, "id", id);
        return ticket;
    }
}
