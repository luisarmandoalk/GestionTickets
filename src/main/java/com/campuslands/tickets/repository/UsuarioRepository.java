package com.campuslands.tickets.repository;

import com.campuslands.tickets.entity.Rol;
import com.campuslands.tickets.entity.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    List<Usuario> findByRolOrderByNombreAsc(Rol rol);

    List<Usuario> findAllByOrderByNombreAsc();
}
