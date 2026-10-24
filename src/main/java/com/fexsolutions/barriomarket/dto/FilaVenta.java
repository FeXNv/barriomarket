package com.fexsolutions.barriomarket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FilaVenta(Integer idVenta, LocalDateTime fecha, String producto,
                        BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}