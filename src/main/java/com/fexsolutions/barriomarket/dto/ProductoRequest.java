package com.fexsolutions.barriomarket.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull Integer idCategoria,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal precio,
        @NotNull @DecimalMin("0") @Digits(integer = 7, fraction = 3) BigDecimal stock,
        @NotBlank @Pattern(regexp = "unidad|kg|litro", message = "Debe ser unidad, kg o litro") String unidadMedida,
        @DecimalMin("0") @Digits(integer = 7, fraction = 3) BigDecimal stockMinimo) {}