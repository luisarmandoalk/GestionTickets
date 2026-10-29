package com.campuslands.tickets.config;

import com.campuslands.tickets.ai.AnalizadorConRespaldo;
import com.campuslands.tickets.ai.AnalizadorLlmService;
import com.campuslands.tickets.ai.AnalizadorReglasService;
import com.campuslands.tickets.ai.AnalizadorTicketService;
import com.campuslands.tickets.ai.PromptAnalisisTicket;
import com.campuslands.tickets.ai.ValidadorAnalisisIa;
import java.net.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** Elige la implementación de {@link AnalizadorTicketService} según la configuración {@code app.ia}. */
@Configuration
@EnableConfigurationProperties(IaProperties.class)
public class IaConfig {

    private static final Logger log = LoggerFactory.getLogger(IaConfig.class);

    @Bean
    public AnalizadorTicketService analizadorTicketService(IaProperties propiedades, JsonMapper jsonMapper) {
        AnalizadorReglasService reglas = new AnalizadorReglasService();
        if (!propiedades.usaModeloDeLenguaje()) {
            log.info("Analizador de tickets: reglas y palabras clave (AI_MODE={}, API key configurada: {})",
                    propiedades.modo(), propiedades.tieneApiKey() ? "sí" : "no");
            return reglas;
        }
        if (propiedades.modelo() == null || propiedades.modelo().isBlank()) {
            throw new IllegalStateException("Para usar el modelo de lenguaje debe configurar la variable AI_MODEL");
        }
        AnalizadorLlmService modelo = new AnalizadorLlmService(crearClienteHttp(propiedades), propiedades,
                jsonMapper, new PromptAnalisisTicket(), new ValidadorAnalisisIa());
        log.info("Analizador de tickets: modelo '{}' en {} (respaldo con reglas: {})", propiedades.modelo(),
                propiedades.baseUrl(), propiedades.respaldoConReglas() ? "sí" : "no");
        return propiedades.respaldoConReglas() ? new AnalizadorConRespaldo(modelo, reglas) : modelo;
    }

    private static RestClient crearClienteHttp(IaProperties propiedades) {
        HttpClient clienteJdk = HttpClient.newBuilder().connectTimeout(propiedades.timeout()).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(clienteJdk);
        fabrica.setReadTimeout(propiedades.timeout());
        RestClient.Builder constructor = RestClient.builder()
                .baseUrl(propiedades.baseUrl())
                .requestFactory(fabrica);
        if (propiedades.tieneApiKey()) {
            constructor.defaultHeaders(cabeceras -> cabeceras.setBearerAuth(propiedades.apiKey()));
        }
        return constructor.build();
    }
}
