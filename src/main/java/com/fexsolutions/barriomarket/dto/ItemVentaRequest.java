package com.fexsolutions.barriomarket.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ItemVentaRequest(
        @NotNull Integer idProducto,
        @NotNull @DecimalMin("0.001") @Digits(integer = 7, fraction = 3) BigDecimal cantidad) {}