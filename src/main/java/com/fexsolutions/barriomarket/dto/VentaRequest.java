package com.fexsolutions.barriomarket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record VentaRequest(@NotEmpty @Valid List<ItemVentaRequest> items) {}