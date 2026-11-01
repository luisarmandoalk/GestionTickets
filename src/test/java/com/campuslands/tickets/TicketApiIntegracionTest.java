package com.campuslands.tickets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Recorre la API completa contra un PostgreSQL real (embebido, sin Docker): migraciones Flyway, validación del
 * esquema por Hibernate, inicio de sesión con JWT, permisos por rol, filtros, ciclo de vida del ticket, auditoría,
 * caché de métricas y eliminación lógica. Usa el clasificador por reglas para no depender de un proveedor de IA.
 */
@SpringBootTest(properties = "app.ia.modo=reglas")
@AutoConfigureMockMvc
class TicketApiIntegracionTest {

    private static final EmbeddedPostgres POSTGRES = iniciarPostgres();
    private static final String CONTRASENA_DEMO = "Demo1234*";
    private static final long LAURA_USUARIO = 1L;
    private static final long CARLOS_USUARIO = 2L;
    private static final long ANDREA_ADMIN = 3L;
    private static final long SOFIA_SOPORTE = 5L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    private RequestPostProcessor laura;
    private RequestPostProcessor carlos;
    private RequestPostProcessor andrea;
    private RequestPostProcessor sofia;

    @DynamicPropertySource
    static void configurarBaseDeDatos(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void detenerPostgres() throws IOException {
        POSTGRES.close();
    }

    @BeforeEach
    void iniciarSesiones() throws Exception {
        laura = conToken("laura.gomez@empresa.com");
        carlos = conToken("carlos.perez@empresa.com");
        andrea = conToken("andrea.ruiz@empresa.com");
        sofia = conToken("sofia.ramirez@empresa.com");
    }

    @Test
    void iniciaSesionYRechazaCredencialesInvalidasOSolicitudesSinToken() throws Exception {
        iniciarSesion("Sofia.Ramirez@Empresa.com", CONTRASENA_DEMO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEn").value(3600))
                .andExpect(jsonPath("$.usuario.rol").value("SOPORTE"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist());

        iniciarSesion("sofia.ramirez@empresa.com", "incorrecta")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    @Test
    void creaYClasificaUnTicketConLosDatosDelEnunciado() throws Exception {
        crearTicket(laura, LAURA_USUARIO, "Problema con internet",
                "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value("CONECTIVIDAD"))
                .andExpect(jsonPath("$.prioridad").value("ALTA"))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.etiquetasIA", hasItem("internet")))
                .andExpect(jsonPath("$.origenAnalisis").value("REGLAS"))
                .andExpect(jsonPath("$.solicitante.nombre").value("Laura Gómez"));
    }

    @Test
    void recorreElCicloDeVidaCompletoDeUnTicket() throws Exception {
        long id = idDe(crearTicket(laura, LAURA_USUARIO, "No enciende el portátil",
                "Mi portátil no enciende aunque está conectado al cargador desde anoche."));

        mockMvc.perform(patch("/api/tickets/{id}/asignar", id).with(sofia).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\": %d}".formatted(SOFIA_SOPORTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tecnicoAsignado.nombre").value("Sofía Ramírez"));

        for (String estado : new String[] {"EN_PROCESO", "RESUELTO", "CERRADO"}) {
            cambiarEstado(sofia, id, estado).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(estado));
        }
        cambiarEstado(sofia, id, "ABIERTO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("no admite más cambios")));

        mockMvc.perform(get("/api/tickets/{id}/historial", id).with(laura))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].estadoNuevo").value("ABIERTO"))
                .andExpect(jsonPath("$[3].estadoAnterior").value("RESUELTO"))
                .andExpect(jsonPath("$[3].estadoNuevo").value("CERRADO"));

        mockMvc.perform(get("/api/tickets").with(sofia).param("estado", "CERRADO").param("tecnicoId", "" + SOFIA_SOPORTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id", hasItem((int) id)));
    }

    @Test
    void rechazaLaAsignacionDeUnTecnicoInvalido() throws Exception {
        long id = idDe(crearTicket(laura, LAURA_USUARIO, "Impresora atascada",
                "La impresora del segundo piso se atasca con cada hoja que imprimo."));

        mockMvc.perform(patch("/api/tickets/{id}/asignar", id).with(andrea).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\": %d}".formatted(ANDREA_ADMIN)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message", containsString("rol ADMIN")));

        mockMvc.perform(patch("/api/tickets/{id}/asignar", id).with(andrea).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\": 9999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el técnico con id 9999"));
    }

    @Test
    void cadaRolSoloAccedeASusOperaciones() throws Exception {
        long id = idDe(crearTicket(laura, LAURA_USUARIO, "Monitor sin señal",
                "El monitor de mi puesto muestra el mensaje sin señal desde que llegué."));

        cambiarEstado(laura, id, "EN_PROCESO").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/metricas").with(laura)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/usuarios").with(laura)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/tickets/{id}", id).with(sofia))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Su rol no tiene permiso para realizar esta operación"));
        mockMvc.perform(post("/api/usuarios").with(sofia).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/{id}/auditoria", id).with(sofia)).andExpect(status().isForbidden());

        cambiarEstado(sofia, id, "EN_PROCESO").andExpect(status().isOk());
        mockMvc.perform(get("/api/usuarios").with(sofia).param("rol", "SOPORTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Sofía Ramírez")));
    }

    @Test
    void unUsuarioSoloVeYRegistraSusPropiosTickets() throws Exception {
        long deCarlos = idDe(crearTicket(carlos, CARLOS_USUARIO, "Outlook no sincroniza",
                "Outlook no descarga los correos nuevos desde ayer en la tarde."));

        mockMvc.perform(get("/api/tickets").with(laura).param("usuarioId", "" + CARLOS_USUARIO).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id", not(hasItem((int) deCarlos))))
                .andExpect(jsonPath("$.contenido[*].solicitante.id", not(hasItem((int) CARLOS_USUARIO))));
        mockMvc.perform(get("/api/tickets/{id}", deCarlos).with(laura))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Solo puede consultar los tickets que usted registró"));
        crearTicket(laura, CARLOS_USUARIO, "Ticket ajeno", "Intento registrar un ticket a nombre de otra persona.")
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tickets/{id}", deCarlos).with(carlos)).andExpect(status().isOk());
        mockMvc.perform(get("/api/tickets/{id}", deCarlos).with(sofia)).andExpect(status().isOk());
    }

    @Test
    void registraLaAuditoriaConElUsuarioDeCadaCambioAunqueElTicketSeElimine() throws Exception {
        long id = idDe(crearTicket(laura, LAURA_USUARIO, "Sin acceso a la VPN",
                "No puedo conectarme a la VPN desde la casa, dice credenciales inválidas."));
        mockMvc.perform(patch("/api/tickets/{id}/asignar", id).with(sofia).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\": %d}".formatted(SOFIA_SOPORTE)))
                .andExpect(status().isOk());
        cambiarEstado(sofia, id, "EN_PROCESO").andExpect(status().isOk());
        mockMvc.perform(delete("/api/tickets/{id}", id).with(andrea)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tickets/{id}/auditoria", id).with(andrea))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].accion").value("CREACION"))
                .andExpect(jsonPath("$[0].usuario.id").value(LAURA_USUARIO))
                .andExpect(jsonPath("$[1].accion").value("ASIGNACION"))
                .andExpect(jsonPath("$[1].detalle").value("Técnico asignado: Sofía Ramírez (id 5)"))
                .andExpect(jsonPath("$[1].usuario.nombre").value("Sofía Ramírez"))
                .andExpect(jsonPath("$[2].accion").value("CAMBIO_ESTADO"))
                .andExpect(jsonPath("$[2].detalle").value("Estado: ABIERTO → EN_PROCESO"))
                .andExpect(jsonPath("$[3].accion").value("ELIMINACION"))
                .andExpect(jsonPath("$[3].usuario.nombre").value("Andrea Ruiz"));

        mockMvc.perform(get("/api/tickets/{id}/auditoria", 9999).with(andrea)).andExpect(status().isNotFound());
    }

    @Test
    void laEliminacionLogicaOcultaElTicketDeTodasLasConsultas() throws Exception {
        long id = idDe(crearTicket(laura, LAURA_USUARIO, "Ticket para eliminar",
                "Este ticket se creó por error y debe eliminarse del sistema."));

        mockMvc.perform(delete("/api/tickets/{id}", id).with(andrea)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tickets/{id}", id).with(andrea))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el ticket con id " + id));
        mockMvc.perform(delete("/api/tickets/{id}", id).with(andrea)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tickets").with(andrea).param("size", "100"))
                .andExpect(jsonPath("$.contenido[*].id", not(hasItem((int) id))));
    }

    @Test
    void buscaPorTextoFiltraPorUsuarioYOrdena() throws Exception {
        long id = idDe(crearTicket(laura, LAURA_USUARIO, "Licencia de Excel vencida",
                "Al abrir Excel aparece un aviso de licencia vencida y no deja editar."));

        mockMvc.perform(get("/api/tickets").with(laura).param("texto", "licencia VENCIDA")
                        .param("usuarioId", "" + LAURA_USUARIO).param("sort", "prioridad,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(id))
                .andExpect(jsonPath("$.contenido[0].categoria").value("SOFTWARE"));

        mockMvc.perform(get("/api/tickets").with(laura).param("sort", "inexistente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("inexistente")));
    }

    @Test
    void creaUsuariosYValidaCorreoYDuplicados() throws Exception {
        String usuario = """
                {"nombre": "Pedro Gil", "correo": "Pedro.Gil@Empresa.com", "password": "Segura123*", "rol": "SOPORTE"}
                """;
        mockMvc.perform(post("/api/usuarios").with(andrea).contentType(MediaType.APPLICATION_JSON).content(usuario))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("pedro.gil@empresa.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(post("/api/usuarios").with(andrea).contentType(MediaType.APPLICATION_JSON).content(usuario))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/usuarios").with(andrea).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre": "Sin correo", "correo": "no-es-un-correo", "password": "Segura123*", "rol": "USUARIO"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("correo"));

        iniciarSesion("pedro.gil@empresa.com", "Segura123*")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("SOPORTE"));
    }

    @Test
    void creaUnTicketConUsuarioInexistenteResponde404() throws Exception {
        crearTicket(andrea, 4040, "Sin usuario", "Este ticket no tiene un solicitante válido.")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el usuario con id 4040"));
    }

    @Test
    void entregaMetricasYLasActualizaAlCrearUnTicket() throws Exception {
        long totalAntes = total(mockMvc.perform(get("/api/tickets/metricas").with(sofia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porCategoria.ACCESOS").isNumber())
                .andExpect(jsonPath("$.porCategoria.OTRO").isNumber())
                .andExpect(jsonPath("$.porPrioridad.CRITICA").isNumber())
                .andExpect(jsonPath("$.porEstado.ABIERTO").isNumber()));

        crearTicket(laura, LAURA_USUARIO, "Sin acceso al ERP", "Olvidé mi contraseña del ERP y la cuenta está bloqueada.")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets/metricas").with(sofia))
                .andExpect(jsonPath("$.total").value(totalAntes + 1));
    }

    private ResultActions iniciarSesion(String correo, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(new Credenciales(correo, password))));
    }

    private RequestPostProcessor conToken(String correo) throws Exception {
        String cuerpo = iniciarSesion(correo, CONTRASENA_DEMO)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = jsonMapper.readTree(cuerpo).get("token").asString();
        return solicitud -> {
            solicitud.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return solicitud;
        };
    }

    private ResultActions crearTicket(RequestPostProcessor sesion, long usuarioId, String titulo, String descripcion)
            throws Exception {
        String cuerpo = jsonMapper.writeValueAsString(new CuerpoTicket(titulo, descripcion, usuarioId));
        return mockMvc.perform(post("/api/tickets").with(sesion).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions cambiarEstado(RequestPostProcessor sesion, long id, String estado) throws Exception {
        return mockMvc.perform(patch("/api/tickets/{id}/estado", id).with(sesion).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"%s\"}".formatted(estado)));
    }

    private long idDe(ResultActions resultado) throws Exception {
        String cuerpo = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(cuerpo).get("id").asLong();
    }

    private long total(ResultActions resultado) throws Exception {
        return jsonMapper.readTree(resultado.andReturn().getResponse().getContentAsString()).get("total").asLong();
    }

    private static EmbeddedPostgres iniciarPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException("No fue posible iniciar PostgreSQL embebido", e);
        }
    }

    private record CuerpoTicket(String titulo, String descripcion, long usuarioId) {
    }

    private record Credenciales(String correo, String password) {
    }
}
