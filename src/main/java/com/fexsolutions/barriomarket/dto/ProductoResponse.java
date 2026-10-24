package com.fexsolutions.barriomarket.dto;

import com.fexsolutions.barriomarket.entity.Producto;
import java.math.BigDecimal;

public record ProductoResponse(Integer id, String nombre, Integer idCategoria, BigDecimal precio,
                               BigDecimal stock, String unidadMedida, String estado, BigDecimal stockMinimo) {

    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getIdCategoria(), p.getPrecio(),
                p.getStock(), p.getUnidadMedida(), p.getEstado(), p.getStockMinimo());
    }
}