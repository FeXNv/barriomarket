package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.CategoriaResponse;
import com.fexsolutions.barriomarket.entity.Categoria;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;
import com.fexsolutions.barriomarket.repository.CategoriaRepository;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(Integer idUsuario) {
        return categorias.findByIdUsuarioOrderByNombre(idUsuario).stream()
                .map(CategoriaResponse::de).toList();
    }

    @Transactional
    public CategoriaResponse crear(Integer idUsuario, String nombre) {
        String limpio = StringUtils.trimToNull(nombre);
        if (limpio == null) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (categorias.existsByIdUsuarioAndNombreIgnoreCase(idUsuario, limpio)) {
            throw new IllegalStateException("Ya tienes una categoría con ese nombre");
        }
        Categoria c = new Categoria();
        c.setNombre(limpio);
        c.setIdUsuario(idUsuario);
        Categoria guardada = categorias.save(c);
        log.info("Categoría creada id={} usuario={}", guardada.getId(), idUsuario);
        return CategoriaResponse.de(guardada);
    }

    @Transactional
    public void eliminar(Integer idUsuario, Integer id) {
        Categoria c = categorias.findByIdAndIdUsuario(id, idUsuario)
                .orElseThrow(() -> new NoEncontradoException("Categoría no encontrada"));
        if (productos.existsByIdCategoria(id)) {
            throw new IllegalStateException("No se puede eliminar una categoría que tiene productos");
        }
        categorias.delete(c);
        log.info("Categoría eliminada id={} usuario={}", id, idUsuario);
    }
}