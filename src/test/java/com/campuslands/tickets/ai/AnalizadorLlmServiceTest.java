package com.campuslands.tickets.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.campuslands.tickets.config.IaProperties;
import com.campuslands.tickets.config.ModoIa;
import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.OrigenAnalisis;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.exception.ProveedorIaNoDisponibleException;
import com.campuslands.tickets.exception.RespuestaIaInvalidaException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class AnalizadorLlmServiceTest {

    private static final String URL_CHAT = "http://ia.prueba/v1/chat/completions";
    private static final String TITULO = "Problema con internet";
    private static final String DESCRIPCION =
            "Desde esta mañana no puedo conectarme a internet y tengo una reunión importante en 20 minutos.";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private MockRestServiceServer servidorIa;
    private AnalizadorLlmService analizador;

    @BeforeEach
    void configurar() {
        RestClient.Builder constructor = RestClient.builder().baseUrl("http://ia.prueba/v1");
        servidorIa = MockRestServiceServer.bindTo(constructor).build();
        IaProperties propiedades = new IaProperties(ModoIa.LLM, false, "http://ia.prueba/v1", "clave-prueba",
                "modelo-prueba", Duration.ofSeconds(5), 1, Duration.ZERO, true);
        analizador = new AnalizadorLlmService(constructor.build(), propiedades, jsonMapper,
                new PromptAnalisisTicket(), new ValidadorAnalisisIa());
    }

    @Test
    void enviaUnPromptEstructuradoYConvierteLaRespuestaJsonDelModelo() {
        servidorIa.expect(once(), requestTo(URL_CHAT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("modelo-prueba"))
                .andExpect(jsonPath("$.temperature").value(0.0))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value(containsString("<ticket>")))
                .andExpect(content().string(containsString("reunión importante")))
                .andRespond(withSuccess(respuestaChat("""
                        {"categoria": "CONECTIVIDAD", "prioridad": "ALTA",
                         "resumen": "Usuario sin conexión a internet antes de una reunión importante.",
                         "etiquetas": ["internet", "conectividad", "reunión"]}
                        """), MediaType.APPLICATION_JSON));

        AnalisisTicketResponse analisis = analizador.analizar(TITULO, DESCRIPCION);

        assertThat(analisis.categoria()).isEqualTo(Categoria.CONECTIVIDAD);
        assertThat(analisis.prioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(analisis.resumen()).isEqualTo("Usuario sin conexión a internet antes de una reunión importante.");
        assertThat(analisis.etiquetas()).containsExactly("internet", "conectividad", "reunión");
        assertThat(analisis.origen()).isEqualTo(OrigenAnalisis.MODELO_LENGUAJE);
        servidorIa.verify();
    }

    @Test
    void aceptaElJsonDentroDeUnBloqueDeCodigo() {
        servidorIa.expect(once(), requestTo(URL_CHAT))
                .andRespond(withSuccess(respuestaChat("""
                        Claro, este es el análisis:
                        ```json
                        {"categoria": "accesos", "prioridad": "media", "resumen": "Usuario bloqueado.", "etiquetas": ["Acceso"]}
                        ```
                        """), MediaType.APPLICATION_JSON));

        AnalisisTicketResponse analisis = analizador.analizar(TITULO, DESCRIPCION);

        assertThat(analisis.categoria()).isEqualTo(Categoria.ACCESOS);
        assertThat(analisis.prioridad()).isEqualTo(Prioridad.MEDIA);
        assertThat(analisis.etiquetas()).containsExactly("acceso");
    }

    @Test
    void reintentaYRechazaUnaCategoriaQueNoExisteEnElCatalogo() {
        servidorIa.expect(times(2), requestTo(URL_CHAT))
                .andRespond(withSuccess(respuestaChat("""
                        {"categoria": "TELEFONIA", "prioridad": "ALTA", "resumen": "x", "etiquetas": ["x"]}
                        """), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> analizador.analizar(TITULO, DESCRIPCION))
                .isInstanceOf(RespuestaIaInvalidaException.class)
                .hasMessageContaining("TELEFONIA");
        servidorIa.verify();
    }

    @Test
    void rechazaUnContenidoQueNoEsJson() {
        servidorIa.expect(times(2), requestTo(URL_CHAT))
                .andRespond(withSuccess(respuestaChat("No puedo clasificar este ticket."), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> analizador.analizar(TITULO, DESCRIPCION))
                .isInstanceOf(RespuestaIaInvalidaException.class);
    }

    @Test
    void reintentaUnErrorTransitorioYDevuelveElSegundoResultado() {
        servidorIa.expect(once(), requestTo(URL_CHAT)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        servidorIa.expect(once(), requestTo(URL_CHAT))
                .andRespond(withSuccess(respuestaChat("""
                        {"categoria": "SOFTWARE", "prioridad": "BAJA", "resumen": "Consulta.", "etiquetas": ["office"]}
                        """), MediaType.APPLICATION_JSON));

        AnalisisTicketResponse analisis = analizador.analizar(TITULO, DESCRIPCION);

        assertThat(analisis.categoria()).isEqualTo(Categoria.SOFTWARE);
        servidorIa.verify();
    }

    @Test
    void reportaProveedorNoDisponibleCuandoPersisteElErrorDelServidor() {
        servidorIa.expect(times(2), requestTo(URL_CHAT)).andRespond(withServerError());

        assertThatThrownBy(() -> analizador.analizar(TITULO, DESCRIPCION))
                .isInstanceOf(ProveedorIaNoDisponibleException.class)
                .hasMessageContaining("500");
        servidorIa.verify();
    }

    @Test
    void noReintentaCuandoElProveedorRechazaLasCredenciales() {
        servidorIa.expect(once(), requestTo(URL_CHAT)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> analizador.analizar(TITULO, DESCRIPCION))
                .isInstanceOf(ProveedorIaNoDisponibleException.class)
                .hasMessageContaining("401");
        servidorIa.verify();
    }

    @Test
    void convierteElTiempoLimiteAgotadoEnProveedorNoDisponible() {
        servidorIa.expect(times(2), requestTo(URL_CHAT)).andRespond(withException(new SocketTimeoutException("timeout")));

        assertThatThrownBy(() -> analizador.analizar(TITULO, DESCRIPCION))
                .isInstanceOf(ProveedorIaNoDisponibleException.class)
                .hasMessageContaining("tiempo límite");
    }

    private String respuestaChat(String contenido) {
        return jsonMapper.writeValueAsString(Map.of("choices",
                List.of(Map.of("index", 0, "message", Map.of("role", "assistant", "content", contenido)))));
    }
}
