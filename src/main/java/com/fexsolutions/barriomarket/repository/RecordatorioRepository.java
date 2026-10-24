package com.fexsolutions.barriomarket.repository;

import com.fexsolutions.barriomarket.entity.Recordatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecordatorioRepository extends JpaRepository<Recordatorio, Integer> {
    List<Recordatorio> findByIdProductoAndLeidoFalse(Integer idProducto);

    @Query("select r from Recordatorio r, Producto p where r.idProducto = p.id "
     + "and p.idUsuario = :idUsuario and r.leido = false order by r.fechaCreacion desc")
    List<Recordatorio> pendientesDeUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("select r from Recordatorio r, Producto p where r.idProducto = p.id "
     + "and r.id = :id and p.idUsuario = :idUsuario")
    Optional<Recordatorio> buscarDeUsuario(@Param("id") Integer id, @Param("idUsuario") Integer idUsuario);
    
}