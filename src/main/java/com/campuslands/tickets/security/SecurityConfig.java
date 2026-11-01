package com.campuslands.tickets.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Autenticación sin estado con JWT firmados por la aplicación (HS256) y autorización por rol en cada ruta.
 * La regla "un USUARIO solo ve sus propios tickets" depende del ticket, por eso se aplica en el servicio.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final int BYTES_MINIMOS_HS256 = 32;
    private static final String ADMIN = "ADMIN";
    private static final String SOPORTE = "SOPORTE";

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ManejadorErroresSeguridad manejadorErrores)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").hasAnyRole(ADMIN, SOPORTE)
                        .requestMatchers(HttpMethod.GET, "/api/tickets/metricas").hasAnyRole(ADMIN, SOPORTE)
                        .requestMatchers(HttpMethod.GET, "/api/tickets/*/auditoria").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/estado", "/api/tickets/*/asignar")
                        .hasAnyRole(ADMIN, SOPORTE)
                        .requestMatchers(HttpMethod.DELETE, "/api/tickets/*").hasRole(ADMIN)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new ConvertidorTokenJwt()))
                        .authenticationEntryPoint(manejadorErrores)
                        .accessDeniedHandler(manejadorErrores))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(manejadorErrores)
                        .accessDeniedHandler(manejadorErrores))
                .build();
    }

    /** Sin JWT_SECRET se genera una clave aleatoria: sirve para probar, pero los tokens caducan al reiniciar. */
    @Bean
    public SecretKey claveFirmaJwt(JwtProperties propiedades) {
        byte[] secreto;
        if (propiedades.tieneSecreto()) {
            secreto = propiedades.secreto().getBytes(StandardCharsets.UTF_8);
            if (secreto.length < BYTES_MINIMOS_HS256) {
                throw new IllegalStateException(
                        "JWT_SECRET debe tener al menos %d caracteres para firmar con HS256".formatted(BYTES_MINIMOS_HS256));
            }
        } else {
            log.warn("JWT_SECRET no está configurado: se usa una clave aleatoria y los tokens dejan de servir al reiniciar");
            secreto = new byte[BYTES_MINIMOS_HS256];
            new SecureRandom().nextBytes(secreto);
        }
        return new SecretKeySpec(secreto, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey claveFirmaJwt) {
        return NimbusJwtEncoder.withSecretKey(claveFirmaJwt).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey claveFirmaJwt, JwtProperties propiedades) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveFirmaJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(propiedades.emisor()));
        return decodificador;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
