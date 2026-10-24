package com.fexsolutions.barriomarket.repository;

import com.fexsolutions.barriomarket.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    List<Categoria> findByIdUsuarioOrderByNombre(Integer idUsuario);
    boolean existsByIdAndIdUsuario(Integer id, Integer idUsuario);
}