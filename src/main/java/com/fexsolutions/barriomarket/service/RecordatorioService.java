package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.entity.Producto;
import com.fexsolutions.barriomarket.entity.Recordatorio;
import com.fexsolutions.barriomarket.repository.RecordatorioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordatorioService {

    private final RecordatorioRepository recordatorios;

    /** Deja un único recordatorio pendiente acorde al estado actual del producto. */
    public void sincronizar(Producto producto) {
        List<Recordatorio> pendientes = recordatorios.findByIdProductoAndLeidoFalse(producto.getId());

        if ("verde".equals(producto.getEstado())) {
            recordatorios.deleteAll(pendientes);
            return;
        }

        boolean agotado = "rojo".equals(producto.getEstado());
        String tipo = agotado ? "agotado" : "poco_stock";
        boolean yaExiste = pendientes.stream().anyMatch(r -> tipo.equals(r.getTipoAlerta()));
        if (yaExiste) {
            return;
        }

        recordatorios.deleteAll(pendientes);
        Recordatorio nuevo = new Recordatorio();
        nuevo.setIdProducto(producto.getId());
        nuevo.setTipoAlerta(tipo);
        nuevo.setMensaje(agotado
                ? "Se agotó " + producto.getNombre()
                : "Queda poco stock de " + producto.getNombre());
        recordatorios.save(nuevo);
    }
}