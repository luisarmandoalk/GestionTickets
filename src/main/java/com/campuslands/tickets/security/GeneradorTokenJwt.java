package com.campuslands.tickets.security;

import com.campuslands.tickets.entity.Usuario;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Emite el token de acceso: el sujeto es el id del usuario y el rol viaja como claim. */
@Component
public class GeneradorTokenJwt {

    static final String CLAIM_ROL = "rol";
    static final String CLAIM_NOMBRE = "nombre";

    private final JwtEncoder codificador;
    private final JwtProperties propiedades;

    public GeneradorTokenJwt(JwtEncoder codificador, JwtProperties propiedades) {
        this.codificador = codificador;
        this.propiedades = propiedades;
    }

    public TokenJwt generar(Usuario usuario) {
        Instant emision = Instant.now();
        Instant expiracion = emision.plus(propiedades.expiracion());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.emisor())
                .subject(usuario.getId().toString())
                .issuedAt(emision)
                .expiresAt(expiracion)
                .claim(CLAIM_ROL, usuario.getRol().name())
                .claim(CLAIM_NOMBRE, usuario.getNombre())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new TokenJwt(valor, propiedades.expiracion().toSeconds());
    }

    public record TokenJwt(String valor, long segundosDeVigencia) {
    }
}
