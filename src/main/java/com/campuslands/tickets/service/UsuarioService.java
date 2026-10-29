package com.campuslands.tickets.service;

import com.campuslands.tickets.dto.CrearUsuarioRequest;
import com.campuslands.tickets.dto.UsuarioResponse;
import com.campuslands.tickets.entity.Rol;
import java.util.List;

public interface UsuarioService {

    UsuarioResponse crear(CrearUsuarioRequest solicitud);

    List<UsuarioResponse> listar(Rol rol);

    UsuarioResponse obtener(Long id);
}
