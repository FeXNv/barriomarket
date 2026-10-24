package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.AjusteStockRequest;
import com.fexsolutions.barriomarket.dto.ProductoResponse;
import com.fexsolutions.barriomarket.entity.Producto;
import com.fexsolutions.barriomarket.repository.HistorialStockRepository;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductoRepository productos;
    private final HistorialStockRepository historial;
    private final SemaforoService semaforo;
    private final RecordatorioService recordatorios;

    public ProductoResponse ajustar(Integer idUsuario, Integer idProducto, AjusteStockRequest req) {
        throw new UnsupportedOperationException();
    }

    /** Único punto del sistema que cambia el stock (lo reutilizará el Modo Caja). */
    public void aplicarCambio(Producto p, BigDecimal nuevoStock, String tipoMovimiento, Integer idUsuario) {
        throw new UnsupportedOperationException();
    }
}