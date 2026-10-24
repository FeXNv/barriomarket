package com.fexsolutions.barriomarket.repository;

import com.fexsolutions.barriomarket.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fexsolutions.barriomarket.dto.FilaVenta;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface VentaRepository extends JpaRepository<Venta, Integer> {

    @Query("select new com.fexsolutions.barriomarket.dto.FilaVenta("
     + "v.id, v.fecha, p.nombre, d.cantidad, d.precioUnitario, d.subtotal) "
     + "from Venta v join v.detalles d, Producto p "
     + "where d.idProducto = p.id and v.idUsuario = :idUsuario "
     + "and v.fecha >= :desde and v.fecha < :hasta "
     + "order by v.fecha desc, v.id desc")
     
    List<FilaVenta> filasReporte(@Param("idUsuario") Integer idUsuario,
                             @Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta);
}