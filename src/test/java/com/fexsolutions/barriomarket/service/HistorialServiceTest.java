package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.ActualizacionStockResponse;
import com.fexsolutions.barriomarket.repository.HistorialStockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistorialServiceTest {

    @Mock HistorialStockRepository historial;
    @InjectMocks HistorialService servicio;

    private ActualizacionStockResponse fila(int id, String producto) {
        return new ActualizacionStockResponse(id, producto, new BigDecimal("5"), new BigDecimal("4"),
                "venta", LocalDateTime.now());
    }

    private int tamanoPedido(Integer limiteSolicitado) {
        when(historial.ultimasDelUsuario(eq(7), any(Pageable.class))).thenReturn(List.of());
        servicio.ultimas(7, limiteSolicitado);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(historial).ultimasDelUsuario(eq(7), captor.capture());
        return captor.getValue().getPageSize();
    }

    @Test
    void devuelveLosMovimientosDelUsuarioTalComoLosEntregaElRepositorio() {
        List<ActualizacionStockResponse> filas = List.of(fila(2, "Leche"), fila(1, "Arroz"));
        when(historial.ultimasDelUsuario(eq(7), any(Pageable.class))).thenReturn(filas);

        assertThat(servicio.ultimas(7, 5)).isEqualTo(filas);
    }

    @Test
    void sinLimiteUsaDiez() {
        assertThat(tamanoPedido(null)).isEqualTo(10);
    }

    @Test
    void limiteMuyGrandeSeRecortaACincuenta() {
        assertThat(tamanoPedido(500)).isEqualTo(50);
    }

    @Test
    void limiteCeroONegativoSeSubeAUno() {
        assertThat(tamanoPedido(0)).isEqualTo(1);
    }
}