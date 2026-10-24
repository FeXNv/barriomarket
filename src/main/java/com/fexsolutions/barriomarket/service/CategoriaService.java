package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.CategoriaResponse;
import com.fexsolutions.barriomarket.repository.CategoriaRepository;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    public List<CategoriaResponse> listar(Integer idUsuario) { throw new UnsupportedOperationException(); }
    public CategoriaResponse crear(Integer idUsuario, String nombre) { throw new UnsupportedOperationException(); }
    public void eliminar(Integer idUsuario, Integer id) { throw new UnsupportedOperationException(); }
}