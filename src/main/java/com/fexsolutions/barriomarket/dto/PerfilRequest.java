package com.fexsolutions.barriomarket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Email @Size(max = 100) String email) {}