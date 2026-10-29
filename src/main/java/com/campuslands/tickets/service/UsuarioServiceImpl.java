package com.campuslands.tickets.service;

import com.campuslands.tickets.dto.CrearUsuarioRequest;
import com.campuslands.tickets.dto.UsuarioResponse;
import com.campuslands.tickets.entity.Rol;
import com.campuslands.tickets.entity.Usuario;
import com.campuslands.tickets.exception.CorreoDuplicadoException;
import com.campuslands.tickets.exception.UsuarioNoEncontradoException;
import com.campuslands.tickets.mapper.UsuarioMapper;
import com.campuslands.tickets.repository.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper,
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest solicitud) {
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new CorreoDuplicadoException(correo);
        }
        Usuario usuario = new Usuario(solicitud.nombre().trim(), correo,
                passwordEncoder.encode(solicitud.password()), solicitud.rol());
        return usuarioMapper.aResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Rol rol) {
        List<Usuario> usuarios = rol == null
                ? usuarioRepository.findAllByOrderByNombreAsc()
                : usuarioRepository.findByRolOrderByNombreAsc(rol);
        return usuarios.stream().map(usuarioMapper::aResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return usuarioRepository.findById(id)
                .map(usuarioMapper::aResponse)
                .orElseThrow(() -> UsuarioNoEncontradoException.usuario(id));
    }
}
