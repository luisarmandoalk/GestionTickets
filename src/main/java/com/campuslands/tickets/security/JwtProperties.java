package com.campuslands.tickets.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración de los tokens JWT (prefijo {@code app.jwt}); el secreto llega por la variable JWT_SECRET. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secreto,
        Duration expiracion,
        String emisor
) {

    public boolean tieneSecreto() {
        return secreto != null && !secreto.isBlank();
    }
}
