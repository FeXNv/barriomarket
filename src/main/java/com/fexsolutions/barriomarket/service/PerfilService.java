package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.CambioPasswordRequest;
import com.fexsolutions.barriomarket.dto.PerfilRequest;
import com.fexsolutions.barriomarket.dto.PerfilResponse;
import com.fexsolutions.barriomarket.repository.UsuarioRepository;
import com.fexsolutions.barriomarket.security.LoginThrottle;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final LoginThrottle throttle;

    public PerfilResponse obtener(Integer idUsuario) { throw new UnsupportedOperationException(); }
    public PerfilResponse actualizar(Integer idUsuario, PerfilRequest req) { throw new UnsupportedOperationException(); }
    public void cambiarPassword(Integer idUsuario, CambioPasswordRequest req) { throw new UnsupportedOperationException(); }
}