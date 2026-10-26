package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.ProductoRequest;
import com.fexsolutions.barriomarket.dto.ProductoResponse;
import com.fexsolutions.barriomarket.entity.Producto;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;
import com.fexsolutions.barriomarket.repository.CategoriaRepository;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fexsolutions.barriomarket.service.RecordatorioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoService {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final SemaforoService semaforo;
    private final RecordatorioService recordatorios;


    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Integer idUsuario) {
        return productos.findByIdUsuarioAndDeletedAtIsNullOrderByNombre(idUsuario)
                .stream().map(ProductoResponse::de).toList();
    }

    @Transactional
    public ProductoResponse crear(Integer idUsuario, ProductoRequest req) {
        validar(idUsuario, req);
        String nombre = req.nombre().trim();
        if (productos.existsByIdUsuarioAndNombreIgnoreCaseAndDeletedAtIsNull(idUsuario, nombre)) {
            throw new IllegalStateException("Ya tienes un producto con ese nombre");
        }
        Producto p = new Producto();
        p.setIdUsuario(idUsuario);
        aplicar(p, req);
        Producto guardado = productos.save(p);
        recordatorios.sincronizar(guardado);
        log.info("Producto creado id={} usuario={}", guardado.getId(), idUsuario);
        return ProductoResponse.de(guardado);

    }

    @Transactional
    public ProductoResponse actualizar(Integer idUsuario, Integer id, ProductoRequest req) {
        Producto p = buscarPropio(idUsuario, id);
        validar(idUsuario, req);
        aplicar(p, req);
        Producto guardado = productos.save(p);
        log.info("Producto actualizado id={} usuario={}", id, idUsuario);
        return ProductoResponse.de(guardado);
    }

    @Transactional
    public void eliminar(Integer idUsuario, Integer id) {
        Producto p = buscarPropio(idUsuario, id);
        p.setDeletedAt(LocalDateTime.now());
        productos.save(p);
        log.info("Producto eliminado (lógico) id={} usuario={}", id, idUsuario);
    }

    private Producto buscarPropio(Integer idUsuario, Integer id) {
        return productos.findByIdAndIdUsuarioAndDeletedAtIsNull(id, idUsuario)
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
    }

    private void validar(Integer idUsuario, ProductoRequest req) {
        if (!categorias.existsByIdAndIdUsuario(req.idCategoria(), idUsuario)) {
            throw new IllegalArgumentException("La categoría no existe");
        }
        boolean esDecimal = req.stock().stripTrailingZeros().scale() > 0;
        if ("unidad".equals(req.unidadMedida()) && esDecimal) {
            throw new IllegalArgumentException("Los productos por unidad solo admiten stock entero");
        }
    }

    private void aplicar(Producto p, ProductoRequest req) {
        p.setNombre(req.nombre().trim());
        p.setIdCategoria(req.idCategoria());
        p.setPrecio(req.precio());
        p.setStock(req.stock());
        p.setUnidadMedida(req.unidadMedida());
        BigDecimal minimo = req.stockMinimo() != null ? req.stockMinimo() : SemaforoService.MINIMO_POR_DEFECTO;
        p.setStockMinimo(minimo);
        p.setEstado(semaforo.calcular(req.stock(), minimo).getValor());
    }
}