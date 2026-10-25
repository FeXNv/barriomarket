package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.CambioPasswordRequest;
import com.fexsolutions.barriomarket.dto.PerfilRequest;
import com.fexsolutions.barriomarket.dto.PerfilResponse;
import com.fexsolutions.barriomarket.entity.Usuario;
import com.fexsolutions.barriomarket.exception.DemasiadosIntentosException;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;
import com.fexsolutions.barriomarket.repository.UsuarioRepository;
import com.fexsolutions.barriomarket.security.LoginThrottle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    private static final Integer ID = 1;

    @Mock UsuarioRepository usuarios;
    @Mock PasswordEncoder encoder;
    @Mock LoginThrottle throttle;
    private PerfilService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        service = new PerfilService(usuarios, encoder, throttle);
        usuario = new Usuario();
        usuario.setId(ID);
        usuario.setNombre("Fernando");
        usuario.setTelefono("987654321");
        usuario.setEmail("a@x.com");
        usuario.setPasswordHash("HASH");
    }

    private void usuarioExiste() {
        when(usuarios.findById(ID)).thenReturn(Optional.of(usuario));
    }

    private void guardadoDevuelveElMismo() {
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void obtenerDevuelveLosDatosDelUsuario() {
        usuarioExiste();

        PerfilResponse r = service.obtener(ID);

        assertThat(r.nombre()).isEqualTo("Fernando");
        assertThat(r.telefono()).isEqualTo("987654321");
        assertThat(r.email()).isEqualTo("a@x.com");
    }

    @Test
    void usuarioInexistenteDaNoEncontrado() {
        when(usuarios.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(ID)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void actualizarCambiaNombreYCorreo() {
        usuarioExiste();
        guardadoDevuelveElMismo();

        PerfilResponse r = service.actualizar(ID, new PerfilRequest("  Fernando José ", "nuevo@x.com"));

        assertThat(r.nombre()).isEqualTo("Fernando José");
        assertThat(r.email()).isEqualTo("nuevo@x.com");
    }

    @Test
    void actualizarConElCorreoDeOtroUsuarioFalla() {
        usuarioExiste();
        when(usuarios.existsByEmail("otro@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(ID, new PerfilRequest("Fernando", "otro@x.com")))
                .isInstanceOf(IllegalStateException.class);
        verify(usuarios, never()).save(any());
    }

    @Test
    void actualizarConservandoSuPropioCorreoNoLoTomaComoRepetido() {
        usuarioExiste();
        guardadoDevuelveElMismo();

        service.actualizar(ID, new PerfilRequest("Fernando", "a@x.com"));

        verify(usuarios, never()).existsByEmail(any());
    }

    @Test
    void actualizarConCorreoEnBlancoLoDejaVacio() {
        usuarioExiste();
        guardadoDevuelveElMismo();

        PerfilResponse r = service.actualizar(ID, new PerfilRequest("Fernando", "   "));

        assertThat(r.email()).isNull();
    }

    @Test
    void actualizarConNombreEnBlancoFalla() {
        usuarioExiste();

        assertThatThrownBy(() -> service.actualizar(ID, new PerfilRequest("   ", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cambiarPasswordGuardaElNuevoHashYLimpiaLosIntentos() {
        usuarioExiste();
        guardadoDevuelveElMismo();
        when(encoder.matches("vieja12345", "HASH")).thenReturn(true);
        when(encoder.encode("nueva12345")).thenReturn("NUEVO");

        service.cambiarPassword(ID, new CambioPasswordRequest("vieja12345", "nueva12345"));

        assertThat(usuario.getPasswordHash()).isEqualTo("NUEVO");
        verify(throttle).limpiar("pwd:1");
    }

    @Test
    void cambiarPasswordConActualIncorrectaFallaYRegistraElFallo() {
        usuarioExiste();
        when(encoder.matches("mala", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.cambiarPassword(ID, new CambioPasswordRequest("mala", "nueva12345")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(throttle).registrarFallo("pwd:1");
        verify(usuarios, never()).save(any());
    }

    @Test
    void cambiarPasswordBloqueadoLanzaDemasiadosIntentos() {
        when(throttle.bloqueado("pwd:1")).thenReturn(true);

        assertThatThrownBy(() -> service.cambiarPassword(ID, new CambioPasswordRequest("x", "nueva12345")))
                .isInstanceOf(DemasiadosIntentosException.class);
        verifyNoInteractions(usuarios);
    }

    @Test
    void laNuevaPasswordDebeSerDistintaDeLaActual() {
        usuarioExiste();
        when(encoder.matches("misma12345", "HASH")).thenReturn(true);

        assertThatThrownBy(() -> service.cambiarPassword(ID, new CambioPasswordRequest("misma12345", "misma12345")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarios, never()).save(any());
    }
}