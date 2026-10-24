package com.fexsolutions.barriomarket.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemaforoServiceTest {

    private final SemaforoService semaforo = new SemaforoService();
    private static final BigDecimal MINIMO = new BigDecimal("3");

    @Test
    void stockCeroEsRojo() {
        assertThat(semaforo.calcular(BigDecimal.ZERO, MINIMO)).isEqualTo(EstadoStock.ROJO);
    }

    @Test
    void stockPorDebajoDelMinimoEsAmarillo() {
        assertThat(semaforo.calcular(new BigDecimal("2"), MINIMO)).isEqualTo(EstadoStock.AMARILLO);
    }

    @Test
    void stockIgualAlMinimoEsAmarillo() {
        assertThat(semaforo.calcular(new BigDecimal("3"), MINIMO)).isEqualTo(EstadoStock.AMARILLO);
    }

    @Test
    void stockPorEncimaDelMinimoEsVerde() {
        assertThat(semaforo.calcular(new BigDecimal("10"), MINIMO)).isEqualTo(EstadoStock.VERDE);
    }

    @Test
    void unOctavoDeKiloEsAmarilloNoRojo() {
        assertThat(semaforo.calcular(new BigDecimal("0.125"), MINIMO)).isEqualTo(EstadoStock.AMARILLO);
    }

    @Test
    void sinMinimoUsaTresPorDefecto() {
        assertThat(semaforo.calcular(new BigDecimal("3"), null)).isEqualTo(EstadoStock.AMARILLO);
        assertThat(semaforo.calcular(new BigDecimal("4"), null)).isEqualTo(EstadoStock.VERDE);
    }

    @Test
    void stockNegativoOSinValorEsInvalido() {
        assertThatThrownBy(() -> semaforo.calcular(new BigDecimal("-1"), MINIMO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> semaforo.calcular(null, MINIMO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}