package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.ProductoRequest;
import com.fexsolutions.barriomarket.dto.ProductoResponse;
import com.fexsolutions.barriomarket.entity.Producto;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;
import com.fexsolutions.barriomarket.repository.CategoriaRepository;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    private static final Integer USUARIO = 5;

    @Mock ProductoRepository productos;
    @Mock CategoriaRepository categorias;
    private ProductoService service;

    @BeforeEach
    void preparar() {
        service = new ProductoService(productos, categorias, new SemaforoService());
    }

    private ProductoRequest req(String stock, String unidad) {
        return new ProductoRequest("Arroz", 1, new BigDecimal("4.50"),
                new BigDecimal(stock), unidad, null);
    }

    private Producto existente(String stock) {
        Producto p = new Producto();
        p.setId(10);
        p.setIdUsuario(USUARIO);
        p.setNombre("Arroz");
        p.setIdCategoria(1);
        p.setPrecio(new BigDecimal("4.50"));
        p.setStock(new BigDecimal(stock));
        p.setUnidadMedida("kg");
        return p;
    }

    @Test
    void crearSinStockQuedaRojo() {
        when(categorias.existsByIdAndIdUsuario(1, USUARIO)).thenReturn(true);
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductoResponse r = service.crear(USUARIO, req("0", "kg"));

        assertThat(r.estado()).isEqualTo("rojo");
    }

    @Test
    void crearConStockBajoQuedaAmarilloYGuardaAlUsuarioDelToken() {
        when(categorias.existsByIdAndIdUsuario(1, USUARIO)).thenReturn(true);
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductoResponse r = service.crear(USUARIO, req("2", "kg"));

        assertThat(r.estado()).isEqualTo("amarillo");
        verify(productos).save(org.mockito.ArgumentMatchers.argThat(p -> USUARIO.equals(p.getIdUsuario())));
    }

    @Test
    void crearConCategoriaDeOtroUsuarioFalla() {
        when(categorias.existsByIdAndIdUsuario(1, USUARIO)).thenReturn(false);

        assertThatThrownBy(() -> service.crear(USUARIO, req("5", "kg")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void crearConNombreRepetidoFalla() {
        when(categorias.existsByIdAndIdUsuario(1, USUARIO)).thenReturn(true);
        when(productos.existsByIdUsuarioAndNombreIgnoreCaseAndDeletedAtIsNull(USUARIO, "Arroz")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(USUARIO, req("5", "kg")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void productoPorUnidadNoAdmiteDecimales() {
        when(categorias.existsByIdAndIdUsuario(1, USUARIO)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(USUARIO, req("2.5", "unidad")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void productoPorKgAdmiteUnOctavo() {
        when(categorias.existsByIdAndIdUsuario(1, USUARIO)).thenReturn(true);
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductoResponse r = service.crear(USUARIO, req("0.125", "kg"));

        assertThat(r.stock()).isEqualByComparingTo("0.125");
        assertThat(r.estado()).isEqualTo("amarillo");
    }

    @Test
    void actualizarRecalculaElEstado() {
        when(productos.findByIdAndIdUsuarioAndDeletedAtIsNull(10, USUARIO)).thenReturn(Optional.of(existente("20")));
        when(categorias.existsByIdAndIdUsuario(1, USUARIO)).thenReturn(true);
        when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ProductoResponse r = service.actualizar(USUARIO, 10, req("0", "kg"));

        assertThat(r.estado()).isEqualTo("rojo");
    }

    @Test
    void eliminarMarcaDeletedAtSinBorrarLaFila() {
        Producto p = existente("5");
        when(productos.findByIdAndIdUsuarioAndDeletedAtIsNull(10, USUARIO)).thenReturn(Optional.of(p));

        service.eliminar(USUARIO, 10);

        assertThat(p.getDeletedAt()).isNotNull();
        verify(productos).save(p);
    }

    @Test
    void eliminarProductoAjenoDaNoEncontrado() {
        when(productos.findByIdAndIdUsuarioAndDeletedAtIsNull(10, USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(USUARIO, 10))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void listarPideSoloLosProductosDelUsuario() {
        when(productos.findByIdUsuarioAndDeletedAtIsNullOrderByNombre(USUARIO)).thenReturn(List.of(existente("5")));

        assertThat(service.listar(USUARIO)).hasSize(1);
    }
}