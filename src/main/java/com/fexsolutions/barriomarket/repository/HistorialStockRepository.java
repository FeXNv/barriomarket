package com.fexsolutions.barriomarket.repository;

import com.fexsolutions.barriomarket.dto.ActualizacionStockResponse;
import com.fexsolutions.barriomarket.entity.HistorialStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface HistorialStockRepository extends JpaRepository<HistorialStock, Integer> {

    @Query("select new com.fexsolutions.barriomarket.dto.ActualizacionStockResponse("
     + "h.id, p.nombre, h.stockAnterior, h.stockNuevo, h.tipoMovimiento, h.fechaHora) "
     + "from HistorialStock h join Producto p on p.id = h.idProducto "
     + "where h.idUsuario = :idUsuario order by h.fechaHora desc, h.id desc")
    List<ActualizacionStockResponse> ultimasDelUsuario(@Param("idUsuario") Integer idUsuario, Pageable pageable);
}