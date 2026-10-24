package com.fexsolutions.barriomarket.dto;

import java.math.BigDecimal;

public record DetalleVentaResponse(Integer idProducto, String nombre, BigDecimal cantidad,
                                   BigDecimal precioUnitario, BigDecimal subtotal) {}
                                   