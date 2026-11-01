package com.campuslands.tickets.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campuslands.tickets.dto.CrearTicketRequest;
import com.campuslands.tickets.dto.FiltroTickets;
import com.campuslands.tickets.dto.PaginaResponse;
import com.campuslands.tickets.dto.TicketResponse;
import com.campuslands.tickets.dto.UsuarioResumenResponse;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.entity.Rol;
import com.campuslands.tickets.exception.TecnicoInvalidoException;
import com.campuslands.tickets.exception.TicketNoEncontradoException;
import com.campuslands.tickets.exception.TransicionEstadoInvalidaException;
import com.campuslands.tickets.security.ConvertidorTokenJwt;
import com.campuslands.tickets.security.ManejadorErroresSeguridad;
import com.campuslands.tickets.security.SecurityConfig;
import com.campuslands.tickets.security.UsuarioAutenticado;
import com.campuslands.tickets.service.AuditoriaTicketService;
import com.campuslands.tickets.service.TicketService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, ManejadorErroresSeguridad.class})
class TicketControllerTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 7, 30, 19, 30, 0, 123_000_000);
    private static final UsuarioAutenticado LAURA = new UsuarioAutenticado(1L, Rol.USUARIO);
    private static final UsuarioAutenticado ANDREA = new UsuarioAutenticado(3L, Rol.ADMIN);
    private static final UsuarioAutenticado SOFIA = new UsuarioAutenticado(5L, Rol.SOPORTE);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private AuditoriaTicketService auditoriaService;

    @Test
    void crearTicket_responde201ConUbicacionYElAnalisisDeLaIa() throws Exception {
        when(ticketService.crear(any(CrearTicketRequest.class), any(UsuarioAutenticado.class)))
                .thenReturn(ticketEjemplo(EstadoTicket.ABIERTO));

        mockMvc.perform(post("/api/tickets").with(como(LAURA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Problema con internet",
                                  "descripcion": "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.",
                                  "usuarioId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/tickets/25")))
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.categoria").value("CONECTIVIDAD"))
                .andExpect(jsonPath("$.prioridad").value("ALTA"))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.resumenIA").value("Usuario sin conexión a internet antes de una reunión importante."))
                .andExpect(jsonPath("$.etiquetasIA", hasSize(3)))
                .andExpect(jsonPath("$.solicitante.nombre").value("Laura Gómez"))
                .andExpect(jsonPath("$.tecnicoAsignado").value(nullValue()))
                .andExpect(jsonPath("$.fechaCreacion").value("2026-07-30T19:30:00"));

        verify(ticketService).crear(any(CrearTicketRequest.class), eq(LAURA));
    }

    @Test
    void crearTicket_conDatosInvalidos_responde400ConElDetalleDeCadaCampo() throws Exception {
        mockMvc.perform(post("/api/tickets").with(como(LAURA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "abc", "descripcion": "muy corta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/tickets"))
                .andExpect(jsonPath("$.errores", hasSize(3)))
                .andExpect(jsonPath("$.errores[?(@.campo == 'titulo')].mensaje")
                        .value("El título debe tener entre 5 y 100 caracteres"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'usuarioId')].mensaje")
                        .value("El id del usuario solicitante es obligatorio"));
        verifyNoInteractions(ticketService);
    }

    @Test
    void crearTicket_conJsonMalFormado_responde400() throws Exception {
        mockMvc.perform(post("/api/tickets").with(como(LAURA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud no es un JSON válido"));
    }

    @Test
    void consultarTicketInexistente_responde404ConLaEstructuraDeError() throws Exception {
        when(ticketService.obtener(eq(15L), any(UsuarioAutenticado.class)))
                .thenThrow(new TicketNoEncontradoException(15L));

        mockMvc.perform(get("/api/tickets/15").with(como(ANDREA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No se encontró el ticket con id 15"))
                .andExpect(jsonPath("$.path").value("/api/tickets/15"))
                .andExpect(jsonPath("$.errores").doesNotExist());
    }

    @Test
    void consultarTicketConIdNoNumerico_responde400() throws Exception {
        mockMvc.perform(get("/api/tickets/abc").with(como(ANDREA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido 'abc' para el parámetro 'id'"));
    }

    @Test
    void consultarTickets_enviaLosFiltrosYElUsuarioAlServicio() throws Exception {
        when(ticketService.listar(any(FiltroTickets.class), any(Pageable.class), any(UsuarioAutenticado.class)))
                .thenReturn(new PaginaResponse<>(List.of(ticketEjemplo(EstadoTicket.ABIERTO)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/tickets").with(como(SOFIA)).param("estado", "ABIERTO").param("prioridad", "ALTA")
                        .param("usuarioId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.totalElementos").value(1));

        ArgumentCaptor<FiltroTickets> filtro = ArgumentCaptor.forClass(FiltroTickets.class);
        verify(ticketService).listar(filtro.capture(), any(Pageable.class), eq(SOFIA));
        assertThat(filtro.getValue()).isEqualTo(
                new FiltroTickets(EstadoTicket.ABIERTO, null, Prioridad.ALTA, 1L, null, null));
    }

    @Test
    void consultarTickets_conPrioridadInexistente_responde400ConLosValoresPermitidos() throws Exception {
        mockMvc.perform(get("/api/tickets").with(como(SOFIA)).param("prioridad", "URGENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Valor inválido 'URGENTE' para el parámetro 'prioridad'. Valores permitidos: BAJA, MEDIA, ALTA, CRITICA"));
    }

    @Test
    void actualizarEstado_responde200ConElNuevoEstado() throws Exception {
        when(ticketService.actualizarEstado(25L, EstadoTicket.EN_PROCESO, SOFIA))
                .thenReturn(ticketEjemplo(EstadoTicket.EN_PROCESO));

        mockMvc.perform(patch("/api/tickets/25/estado").with(como(SOFIA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"EN_PROCESO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));
    }

    @Test
    void actualizarEstado_conValorDesconocido_responde400() throws Exception {
        mockMvc.perform(patch("/api/tickets/25/estado").with(como(SOFIA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"PAUSADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Valor inválido 'PAUSADO' para el campo 'estado'. Valores permitidos: ABIERTO, EN_PROCESO, RESUELTO, CERRADO"));
        verifyNoInteractions(ticketService);
    }

    @Test
    void actualizarEstado_conTransicionNoPermitida_responde409() throws Exception {
        when(ticketService.actualizarEstado(25L, EstadoTicket.CERRADO, SOFIA))
                .thenThrow(new TransicionEstadoInvalidaException(EstadoTicket.ABIERTO, EstadoTicket.CERRADO));

        mockMvc.perform(patch("/api/tickets/25/estado").with(como(SOFIA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"CERRADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("de ABIERTO a CERRADO")));
    }

    @Test
    void asignarTecnicoSinRolSoporte_responde422() throws Exception {
        when(ticketService.asignarTecnico(25L, 3L, ANDREA)).thenThrow(new TecnicoInvalidoException(3L, Rol.ADMIN));

        mockMvc.perform(patch("/api/tickets/25/asignar").with(como(ANDREA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\": 3}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message", containsString("solo un usuario con rol SOPORTE")));
    }

    @Test
    void metodoNoSoportado_responde405ConLaMismaEstructuraDeError() throws Exception {
        mockMvc.perform(put("/api/tickets/25").with(como(ANDREA)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("El método PUT no está permitido en esta ruta"));
    }

    @Test
    void eliminarTicket_conRolAdmin_responde204() throws Exception {
        mockMvc.perform(delete("/api/tickets/25").with(como(ANDREA))).andExpect(status().isNoContent());

        verify(ticketService).eliminar(25L, ANDREA);
    }

    @Test
    void sinToken_responde401ConLaEstructuraDeError() throws Exception {
        mockMvc.perform(get("/api/tickets/15"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message", containsString("Authorization: Bearer")))
                .andExpect(jsonPath("$.path").value("/api/tickets/15"));
        verifyNoInteractions(ticketService);
    }

    @Test
    void conTokenInvalido_responde401() throws Exception {
        mockMvc.perform(get("/api/tickets/15").header("Authorization", "Bearer no.es.un-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("no es válido o ya expiró")));
        verifyNoInteractions(ticketService);
    }

    @Test
    void usuarioFinal_noPuedeCambiarEstadoNiEliminar_responde403() throws Exception {
        mockMvc.perform(patch("/api/tickets/25/estado").with(como(LAURA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"EN_PROCESO\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Su rol no tiene permiso para realizar esta operación"));

        mockMvc.perform(delete("/api/tickets/25").with(como(SOFIA))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/metricas").with(como(LAURA))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/25/auditoria").with(como(SOFIA))).andExpect(status().isForbidden());
        verifyNoInteractions(ticketService, auditoriaService);
    }

    private static RequestPostProcessor como(UsuarioAutenticado usuario) {
        return authentication(ConvertidorTokenJwt.autenticacion(usuario, null));
    }

    private static TicketResponse ticketEjemplo(EstadoTicket estado) {
        return new TicketResponse(25L, "Problema con internet",
                "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.",
                Categoria.CONECTIVIDAD, Prioridad.ALTA, estado,
                "Usuario sin conexión a internet antes de una reunión importante.",
                List.of("internet", "conectividad", "reunión"), OrigenAnalisis.MODELO_LENGUAJE,
                new UsuarioResumenResponse(1L, "Laura Gómez"), null, FECHA, FECHA);
    }
}
