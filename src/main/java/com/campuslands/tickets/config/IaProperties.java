package com.campuslands.tickets.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración del analizador de IA (prefijo {@code app.ia}); los valores llegan por variables de entorno. */
@ConfigurationProperties(prefix = "app.ia")
public record IaProperties(
        ModoIa modo,
        boolean respaldoConReglas,
        String baseUrl,
        String apiKey,
        String modelo,
        Duration timeout,
        int reintentos,
        Duration esperaEntreReintentos,
        boolean forzarRespuestaJson
) {

    public IaProperties {
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }

    public boolean tieneApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean usaModeloDeLenguaje() {
        return switch (modo) {
            case LLM -> true;
            case REGLAS -> false;
            case AUTO -> tieneApiKey();
        };
    }
}
