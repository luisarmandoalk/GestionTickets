package com.campuslands.tickets.controller;

import com.campuslands.tickets.dto.CrearUsuarioRequest;
import com.campuslands.tickets.dto.UsuarioResponse;
import com.campuslands.tickets.entity.Rol;
import com.campuslands.tickets.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Solicitantes, técnicos de soporte y administradores")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @Operation(summary = "Crear un usuario", description = "La contraseña se almacena cifrada con BCrypt")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest solicitud) {
        UsuarioResponse creado = usuarioService.crear(solicitud);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    @Operation(summary = "Consultar usuarios", description = "Filtro opcional por rol")
    public List<UsuarioResponse> listar(@RequestParam(required = false) Rol rol) {
        return usuarioService.listar(rol);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un usuario")
    public UsuarioResponse obtener(@Parameter(description = "Id del usuario", example = "1") @PathVariable Long id) {
        return usuarioService.obtener(id);
    }
}
