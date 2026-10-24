package com.fexsolutions.barriomarket.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRETO = "una-clave-de-prueba-de-al-menos-32-caracteres!!";

    @Test
    void tokenGeneradoDevuelveElMismoIdDeUsuario() {
        JwtService jwt = new JwtService(SECRETO, 60_000);
        String token = jwt.generar(7);
        assertThat(jwt.extraerIdUsuario(token)).isEqualTo(7);
    }

    @Test
    void tokenAlteradoEsRechazado() {
        JwtService jwt = new JwtService(SECRETO, 60_000);
        String token = jwt.generar(7);
        String alterado = token.substring(0, token.length() - 2) + "xx";
        assertThatThrownBy(() -> jwt.extraerIdUsuario(alterado)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenExpiradoEsRechazado() {
        JwtService jwt = new JwtService(SECRETO, -1_000);
        String token = jwt.generar(7);
        assertThatThrownBy(() -> jwt.extraerIdUsuario(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void secretoCortoNoEsPermitido() {
        assertThatThrownBy(() -> new JwtService("corto", 60_000))
                .isInstanceOf(IllegalStateException.class);
    }
}