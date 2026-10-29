package com.campuslands.tickets.ai;

import com.campuslands.tickets.config.IaProperties;
import com.campuslands.tickets.exception.AnalisisIaException;
import com.campuslands.tickets.exception.ProveedorIaNoDisponibleException;
import com.campuslands.tickets.exception.RespuestaIaInvalidaException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Analizador basado en un modelo de lenguaje, a través de la API {@code /chat/completions} compatible con
 * OpenAI. Reintenta los errores transitorios y valida la respuesta antes de devolverla.
 */
public class AnalizadorLlmService implements AnalizadorTicketService {

    private static final Logger log = LoggerFactory.getLogger(AnalizadorLlmService.class);
    private static final String RUTA_CHAT = "/chat/completions";
    private static final double TEMPERATURA = 0.0;

    private final RestClient clienteHttp;
    private final IaProperties propiedades;
    private final JsonMapper jsonMapper;
    private final PromptAnalisisTicket prompt;
    private final ValidadorAnalisisIa validador;

    public AnalizadorLlmService(RestClient clienteHttp, IaProperties propiedades, JsonMapper jsonMapper,
                                PromptAnalisisTicket prompt, ValidadorAnalisisIa validador) {
        this.clienteHttp = clienteHttp;
        this.propiedades = propiedades;
        this.jsonMapper = jsonMapper;
        this.prompt = prompt;
        this.validador = validador;
    }

    @Override
    public AnalisisTicketResponse analizar(String titulo, String descripcion) {
        int intentosMaximos = 1 + Math.max(0, propiedades.reintentos());
        for (int intento = 1; ; intento++) {
            try {
                return analizarUnaVez(titulo, descripcion);
            } catch (AnalisisIaException e) {
                if (!e.esReintentable() || intento >= intentosMaximos) {
                    throw e;
                }
                log.warn("Intento {} de {} fallido al consultar el modelo: {}", intento, intentosMaximos,
                        e.getMessage());
                esperarAntesDeReintentar(intento);
            }
        }
    }

    private AnalisisTicketResponse analizarUnaVez(String titulo, String descripcion) {
        byte[] cuerpo = consultarModelo(titulo, descripcion);
        String contenido = extraerContenido(cuerpo);
        return validador.validar(leerAnalisis(contenido));
    }

    private byte[] consultarModelo(String titulo, String descripcion) {
        ChatCompletion.Solicitud solicitud = new ChatCompletion.Solicitud(
                propiedades.modelo(),
                List.of(ChatCompletion.Mensaje.sistema(prompt.instrucciones()),
                        ChatCompletion.Mensaje.usuario(prompt.ticket(titulo, descripcion))),
                TEMPERATURA,
                propiedades.forzarRespuestaJson() ? ChatCompletion.FormatoRespuesta.OBJETO_JSON : null);
        try {
            return clienteHttp.post()
                    .uri(RUTA_CHAT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(jsonMapper.writeValueAsBytes(solicitud))
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientResponseException e) {
            int estado = e.getStatusCode().value();
            boolean reintentable = estado == 429 || e.getStatusCode().is5xxServerError();
            throw new ProveedorIaNoDisponibleException(
                    "El proveedor respondió con estado HTTP " + estado, reintentable, e);
        } catch (ResourceAccessException e) {
            throw new ProveedorIaNoDisponibleException(
                    "No fue posible conectarse con el proveedor (conexión rechazada o tiempo límite agotado)", true, e);
        } catch (RestClientException e) {
            throw new ProveedorIaNoDisponibleException("Error al invocar al proveedor: " + e.getMessage(), false, e);
        }
    }

    private String extraerContenido(byte[] cuerpo) {
        if (cuerpo == null || cuerpo.length == 0) {
            throw new RespuestaIaInvalidaException("el proveedor devolvió un cuerpo vacío");
        }
        try {
            ChatCompletion.Respuesta respuesta = jsonMapper.readValue(cuerpo, ChatCompletion.Respuesta.class);
            if (respuesta.choices() == null || respuesta.choices().isEmpty()
                    || respuesta.choices().getFirst().message() == null) {
                throw new RespuestaIaInvalidaException("la respuesta no contiene opciones");
            }
            String contenido = respuesta.choices().getFirst().message().content();
            if (contenido == null || contenido.isBlank()) {
                throw new RespuestaIaInvalidaException("el modelo devolvió un mensaje vacío");
            }
            return contenido;
        } catch (JacksonException e) {
            throw new RespuestaIaInvalidaException("la respuesta del proveedor no es JSON", e);
        }
    }

    /** Tolera texto alrededor del JSON (por ejemplo, bloques ```json) tomando el primer objeto completo. */
    private RespuestaModeloIa leerAnalisis(String contenido) {
        int inicio = contenido.indexOf('{');
        int fin = contenido.lastIndexOf('}');
        if (inicio < 0 || fin <= inicio) {
            throw new RespuestaIaInvalidaException("el contenido del modelo no incluye un objeto JSON");
        }
        try {
            return jsonMapper.readValue(contenido.substring(inicio, fin + 1), RespuestaModeloIa.class);
        } catch (JacksonException e) {
            throw new RespuestaIaInvalidaException("el JSON del modelo no tiene la estructura esperada", e);
        }
    }

    private void esperarAntesDeReintentar(int intento) {
        long espera = propiedades.esperaEntreReintentos().toMillis() * intento;
        if (espera <= 0) {
            return;
        }
        try {
            Thread.sleep(espera);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProveedorIaNoDisponibleException("Se interrumpió la espera entre reintentos", false, e);
        }
    }
}
