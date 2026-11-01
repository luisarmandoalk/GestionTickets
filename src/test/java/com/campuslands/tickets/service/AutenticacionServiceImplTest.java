package com.campuslands.tickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.campuslands.tickets.dto.LoginRequest;
import com.campuslands.tickets.dto.TokenResponse;
import com.campuslands.tickets.entity.Rol;
import com.campuslands.tickets.entity.Usuario;
import com.campuslands.tickets.exception.CredencialesInvalidasException;
import com.campuslands.tickets.mapper.UsuarioMapper;
import com.campuslands.tickets.repository.UsuarioRepository;
import com.campuslands.tickets.security.ConvertidorTokenJwt;
import com.campuslands.tickets.security.GeneradorTokenJwt;
import com.campuslands.tickets.security.JwtProperties;
import com.campuslands.tickets.security.SecurityConfig;
import com.campuslands.tickets.security.UsuarioAutenticado;
import java.time.Duration;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceImplTest {

    private static final JwtProperties PROPIEDADES =
            new JwtProperties("una-clave-de-prueba-con-mas-de-32-caracteres", Duration.ofMinutes(30), "pruebas");

    @Mock
    private UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final SecurityConfig seguridad = new SecurityConfig();
    private final SecretKey clave = seguridad.claveFirmaJwt(PROPIEDADES);
    private final JwtDecoder decodificador = seguridad.jwtDecoder(clave, PROPIEDADES);
    private AutenticacionServiceImpl servicio;

    @BeforeEach
    void configurar() {
        GeneradorTokenJwt generador = new GeneradorTokenJwt(seguridad.jwtEncoder(clave), PROPIEDADES);
        servicio = new AutenticacionServiceImpl(usuarioRepository, passwordEncoder, generador, new UsuarioMapper());
    }

    @Test
    void conCredencialesCorrectas_emiteUnTokenFirmadoConElIdYElRol() {
        when(usuarioRepository.findByCorreoIgnoreCase("sofia.ramirez@empresa.com"))
                .thenReturn(Optional.of(usuario(5L, "Sofía Ramírez", Rol.SOPORTE, "Demo1234*")));

        TokenResponse respuesta = servicio.iniciarSesion(new LoginRequest(" sofia.ramirez@empresa.com ", "Demo1234*"));

        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.expiraEn()).isEqualTo(1800);
        assertThat(respuesta.usuario().rol()).isEqualTo(Rol.SOPORTE);
        Jwt jwt = decodificador.decode(respuesta.token());
        assertThat(jwt.getSubject()).isEqualTo("5");
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("SOPORTE");
        assertThat(new ConvertidorTokenJwt().convert(jwt).getPrincipal())
                .isEqualTo(new UsuarioAutenticado(5L, Rol.SOPORTE));
    }

    @Test
    void conContrasenaIncorrecta_lanzaCredencialesInvalidas() {
        when(usuarioRepository.findByCorreoIgnoreCase("sofia.ramirez@empresa.com"))
                .thenReturn(Optional.of(usuario(5L, "Sofía Ramírez", Rol.SOPORTE, "Demo1234*")));

        assertThatThrownBy(() -> servicio.iniciarSesion(new LoginRequest("sofia.ramirez@empresa.com", "otra")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    void conCorreoInexistente_lanzaElMismoErrorQueConContrasenaIncorrecta() {
        when(usuarioRepository.findByCorreoIgnoreCase("nadie@empresa.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciarSesion(new LoginRequest("nadie@empresa.com", "Demo1234*")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    void rechazaUnSecretoJwtDemasiadoCorto() {
        JwtProperties corto = new JwtProperties("corto", Duration.ofMinutes(30), "pruebas");

        assertThatThrownBy(() -> seguridad.claveFirmaJwt(corto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    private Usuario usuario(Long id, String nombre, Rol rol, String password) {
        Usuario usuario = new Usuario(nombre, "correo@empresa.com", passwordEncoder.encode(password), rol);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
