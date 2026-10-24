package com.fexsolutions.barriomarket.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AjusteStockRequest(
        @NotBlank @Pattern(regexp = "reposicion|merma|ajuste", message = "Debe ser reposicion, merma o ajuste") String tipo,
        @NotNull @DecimalMin("0") @Digits(integer = 7, fraction = 3) BigDecimal cantidad) {}