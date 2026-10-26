package com.fexsolutions.barriomarket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActualizacionStockResponse(
        Integer id, String producto, BigDecimal stockAnterior, BigDecimal stockNuevo,
        String tipoMovimiento, LocalDateTime fechaHora) {
}