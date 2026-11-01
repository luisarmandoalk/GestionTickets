package com.campuslands.tickets.service;

import com.campuslands.tickets.dto.LoginRequest;
import com.campuslands.tickets.dto.TokenResponse;
import com.campuslands.tickets.entity.Usuario;
import com.campuslands.tickets.exception.CredencialesInvalidasException;
import com.campuslands.tickets.mapper.UsuarioMapper;
import com.campuslands.tickets.repository.UsuarioRepository;
import com.campuslands.tickets.security.GeneradorTokenJwt;
import com.campuslands.tickets.security.GeneradorTokenJwt.TokenJwt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeneradorTokenJwt generadorToken;
    private final UsuarioMapper usuarioMapper;

    public AutenticacionServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                                    GeneradorTokenJwt generadorToken, UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.generadorToken = generadorToken;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse iniciarSesion(LoginRequest solicitud) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(solicitud.correo().trim())
                .filter(encontrado -> passwordEncoder.matches(solicitud.password(), encontrado.getPassword()))
                .orElseThrow(CredencialesInvalidasException::new);
        TokenJwt token = generadorToken.generar(usuario);
        return new TokenResponse(token.valor(), "Bearer", token.segundosDeVigencia(), usuarioMapper.aResponse(usuario));
    }
}
