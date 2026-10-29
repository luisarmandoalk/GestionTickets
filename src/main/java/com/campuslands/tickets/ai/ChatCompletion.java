package com.campuslands.tickets.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Contrato mínimo del endpoint {@code /chat/completions} compatible con OpenAI, que también exponen
 * Groq, OpenRouter, Ollama y otros proveedores.
 */
public final class ChatCompletion {

    private ChatCompletion() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Solicitud(
            String model,
            List<Mensaje> messages,
            double temperature,
            @JsonProperty("response_format") FormatoRespuesta formatoRespuesta
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Mensaje(String role, String content) {

        static Mensaje sistema(String contenido) {
            return new Mensaje("system", contenido);
        }

        static Mensaje usuario(String contenido) {
            return new Mensaje("user", contenido);
        }
    }

    public record FormatoRespuesta(String type) {

        static final FormatoRespuesta OBJETO_JSON = new FormatoRespuesta("json_object");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Respuesta(List<Opcion> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Opcion(Mensaje message) {
    }
}
