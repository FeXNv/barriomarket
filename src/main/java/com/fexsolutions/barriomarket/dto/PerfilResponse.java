package com.fexsolutions.barriomarket.dto;

import com.fexsolutions.barriomarket.entity.Usuario;

public record PerfilResponse(String nombre, String telefono, String email) {
    public static PerfilResponse de(Usuario u) {
        return new PerfilResponse(u.getNombre(), u.getTelefono(), u.getEmail());
    }
}