package com.campuslands.tickets.controller;

import com.campuslands.tickets.dto.LoginRequest;
import com.campuslands.tickets.dto.TokenResponse;
import com.campuslands.tickets.service.AutenticacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT")
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión",
            description = "Devuelve el token para la cabecera Authorization: Bearer <token>. "
                    + "Los usuarios de demostración usan la contraseña Demo1234*")
    public TokenResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
        return autenticacionService.iniciarSesion(solicitud);
    }
}
