package com.campuslands.tickets.exception;

import com.campuslands.tickets.entity.Rol;

public class TecnicoInvalidoException extends RuntimeException {

    public TecnicoInvalidoException(Long usuarioId, Rol rol) {
        super("El usuario con id %d tiene rol %s; solo un usuario con rol %s puede ser asignado como técnico"
                .formatted(usuarioId, rol, Rol.SOPORTE));
    }
}
