package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.VentaRequest;
import com.fexsolutions.barriomarket.dto.VentaResponse;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import com.fexsolutions.barriomarket.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final ProductoRepository productos;
    private final VentaRepository ventas;
    private final StockService stockService;

    public VentaResponse registrar(Integer idUsuario, VentaRequest req) {
        throw new UnsupportedOperationException();
    }
}