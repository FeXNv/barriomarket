package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.entity.Producto;
import com.fexsolutions.barriomarket.entity.Recordatorio;
import com.fexsolutions.barriomarket.repository.RecordatorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {

    @Mock RecordatorioRepository repo;
    private RecordatorioService service;

    @BeforeEach
    void preparar() {
        service = new RecordatorioService(repo);
    }

    private Producto producto(String estado) {
        Producto p = new Producto();
        p.setId(10);
        p.setNombre("Arroz");
        p.setEstado(estado);
        return p;
    }

    private Recordatorio pendiente(String tipo) {
        Recordatorio r = new Recordatorio();
        r.setIdProducto(10);
        r.setTipoAlerta(tipo);
        return r;
    }

    @Test
    void verdeBorraLosRecordatoriosPendientesYNoCreaOtro() {
        List<Recordatorio> pendientes = List.of(pendiente("poco_stock"));
        when(repo.findByIdProductoAndLeidoFalse(10)).thenReturn(pendientes);

        service.sincronizar(producto("verde"));

        verify(repo).deleteAll(pendientes);
        verify(repo, never()).save(any());
    }

    @Test
    void amarilloCreaAlertaDePocoStock() {
        when(repo.findByIdProductoAndLeidoFalse(10)).thenReturn(List.of());

        service.sincronizar(producto("amarillo"));

        verify(repo).save(argThat(r -> "poco_stock".equals(r.getTipoAlerta()) && r.getIdProducto() == 10));
    }

    @Test
    void rojoReemplazaLaAlertaDePocoStockPorAgotado() {
        List<Recordatorio> pendientes = List.of(pendiente("poco_stock"));
        when(repo.findByIdProductoAndLeidoFalse(10)).thenReturn(pendientes);

        service.sincronizar(producto("rojo"));

        verify(repo).deleteAll(pendientes);
        verify(repo).save(argThat(r -> "agotado".equals(r.getTipoAlerta())));
    }

    @Test
    void sinoYaExisteLaMismaAlertaNoSeDuplica() {
        when(repo.findByIdProductoAndLeidoFalse(10)).thenReturn(List.of(pendiente("poco_stock")));

        service.sincronizar(producto("amarillo"));

        verify(repo, never()).save(any());
    }
}