package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.CambioPasswordRequest;
import com.fexsolutions.barriomarket.dto.PerfilRequest;
import com.fexsolutions.barriomarket.dto.PerfilResponse;
import com.fexsolutions.barriomarket.entity.Usuario;
import com.fexsolutions.barriomarket.exception.DemasiadosIntentosException;
import com.fexsolutions.barriomarket.exception.NoEncontradoException;
import com.fexsolutions.barriomarket.repository.UsuarioRepository;
import com.fexsolutions.barriomarket.security.LoginThrottle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerfilService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final LoginThrottle throttle;

    @Transactional(readOnly = true)
    public PerfilResponse obtener(Integer idUsuario) {
        return PerfilResponse.de(buscar(idUsuario));
    }

    @Transactional
    public PerfilResponse actualizar(Integer idUsuario, PerfilRequest req) {
        Usuario u = buscar(idUsuario);

        String nombre = StringUtils.trimToNull(req.nombre());
        if (nombre == null) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        String email = StringUtils.trimToNull(req.email());
        if (email != null && !email.equals(u.getEmail()) && usuarios.existsByEmail(email)) {
            throw new IllegalStateException("Ese correo ya está registrado");
        }

        u.setNombre(nombre);
        u.setEmail(email);
        Usuario guardado = usuarios.save(u);
        log.info("Perfil actualizado usuario={}", idUsuario);
        return PerfilResponse.de(guardado);
    }

    @Transactional
    public void cambiarPassword(Integer idUsuario, CambioPasswordRequest req) {
        String clave = "pwd:" + idUsuario;
        if (throttle.bloqueado(clave)) {
            log.warn("Cambio de contraseña bloqueado usuario={}", idUsuario);
            throw new DemasiadosIntentosException("Demasiados intentos fallidos. Intenta de nuevo en unos minutos");
        }

        Usuario u = buscar(idUsuario);
        if (!encoder.matches(req.passwordActual(), u.getPasswordHash())) {
            throttle.registrarFallo(clave);
            log.warn("Cambio de contraseña con clave actual incorrecta usuario={}", idUsuario);
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        if (req.passwordActual().equals(req.passwordNueva())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta de la actual");
        }

        u.setPasswordHash(encoder.encode(req.passwordNueva()));
        usuarios.save(u);
        throttle.limpiar(clave);
        log.info("Contraseña cambiada usuario={}", idUsuario);
    }

    private Usuario buscar(Integer idUsuario) {
        return usuarios.findById(idUsuario)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado"));
    }
}