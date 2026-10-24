package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.AjusteStockRequest;
import com.fexsolutions.barriomarket.dto.ProductoResponse;
import com.fexsolutions.barriomarket.entity.HistorialStock;
import com.fexsolutions.barriomarket.entity.Producto;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;
import com.fexsolutions.barriomarket.repository.HistorialStockRepository;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final ProductoRepository productos;
    private final HistorialStockRepository historial;
    private final SemaforoService semaforo;
    private final RecordatorioService recordatorios;

    @Transactional
    public ProductoResponse ajustar(Integer idUsuario, Integer idProducto, AjusteStockRequest req) {
        Producto p = productos.buscarParaActualizar(idProducto, idUsuario)
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));

        BigDecimal actual = p.getStock();
        BigDecimal cantidad = req.cantidad();

        BigDecimal nuevo = switch (req.tipo()) {
            case "reposicion" -> {
                exigirPositiva(cantidad);
                yield actual.add(cantidad);
            }
            case "merma" -> {
                exigirPositiva(cantidad);
                if (actual.compareTo(cantidad) < 0) {
                    throw new IllegalStateException("Stock insuficiente");
                }
                yield actual.subtract(cantidad);
            }
            case "ajuste" -> cantidad;
            default -> throw new IllegalArgumentException("Tipo de movimiento no válido");
        };

        aplicarCambio(p, nuevo, req.tipo(), idUsuario);
        return ProductoResponse.de(p);
    }

    /** Único punto del sistema que cambia el stock (lo reutilizará el Modo Caja). */
    @Transactional
    public void aplicarCambio(Producto p, BigDecimal nuevoStock, String tipoMovimiento, Integer idUsuario) {
        boolean fraccion = nuevoStock.stripTrailingZeros().scale() > 0;
        if ("unidad".equals(p.getUnidadMedida()) && fraccion) {
            throw new IllegalArgumentException("Los productos por unidad solo admiten stock entero");
        }

        BigDecimal anterior = p.getStock();
        p.setStock(nuevoStock);
        p.setEstado(semaforo.calcular(nuevoStock, p.getStockMinimo()).getValor());
        productos.save(p);

        HistorialStock h = new HistorialStock();
        h.setIdProducto(p.getId());
        h.setIdUsuario(idUsuario);
        h.setStockAnterior(anterior);
        h.setStockNuevo(nuevoStock);
        h.setTipoMovimiento(tipoMovimiento);
        historial.save(h);

        recordatorios.sincronizar(p);
        log.info("Stock producto={} {} -> {} ({})", p.getId(), anterior, nuevoStock, tipoMovimiento);
    }

    private void exigirPositiva(BigDecimal cantidad) {
        if (cantidad.signum() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
    }
}