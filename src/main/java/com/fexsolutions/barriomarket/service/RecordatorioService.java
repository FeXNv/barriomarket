package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.entity.Producto;
import com.fexsolutions.barriomarket.entity.Recordatorio;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;
import com.fexsolutions.barriomarket.repository.RecordatorioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fexsolutions.barriomarket.dto.RecordatorioResponse;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;

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

    @Transactional(readOnly = true)
    public List<RecordatorioResponse> pendientes(Integer idUsuario) {
        return recordatorios.pendientesDeUsuario(idUsuario).stream()
                .map(RecordatorioResponse::de).toList();
    }

    @Transactional
    public void marcarLeido(Integer idUsuario, Integer id) {
        Recordatorio r = recordatorios.buscarDeUsuario(id, idUsuario)
                .orElseThrow(() -> new NoEncontradoException("Recordatorio no encontrado"));
        r.setLeido(true);
        recordatorios.save(r);
    }   
}