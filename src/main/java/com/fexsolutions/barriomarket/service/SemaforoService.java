package com.fexsolutions.barriomarket.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class SemaforoService {

    public static final BigDecimal MINIMO_POR_DEFECTO = new BigDecimal("3");

    public EstadoStock calcular(BigDecimal stock, BigDecimal stockMinimo) {
        return EstadoStock.VERDE; // TODO: aún sin implementar
    }
}