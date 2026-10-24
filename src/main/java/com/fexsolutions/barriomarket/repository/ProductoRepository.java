package com.fexsolutions.barriomarket.repository;

import com.fexsolutions.barriomarket.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findByIdUsuarioAndDeletedAtIsNullOrderByNombre(Integer idUsuario);
    Optional<Producto> findByIdAndIdUsuarioAndDeletedAtIsNull(Integer id, Integer idUsuario);
    boolean existsByIdUsuarioAndNombreIgnoreCaseAndDeletedAtIsNull(Integer idUsuario, String nombre);
}