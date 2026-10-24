package com.fexsolutions.barriomarket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(Integer id, LocalDateTime fecha, BigDecimal total,
                            List<DetalleVentaResponse> detalles) {}