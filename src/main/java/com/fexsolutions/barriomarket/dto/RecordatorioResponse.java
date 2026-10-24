package com.fexsolutions.barriomarket.dto;

import com.fexsolutions.barriomarket.entity.Recordatorio;
import java.time.LocalDateTime;

public record RecordatorioResponse(Integer id, Integer idProducto, String tipoAlerta,
                                   String mensaje, LocalDateTime fechaCreacion) {
    public static RecordatorioResponse de(Recordatorio r) {
        return new RecordatorioResponse(r.getId(), r.getIdProducto(), r.getTipoAlerta(),
                r.getMensaje(), r.getFechaCreacion());
    }
}