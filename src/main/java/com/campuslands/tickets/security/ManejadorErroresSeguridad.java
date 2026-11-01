package com.campuslands.tickets.security;

import com.campuslands.tickets.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Responde los 401 y 403 de Spring Security con la misma estructura de error del resto de la API. Estos errores
 * ocurren en los filtros, antes de llegar a los controladores, por eso no los atiende el {@code @RestControllerAdvice}.
 */
@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public ManejadorErroresSeguridad(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest solicitud, HttpServletResponse respuesta, AuthenticationException ex)
            throws IOException {
        String mensaje = ex instanceof OAuth2AuthenticationException
                ? "El token no es válido o ya expiró. Inicie sesión de nuevo en /api/auth/login"
                : "Debe iniciar sesión y enviar el token en la cabecera Authorization: Bearer <token>";
        respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escribir(respuesta, HttpStatus.UNAUTHORIZED, mensaje, solicitud);
    }

    @Override
    public void handle(HttpServletRequest solicitud, HttpServletResponse respuesta, AccessDeniedException ex)
            throws IOException {
        escribir(respuesta, HttpStatus.FORBIDDEN, "Su rol no tiene permiso para realizar esta operación", solicitud);
    }

    private void escribir(HttpServletResponse respuesta, HttpStatus estado, String mensaje,
                          HttpServletRequest solicitud) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(respuesta.getOutputStream(), ErrorResponse.de(estado, mensaje, solicitud.getRequestURI()));
    }
}
