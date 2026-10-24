package com.fexsolutions.barriomarket.dto;

import com.fexsolutions.barriomarket.entity.Categoria;

public record CategoriaResponse(Integer id, String nombre) {
    public static CategoriaResponse de(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre());
    }
}