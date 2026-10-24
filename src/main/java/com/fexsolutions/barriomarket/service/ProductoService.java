package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.ProductoRequest;
import com.fexsolutions.barriomarket.dto.ProductoResponse;
import com.fexsolutions.barriomarket.repository.CategoriaRepository;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final SemaforoService semaforo;

    public List<ProductoResponse> listar(Integer idUsuario) { throw new UnsupportedOperationException(); }
    public ProductoResponse crear(Integer idUsuario, ProductoRequest req) { throw new UnsupportedOperationException(); }
    public ProductoResponse actualizar(Integer idUsuario, Integer id, ProductoRequest req) { throw new UnsupportedOperationException(); }
    public void eliminar(Integer idUsuario, Integer id) { throw new UnsupportedOperationException(); }
}