package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.DetalleVentaResponse;
import com.fexsolutions.barriomarket.dto.ItemVentaRequest;
import com.fexsolutions.barriomarket.dto.VentaRequest;
import com.fexsolutions.barriomarket.dto.VentaResponse;
import com.fexsolutions.barriomarket.entity.DetalleVenta;
import com.fexsolutions.barriomarket.entity.Producto;
import com.fexsolutions.barriomarket.entity.Venta;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import com.fexsolutions.barriomarket.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentaService {

    private final ProductoRepository productos;
    private final VentaRepository ventas;
    private final StockService stockService;

    @Transactional
    public VentaResponse registrar(Integer idUsuario, VentaRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new IllegalArgumentException("La venta no tiene productos");
        }

        // TreeMap: junta productos repetidos y los deja ordenados por id (orden fijo de bloqueo)
        Map<Integer, BigDecimal> cantidades = new TreeMap<>();
        Map<Integer, BigDecimal> preciosIndicados = new HashMap();


        for (ItemVentaRequest item : req.items()) {
            if (item.cantidad() == null || item.cantidad().signum() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
            }
            cantidades.merge(item.idProducto(), item.cantidad(), BigDecimal::add);
            if (item.precioUnitario() != null) {
                BigDecimal previo = preciosIndicados.putIfAbsent(item.idProducto(), item.precioUnitario());
                if (previo != null && previo.compareTo(item.precioUnitario()) != 0) {
                    throw new IllegalArgumentException("El mismo producto aparece con precios distintos");
                }
            }
        }

        Venta venta = new Venta();
        venta.setIdUsuario(idUsuario);
        List<DetalleVentaResponse> detalles = new ArrayList<>();

        for (Map.Entry<Integer, BigDecimal> e : cantidades.entrySet()) {
            Producto p = productos.buscarParaActualizar(e.getKey(), idUsuario)
                    .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
            BigDecimal cantidad = e.getValue();
            BigDecimal precio = preciosIndicados.getOrDefault(p.getId(), p.getPrecio());

            if (p.getStock().compareTo(cantidad) < 0) {
                throw new IllegalStateException("Stock insuficiente de " + p.getNombre());
            }
            BigDecimal subtotal = precio.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            if (subtotal.signum() <= 0) {
                throw new IllegalArgumentException("La cantidad de " + p.getNombre() + " es demasiado pequeña");
            }

            DetalleVenta d = new DetalleVenta();
            d.setIdProducto(p.getId());
            d.setCantidad(cantidad);
            d.setPrecioUnitario(precio);
            d.setSubtotal(subtotal);
            venta.agregarDetalle(d);

            stockService.aplicarCambio(p, p.getStock().subtract(cantidad), "venta", idUsuario);
            detalles.add(new DetalleVentaResponse(p.getId(), p.getNombre(), cantidad, precio, subtotal));
        }

        Venta guardada = ventas.save(venta);
        log.info("Venta registrada id={} usuario={} total={}", guardada.getId(), idUsuario, guardada.getTotal());
        return new VentaResponse(guardada.getId(), guardada.getFecha(), guardada.getTotal(), detalles);
    }
}