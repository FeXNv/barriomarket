package com.fexsolutions.barriomarket.controller;

import com.fexsolutions.barriomarket.dto.AjusteStockRequest;
import com.fexsolutions.barriomarket.dto.ProductoRequest;
import com.fexsolutions.barriomarket.dto.ProductoResponse;
import com.fexsolutions.barriomarket.service.ProductoService;
import com.fexsolutions.barriomarket.service.StockService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;
    private final StockService stockService;

    @GetMapping
    public List<ProductoResponse> listar(@AuthenticationPrincipal Integer idUsuario) {
        return service.listar(idUsuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@AuthenticationPrincipal Integer idUsuario,
                                  @Valid @RequestBody ProductoRequest req) {
        return service.crear(idUsuario, req);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@AuthenticationPrincipal Integer idUsuario,
                                       @PathVariable Integer id,
                                       @Valid @RequestBody ProductoRequest req) {
        return service.actualizar(idUsuario, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Integer idUsuario, @PathVariable Integer id) {
        service.eliminar(idUsuario, id);
    }

    @PatchMapping("/{id}/stock")
    public ProductoResponse ajustarStock(@AuthenticationPrincipal Integer idUsuario,
                                        @PathVariable Integer id,
                                        @Valid @RequestBody AjusteStockRequest req) {
        return stockService.ajustar(idUsuario, id, req);
    }
}