package com.fexsolutions.barriomarket.repository;

import com.fexsolutions.barriomarket.entity.Recordatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecordatorioRepository extends JpaRepository<Recordatorio, Integer> {
    List<Recordatorio> findByIdProductoAndLeidoFalse(Integer idProducto);
}