package com.campuslands.tickets.mapper;

import com.campuslands.tickets.dto.UsuarioResponse;
import com.campuslands.tickets.dto.UsuarioResumenResponse;
import com.campuslands.tickets.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResponse aResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol(),
                usuario.getFechaCreacion());
    }

    public UsuarioResumenResponse aResumen(Usuario usuario) {
        return usuario == null ? null : new UsuarioResumenResponse(usuario.getId(), usuario.getNombre());
    }
}
