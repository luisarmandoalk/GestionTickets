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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/**
 * Recorre la API completa contra un PostgreSQL real (embebido, sin Docker): migraciones Flyway,
 * validación del esquema por Hibernate, filtros, ciclo de vida del ticket y eliminación lógica.
 * Usa el clasificador por reglas para no depender de un proveedor externo de IA.
 */
@SpringBootTest(properties = "app.ia.modo=reglas")
@AutoConfigureMockMvc
class TicketApiIntegracionTest {

    private static final EmbeddedPostgres POSTGRES = iniciarPostgres();
    private static final long LAURA_USUARIO = 1L;
    private static final long ANDREA_ADMIN = 3L;
    private static final long SOFIA_SOPORTE = 5L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

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

    @Test
    void creaYClasificaUnTicketConLosDatosDelEnunciado() throws Exception {
        crearTicket("Problema con internet",
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
        long id = idDe(crearTicket("No enciende el portátil",
                "Mi portátil no enciende aunque está conectado al cargador desde anoche."));

        mockMvc.perform(patch("/api/tickets/{id}/asignar", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\": %d}".formatted(SOFIA_SOPORTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tecnicoAsignado.nombre").value("Sofía Ramírez"));

        for (String estado : new String[] {"EN_PROCESO", "RESUELTO", "CERRADO"}) {
            cambiarEstado(id, estado).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(estado));
        }
        cambiarEstado(id, "ABIERTO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("no admite más cambios")));

        mockMvc.perform(get("/api/tickets/{id}/historial", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].estadoNuevo").value("ABIERTO"))
                .andExpect(jsonPath("$[3].estadoAnterior").value("RESUELTO"))
                .andExpect(jsonPath("$[3].estadoNuevo").value("CERRADO"));

        mockMvc.perform(get("/api/tickets").param("estado", "CERRADO").param("tecnicoId", "" + SOFIA_SOPORTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id", hasItem((int) id)));
    }

    @Test
    void rechazaLaAsignacionDeUnTecnicoInvalido() throws Exception {
        long id = idDe(crearTicket("Impresora atascada",
                "La impresora del segundo piso se atasca con cada hoja que imprimo."));

        mockMvc.perform(patch("/api/tickets/{id}/asignar", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\": %d}".formatted(ANDREA_ADMIN)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message", containsString("rol ADMIN")));

        mockMvc.perform(patch("/api/tickets/{id}/asignar", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\": 9999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el técnico con id 9999"));
    }

    @Test
    void laEliminacionLogicaOcultaElTicketDeTodasLasConsultas() throws Exception {
        long id = idDe(crearTicket("Ticket para eliminar",
                "Este ticket se creó por error y debe eliminarse del sistema."));

        mockMvc.perform(delete("/api/tickets/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tickets/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el ticket con id " + id));
        mockMvc.perform(delete("/api/tickets/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tickets").param("size", "100"))
                .andExpect(jsonPath("$.contenido[*].id", not(hasItem((int) id))));
    }

    @Test
    void buscaPorTextoFiltraPorUsuarioYOrdena() throws Exception {
        long id = idDe(crearTicket("Licencia de Excel vencida",
                "Al abrir Excel aparece un aviso de licencia vencida y no deja editar."));

        mockMvc.perform(get("/api/tickets").param("texto", "licencia VENCIDA")
                        .param("usuarioId", "" + LAURA_USUARIO).param("sort", "prioridad,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(id))
                .andExpect(jsonPath("$.contenido[0].categoria").value("SOFTWARE"));

        mockMvc.perform(get("/api/tickets").param("sort", "inexistente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("inexistente")));
    }

    @Test
    void creaUsuariosYValidaCorreoYDuplicados() throws Exception {
        String usuario = """
                {"nombre": "Pedro Gil", "correo": "Pedro.Gil@Empresa.com", "password": "Segura123*", "rol": "SOPORTE"}
                """;
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(usuario))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("pedro.gil@empresa.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(usuario))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre": "Sin correo", "correo": "no-es-un-correo", "password": "Segura123*", "rol": "USUARIO"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("correo"));
    }

    @Test
    void creaUnTicketConUsuarioInexistenteResponde404() throws Exception {
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"titulo": "Sin usuario", "descripcion": "Este ticket no tiene un solicitante válido.", "usuarioId": 4040}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el usuario con id 4040"));
    }

    @Test
    void entregaMetricasPorCategoriaPrioridadYEstado() throws Exception {
        crearTicket("Sin acceso al ERP", "Olvidé mi contraseña del ERP y la cuenta está bloqueada.");

        mockMvc.perform(get("/api/tickets/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porCategoria.ACCESOS").isNumber())
                .andExpect(jsonPath("$.porCategoria.OTRO").isNumber())
                .andExpect(jsonPath("$.porPrioridad.CRITICA").isNumber())
                .andExpect(jsonPath("$.porEstado.ABIERTO").isNumber());
    }

    private ResultActions crearTicket(String titulo, String descripcion) throws Exception {
        String cuerpo = jsonMapper.writeValueAsString(new CuerpoTicket(titulo, descripcion, LAURA_USUARIO));
        return mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions cambiarEstado(long id, String estado) throws Exception {
        return mockMvc.perform(patch("/api/tickets/{id}/estado", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"%s\"}".formatted(estado)));
    }

    private long idDe(ResultActions resultado) throws Exception {
        String cuerpo = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(cuerpo).get("id").asLong();
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
}
