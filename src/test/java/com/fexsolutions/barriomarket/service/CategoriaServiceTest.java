package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.CategoriaResponse;
import com.fexsolutions.barriomarket.entity.Categoria;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;
import com.fexsolutions.barriomarket.repository.CategoriaRepository;
import com.fexsolutions.barriomarket.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    private static final Integer USUARIO = 5;

    @Mock CategoriaRepository categorias;
    @Mock ProductoRepository productos;
    private CategoriaService service;

    @BeforeEach
    void preparar() {
        service = new CategoriaService(categorias, productos);
    }

    private Categoria categoria(int id, String nombre) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre(nombre);
        c.setIdUsuario(USUARIO);
        return c;
    }

    @Test
    void crearGuardaConElUsuarioDelTokenYElNombreRecortado() {
        when(categorias.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));

        CategoriaResponse r = service.crear(USUARIO, "  Snacks ");

        assertThat(r.nombre()).isEqualTo("Snacks");
        verify(categorias).save(argThat(c -> USUARIO.equals(c.getIdUsuario())));
    }

    @Test
    void crearUnaCategoriaRepetidaFalla() {
        when(categorias.existsByIdUsuarioAndNombreIgnoreCase(USUARIO, "Snacks")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(USUARIO, "Snacks"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void crearConNombreEnBlancoFalla() {
        assertThatThrownBy(() -> service.crear(USUARIO, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eliminarConProductosFalla() {
        Categoria c = categoria(3, "Granos");
        when(categorias.findByIdAndIdUsuario(3, USUARIO)).thenReturn(Optional.of(c));
        when(productos.existsByIdCategoria(3)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(USUARIO, 3))
                .isInstanceOf(IllegalStateException.class);
        verify(categorias, never()).delete(any());
    }

    @Test
    void eliminarSinProductosLaBorra() {
        Categoria c = categoria(3, "Granos");
        when(categorias.findByIdAndIdUsuario(3, USUARIO)).thenReturn(Optional.of(c));
        when(productos.existsByIdCategoria(3)).thenReturn(false);

        service.eliminar(USUARIO, 3);

        verify(categorias).delete(c);
    }

    @Test
    void eliminarUnaCategoriaAjenaDaNoEncontrado() {
        when(categorias.findByIdAndIdUsuario(3, USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(USUARIO, 3))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void listarDevuelveSoloLasCategoriasDelUsuario() {
        when(categorias.findByIdUsuarioOrderByNombre(USUARIO)).thenReturn(List.of(categoria(3, "Granos")));

        assertThat(service.listar(USUARIO)).hasSize(1);
    }
}