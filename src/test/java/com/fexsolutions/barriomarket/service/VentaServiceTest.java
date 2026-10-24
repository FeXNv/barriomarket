package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.ItemVentaRequest;
import com.fexsolutions.barriomarket.dto.VentaRequest;
import com.fexsolutions.barriomarket.dto.VentaResponse;
import com.fexsolutions.barriomarket.entity.Producto;
import com.fexsolutions.barriomarket.entity.Venta;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import com.fexsolutions.barriomarket.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    private static final Integer USUARIO = 5;

    @Mock ProductoRepository productos;
    @Mock VentaRepository ventas;
    @Mock StockService stockService;
    private VentaService service;

    @BeforeEach
    void preparar() {
        service = new VentaService(productos, ventas, stockService);
    }

    private Producto producto(int id, String nombre, String precio, String stock, String unidad) {
        Producto p = new Producto();
        p.setId(id);
        p.setIdUsuario(USUARIO);
        p.setNombre(nombre);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(new BigDecimal(stock));
        p.setUnidadMedida(unidad);
        return p;
    }

    private void encontrado(Producto p) {
        when(productos.buscarParaActualizar(p.getId(), USUARIO)).thenReturn(Optional.of(p));
    }

    private void ventaSeGuarda() {
        when(ventas.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ItemVentaRequest item(int idProducto, String cantidad) {
        return new ItemVentaRequest(idProducto, new BigDecimal(cantidad), null);
    }

    private ItemVentaRequest itemConPrecio(int idProducto, String cantidad, String precio) {
        return new ItemVentaRequest(idProducto, new BigDecimal(cantidad), new BigDecimal(precio));
    }

    @Test
    void totalSeCalculaConElPrecioDelServidor() {
        Producto arroz = producto(10, "Arroz", "4.50", "5", "kg");
        encontrado(arroz);
        ventaSeGuarda();

        VentaResponse r = service.registrar(USUARIO, new VentaRequest(List.of(item(10, "0.5"))));

        assertThat(r.total()).isEqualByComparingTo("2.25");
        assertThat(r.detalles()).hasSize(1);
        assertThat(r.detalles().get(0).precioUnitario()).isEqualByComparingTo("4.50");
        ArgumentCaptor<BigDecimal> nuevoStock = ArgumentCaptor.forClass(BigDecimal.class);
        verify(stockService).aplicarCambio(eq(arroz), nuevoStock.capture(), eq("venta"), eq(USUARIO));
        assertThat(nuevoStock.getValue()).isEqualByComparingTo("4.5");
    }

    @Test
    void unOctavoDeKiloSeCobraCorrectamente() {
        Producto azucar = producto(10, "Azúcar", "8.00", "5", "kg");
        encontrado(azucar);
        ventaSeGuarda();

        VentaResponse r = service.registrar(USUARIO, new VentaRequest(List.of(item(10, "0.125"))));

        assertThat(r.total()).isEqualByComparingTo("1.00");
    }

    @Test
    void ventaConVariosProductosSumaElTotal() {
        encontrado(producto(10, "Arroz", "4.50", "5", "kg"));
        encontrado(producto(20, "Leche", "2.00", "10", "unidad"));
        ventaSeGuarda();

        VentaResponse r = service.registrar(USUARIO,
                new VentaRequest(List.of(item(10, "2"), item(20, "3"))));

        assertThat(r.total()).isEqualByComparingTo("15.00");
        assertThat(r.detalles()).hasSize(2);
    }

    @Test
    void stockInsuficienteFallaYNoGuardaLaVenta() {
        encontrado(producto(10, "Arroz", "4.50", "1", "kg"));

        assertThatThrownBy(() -> service.registrar(USUARIO, new VentaRequest(List.of(item(10, "2")))))
                .isInstanceOf(IllegalStateException.class);
        verify(ventas, never()).save(any());
    }

    @Test
    void productoDeOtroUsuarioDaNoEncontrado() {
        when(productos.buscarParaActualizar(10, USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(USUARIO, new VentaRequest(List.of(item(10, "1")))))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void ventaSinProductosEsInvalida() {
        assertThatThrownBy(() -> service.registrar(USUARIO, new VentaRequest(List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cantidadCeroEsInvalida() {
        assertThatThrownBy(() -> service.registrar(USUARIO, new VentaRequest(List.of(item(10, "0")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cantidadTanPequenaQueElSubtotalSeaCeroEsInvalida() {
        encontrado(producto(10, "Fideo", "0.50", "5", "kg"));

        assertThatThrownBy(() -> service.registrar(USUARIO, new VentaRequest(List.of(item(10, "0.001")))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(ventas, never()).save(any());
    }

    @Test
    void elMismoProductoRepetidoSeUneEnUnSoloDetalle() {
        Producto arroz = producto(10, "Arroz", "4.50", "5", "kg");
        encontrado(arroz);
        ventaSeGuarda();

        VentaResponse r = service.registrar(USUARIO,
                new VentaRequest(List.of(item(10, "1"), item(10, "1"))));

        assertThat(r.detalles()).hasSize(1);
        assertThat(r.detalles().get(0).cantidad()).isEqualByComparingTo("2");
        verify(stockService, times(1)).aplicarCambio(eq(arroz), any(BigDecimal.class), eq("venta"), eq(USUARIO));
    }

    @Test
    void bloqueaLosProductosEnOrdenDeIdParaEvitarInterbloqueos() {
        encontrado(producto(10, "Arroz", "4.50", "5", "kg"));
        encontrado(producto(20, "Leche", "2.00", "10", "unidad"));
        ventaSeGuarda();

        service.registrar(USUARIO, new VentaRequest(List.of(item(20, "1"), item(10, "1"))));

        InOrder orden = inOrder(productos);
        orden.verify(productos).buscarParaActualizar(10, USUARIO);
        orden.verify(productos).buscarParaActualizar(20, USUARIO);
    }

    @Test
    void siElBodeguerroIndicaPrecioSeUsaEsePrecio() {
        Producto arroz = producto(10, "Arroz", "4.50", "5", "kg");
        encontrado(arroz);
        ventaSeGuarda();

        VentaResponse r = service.registrar(USUARIO, new VentaRequest(List.of(itemConPrecio(10, "2", "5.00"))));

        assertThat(r.total()).isEqualByComparingTo("10.00");
        assertThat(r.detalles().get(0).precioUnitario()).isEqualByComparingTo("5.00");
    }

    @Test
    void elPrecioIndicadoNoCambiaElPrecioDeLista() {
        Producto arroz = producto(10, "Arroz", "4.50", "5", "kg");
        encontrado(arroz);
        ventaSeGuarda();

        service.registrar(USUARIO, new VentaRequest(List.of(itemConPrecio(10, "1", "5.00"))));

        assertThat(arroz.getPrecio()).isEqualByComparingTo("4.50");
    }

    @Test
    void mismoProductoConPreciosDistintosEsInvalido() {
        assertThatThrownBy(() -> service.registrar(USUARIO, new VentaRequest(
                List.of(itemConPrecio(10, "1", "5.00"), itemConPrecio(10, "1", "6.00")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

}