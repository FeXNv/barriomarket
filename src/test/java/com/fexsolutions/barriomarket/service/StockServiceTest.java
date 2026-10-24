package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.AjusteStockRequest;
import com.fexsolutions.barriomarket.dto.ProductoResponse;
import com.fexsolutions.barriomarket.entity.HistorialStock;
import com.fexsolutions.barriomarket.entity.Producto;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;
import com.fexsolutions.barriomarket.repository.HistorialStockRepository;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    private static final Integer USUARIO = 5;

    @Mock ProductoRepository productos;
    @Mock HistorialStockRepository historial;
    @Mock RecordatorioService recordatorios;
    private StockService service;
    private Producto producto;

    @BeforeEach
    void preparar() {
        service = new StockService(productos, historial, new SemaforoService(), recordatorios);
        producto = new Producto();
        producto.setId(10);
        producto.setIdUsuario(USUARIO);
        producto.setNombre("Arroz");
        producto.setIdCategoria(1);
        producto.setPrecio(new BigDecimal("4.50"));
        producto.setStock(new BigDecimal("5"));
        producto.setUnidadMedida("kg");
        producto.setStockMinimo(new BigDecimal("3"));
        producto.setEstado("verde");
    }

    private void productoEncontrado() {
        when(productos.buscarParaActualizar(10, USUARIO)).thenReturn(Optional.of(producto));
    }

    private AjusteStockRequest ajuste(String tipo, String cantidad) {
        return new AjusteStockRequest(tipo, new BigDecimal(cantidad));
    }

    @Test
    void reposicionSumaStockYRegistraHistorial() {
        productoEncontrado();
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductoResponse r = service.ajustar(USUARIO, 10, ajuste("reposicion", "2.5"));

        assertThat(r.stock()).isEqualByComparingTo("7.5");
        ArgumentCaptor<HistorialStock> cap = ArgumentCaptor.forClass(HistorialStock.class);
        verify(historial).save(cap.capture());
        HistorialStock h = cap.getValue();
        assertThat(h.getStockAnterior()).isEqualByComparingTo("5");
        assertThat(h.getStockNuevo()).isEqualByComparingTo("7.5");
        assertThat(h.getTipoMovimiento()).isEqualTo("reposicion");
        assertThat(h.getIdUsuario()).isEqualTo(USUARIO);
        assertThat(h.getIdProducto()).isEqualTo(10);
    }

    @Test
    void mermaRestaStock() {
        productoEncontrado();
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductoResponse r = service.ajustar(USUARIO, 10, ajuste("merma", "1"));

        assertThat(r.stock()).isEqualByComparingTo("4");
        assertThat(r.estado()).isEqualTo("verde");
    }

    @Test
    void mermaMayorAlStockFallaYNoEscribeHistorial() {
        productoEncontrado();

        assertThatThrownBy(() -> service.ajustar(USUARIO, 10, ajuste("merma", "6")))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(historial);
    }

    @Test
    void ajusteFijaElStockYRecalculaElEstado() {
        productoEncontrado();
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductoResponse r = service.ajustar(USUARIO, 10, ajuste("ajuste", "0"));

        assertThat(r.stock()).isEqualByComparingTo("0");
        assertThat(r.estado()).isEqualTo("rojo");
    }

    @Test
    void reposicionConCantidadCeroEsInvalida() {
        productoEncontrado();

        assertThatThrownBy(() -> service.ajustar(USUARIO, 10, ajuste("reposicion", "0")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tipoDesconocidoEsInvalido() {
        productoEncontrado();

        assertThatThrownBy(() -> service.ajustar(USUARIO, 10, ajuste("regalo", "1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void productoAjenoDaNoEncontrado() {
        when(productos.buscarParaActualizar(10, USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ajustar(USUARIO, 10, ajuste("reposicion", "1")))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void productoPorUnidadNoAdmiteFracciones() {
        producto.setUnidadMedida("unidad");
        productoEncontrado();

        assertThatThrownBy(() -> service.ajustar(USUARIO, 10, ajuste("reposicion", "0.5")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void despuesDelCambioSincronizaLosRecordatorios() {
        productoEncontrado();
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        service.ajustar(USUARIO, 10, ajuste("merma", "4"));

        verify(recordatorios).sincronizar(producto);
        assertThat(producto.getEstado()).isEqualTo("amarillo");
    }
}