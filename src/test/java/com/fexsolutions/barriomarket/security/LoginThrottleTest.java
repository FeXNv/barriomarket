package com.fexsolutions.barriomarket.security;

import com.google.common.base.Ticker;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LoginThrottleTest {

    private final AtomicLong nanos = new AtomicLong();
    private final Ticker reloj = new Ticker() {
        @Override public long read() { return nanos.get(); }
    };

    private LoginThrottle throttle() {
        return new LoginThrottle(3, Duration.ofMinutes(15), reloj);
    }

    @Test
    void noBloqueaAntesDelLimite() {
        LoginThrottle t = throttle();
        t.registrarFallo("987654321");
        t.registrarFallo("987654321");

        assertThat(t.bloqueado("987654321")).isFalse();
    }

    @Test
    void bloqueaAlLlegarAlLimite() {
        LoginThrottle t = throttle();
        for (int i = 0; i < 3; i++) t.registrarFallo("987654321");

        assertThat(t.bloqueado("987654321")).isTrue();
    }

    @Test
    void unaCuentaBloqueadaNoAfectaALasDemas() {
        LoginThrottle t = throttle();
        for (int i = 0; i < 3; i++) t.registrarFallo("987654321");

        assertThat(t.bloqueado("912345678")).isFalse();
    }

    @Test
    void limpiarDesbloqueaLaCuenta() {
        LoginThrottle t = throttle();
        for (int i = 0; i < 3; i++) t.registrarFallo("987654321");
        t.limpiar("987654321");

        assertThat(t.bloqueado("987654321")).isFalse();
    }

    @Test
    void elBloqueoExpiraConElTiempo() {
        LoginThrottle t = throttle();
        for (int i = 0; i < 3; i++) t.registrarFallo("987654321");
        nanos.addAndGet(Duration.ofMinutes(16).toNanos());

        assertThat(t.bloqueado("987654321")).isFalse();
    }
}