package com.campuslands.tickets.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI documentacionApi() {
        return new OpenAPI().info(new Info()
                .title("Sistema Inteligente de Gestión de Tickets")
                .version("1.0.0")
                .description("API REST para registrar y gestionar solicitudes de soporte técnico. Cada ticket se "
                        + "clasifica automáticamente (categoría, prioridad, resumen y etiquetas) con un modelo de "
                        + "lenguaje o, si no está disponible, con un clasificador por palabras clave."));
    }
}
