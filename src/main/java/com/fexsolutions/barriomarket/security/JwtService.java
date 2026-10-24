package com.fexsolutions.barriomarket.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${jwt.secret}") String secreto,
                      @Value("${jwt.expiracion-ms:36000000}") long expiracionMs) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.expiracionMs = expiracionMs;
    }

    public String generar(Integer idUsuario) {
        long ahora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(idUsuario))
                .issuedAt(new Date(ahora))
                .expiration(new Date(ahora + expiracionMs))
                .signWith(clave)
                .compact();
    }

    /** Lanza JwtException si el token es inválido, está alterado o expiró. */
    public Integer extraerIdUsuario(String token) {
        String subject = Jwts.parser().verifyWith(clave).build()
                .parseSignedClaims(token).getPayload().getSubject();
        return Integer.valueOf(subject);
    }
}