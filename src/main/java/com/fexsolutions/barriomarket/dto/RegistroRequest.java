package com.fexsolutions.barriomarket.dto;

import jakarta.validation.constraints.*;

public record RegistroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Pattern(regexp = "^9\\d{8}$", message = "Teléfono de 9 dígitos que empiece con 9") String telefono,
        @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password) {}