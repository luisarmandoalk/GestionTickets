package com.campuslands.tickets.security;

import com.campuslands.tickets.entity.Rol;

/** Usuario que hace la solicitud, tomado del token JWT. */
public record UsuarioAutenticado(Long id, Rol rol) {

    /** ADMIN y SOPORTE gestionan todos los tickets; un USUARIO solo los suyos. */
    public boolean gestionaTickets() {
        return rol == Rol.ADMIN || rol == Rol.SOPORTE;
    }
}
