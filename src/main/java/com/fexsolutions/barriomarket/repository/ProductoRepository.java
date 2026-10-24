package com.fexsolutions.barriomarket.repository;

import com.fexsolutions.barriomarket.entity.Producto;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findByIdUsuarioAndDeletedAtIsNullOrderByNombre(Integer idUsuario);
    Optional<Producto> findByIdAndIdUsuarioAndDeletedAtIsNull(Integer id, Integer idUsuario);
    boolean existsByIdUsuarioAndNombreIgnoreCaseAndDeletedAtIsNull(Integer idUsuario, String nombre);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id and p.idUsuario = :idUsuario and p.deletedAt is null")
    Optional<Producto> buscarParaActualizar(@Param("id") Integer id, @Param("idUsuario") Integer idUsuario);

    boolean existsByIdCategoria(Integer idCategoria);
}