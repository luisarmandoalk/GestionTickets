package com.campuslands.tickets.config;

import com.campuslands.tickets.dto.FormatoFecha;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import java.time.LocalDateTime;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static {
        // Sin esto Swagger documenta las fechas como date-time ISO con zona, que no es lo que devuelve la API
        SpringDocUtils.getConfig().replaceWithSchema(LocalDateTime.class, new StringSchema()
                .description("Fecha y hora local, sin zona horaria (" + FormatoFecha.PATRON + ")")
                .example(FormatoFecha.EJEMPLO));
    }

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
