package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.AuthResponse;
import com.fexsolutions.barriomarket.dto.LoginRequest;
import com.fexsolutions.barriomarket.dto.RegistroRequest;
import com.fexsolutions.barriomarket.entity.Usuario;
import com.fexsolutions.barriomarket.exception.DemasiadosIntentosException;
import com.fexsolutions.barriomarket.repository.CategoriaRepository;
import com.fexsolutions.barriomarket.repository.UsuarioRepository;
import com.fexsolutions.barriomarket.security.JwtService;
import com.fexsolutions.barriomarket.security.LoginThrottle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TELEFONO = "987654321";

    @Mock UsuarioRepository usuarios;
    @Mock CategoriaRepository categorias;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @Mock LoginThrottle throttle;
    private AuthService service;

    @BeforeEach
    void preparar() {
        service = new AuthService(usuarios, categorias, encoder, jwt, throttle);
    }

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(1);
        u.setNombre("Fernando");
        u.setTelefono(TELEFONO);
        u.setPasswordHash("HASH");
        return u;
    }

    @Test
    void registrarGuardaElHashYNuncaLaClaveEnClaro() {
        when(usuarios.existsByTelefono(TELEFONO)).thenReturn(false);
        when(encoder.encode("clave12345")).thenReturn("HASH");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(1);
            return u;
        });
        when(jwt.generar(1)).thenReturn("tok");

        AuthResponse r = service.registrar(new RegistroRequest("Fernando", TELEFONO, null, "clave12345"));

        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(cap.capture());
        assertThat(cap.getValue().getPasswordHash()).isEqualTo("HASH");
        assertThat(r.token()).isEqualTo("tok");
    }

    @Test
    void registrarCreaLasCincoCategoriasIniciales() {
        when(encoder.encode(any())).thenReturn("HASH");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(1);
            return u;
        });

        service.registrar(new RegistroRequest("Fernando", TELEFONO, null, "clave12345"));

        verify(categorias, times(5)).save(any());
    }

    @Test
    void registrarConTelefonoRepetidoFalla() {
        when(usuarios.existsByTelefono(TELEFONO)).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(new RegistroRequest("Otro", TELEFONO, null, "clave12345")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void loginBloqueadoLanzaDemasiadosIntentosYNoConsultaLaBase() {
        when(throttle.bloqueado(TELEFONO)).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginRequest(TELEFONO, "x")))
                .isInstanceOf(DemasiadosIntentosException.class);
        verifyNoInteractions(usuarios);
    }

    @Test
    void loginConTelefonoInexistenteRegistraElFallo() {
        when(usuarios.findByTelefonoAndDeletedAtIsNull(TELEFONO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest(TELEFONO, "x")))
                .isInstanceOf(BadCredentialsException.class);
        verify(throttle).registrarFallo(TELEFONO);
    }

    @Test
    void loginConClaveIncorrectaRegistraElFallo() {
        when(usuarios.findByTelefonoAndDeletedAtIsNull(TELEFONO)).thenReturn(Optional.of(usuario()));
        when(encoder.matches("mala", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest(TELEFONO, "mala")))
                .isInstanceOf(BadCredentialsException.class);
        verify(throttle).registrarFallo(TELEFONO);
    }

    @Test
    void loginCorrectoDevuelveTokenYLimpiaLosIntentos() {
        when(usuarios.findByTelefonoAndDeletedAtIsNull(TELEFONO)).thenReturn(Optional.of(usuario()));
        when(encoder.matches("clave12345", "HASH")).thenReturn(true);
        when(jwt.generar(1)).thenReturn("tok");

        AuthResponse r = service.login(new LoginRequest(TELEFONO, "clave12345"));

        assertThat(r.token()).isEqualTo("tok");
        verify(throttle).limpiar(TELEFONO);
    }
}