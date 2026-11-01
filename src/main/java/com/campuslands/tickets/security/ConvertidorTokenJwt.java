package com.campuslands.tickets.security;

import com.campuslands.tickets.entity.Rol;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Convierte un JWT ya validado en la autenticación de la aplicación: el principal es {@link UsuarioAutenticado}
 * (lo reciben los controladores con {@code @AuthenticationPrincipal}) y el rol es la autoridad {@code ROLE_<rol>}.
 */
public class ConvertidorTokenJwt implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(Long.valueOf(jwt.getSubject()),
                Rol.valueOf(jwt.getClaimAsString(GeneradorTokenJwt.CLAIM_ROL)));
        return autenticacion(usuario, jwt);
    }

    public static AbstractAuthenticationToken autenticacion(UsuarioAutenticado usuario, Object credenciales) {
        return UsernamePasswordAuthenticationToken.authenticated(usuario, credenciales,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
    }
}
