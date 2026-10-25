package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.*;
import com.fexsolutions.barriomarket.entity.Categoria;
import com.fexsolutions.barriomarket.entity.Usuario;
import com.fexsolutions.barriomarket.repository.CategoriaRepository;
import com.fexsolutions.barriomarket.repository.UsuarioRepository;
import com.fexsolutions.barriomarket.security.JwtService;
import com.fexsolutions.barriomarket.security.LoginThrottle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fexsolutions.barriomarket.security.LoginThrottle;
import com.fexsolutions.barriomarket.exception.DemasiadosIntentosException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final List<String> CATEGORIAS_INICIALES =
            List.of("Granos", "Lácteos", "Bebidas", "Dulces", "Cocina");

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginThrottle throttle;

    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        if (usuarios.existsByTelefono(req.telefono())) {
            throw new IllegalStateException("Ese teléfono ya está registrado");
        }
        String email = StringUtils.trimToNull(req.email());
        if (email != null && usuarios.existsByEmail(email)) {
            throw new IllegalStateException("Ese correo ya está registrado");
        }

        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setTelefono(req.telefono());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u = usuarios.save(u);

        for (String nombre : CATEGORIAS_INICIALES) {
            Categoria c = new Categoria();
            c.setNombre(nombre);
            c.setIdUsuario(u.getId());
            categorias.save(c);
        }
        log.info("Usuario registrado id={}", u.getId());
        return new AuthResponse(jwt.generar(u.getId()), u.getId(), u.getNombre());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String clave = req.telefono().trim();
        if (throttle.bloqueado(clave)) {
            log.warn("Login bloqueado por demasiados intentos fallidos");
            throw new DemasiadosIntentosException("Demasiados intentos fallidos. Intenta de nuevo en unos minutos");
        }

        Usuario u = usuarios.findByTelefonoAndDeletedAtIsNull(clave)
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> {
                    throttle.registrarFallo(clave);
                    log.warn("Login fallido");
                    return new BadCredentialsException("Credenciales inválidas");
                });

        throttle.limpiar(clave);
        log.info("Login correcto id={}", u.getId());
        return new AuthResponse(jwt.generar(u.getId()), u.getId(), u.getNombre());
    }
}