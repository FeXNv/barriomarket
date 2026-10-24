package com.fexsolutions.barriomarket.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class SemaforoService {

    public static final BigDecimal MINIMO_POR_DEFECTO = new BigDecimal("3");

    public EstadoStock calcular(BigDecimal stock, BigDecimal stockMinimo) {
    if (stock == null || stock.signum() < 0) {
        throw new IllegalArgumentException("El stock no puede ser nulo ni negativo");
    }
    BigDecimal minimo = (stockMinimo == null) ? MINIMO_POR_DEFECTO : stockMinimo;

    if (stock.signum() == 0) {
        return EstadoStock.ROJO;
    }
    if (stock.compareTo(minimo) <= 0) {
        return EstadoStock.AMARILLO;
    }
    return EstadoStock.VERDE;
}
}