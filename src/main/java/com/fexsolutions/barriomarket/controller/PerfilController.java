package com.fexsolutions.barriomarket.controller;

import com.fexsolutions.barriomarket.dto.CambioPasswordRequest;
import com.fexsolutions.barriomarket.dto.PerfilRequest;
import com.fexsolutions.barriomarket.dto.PerfilResponse;
import com.fexsolutions.barriomarket.service.PerfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService service;

    @GetMapping
    public PerfilResponse obtener(@AuthenticationPrincipal Integer idUsuario) {
        return service.obtener(idUsuario);
    }

    @PutMapping
    public PerfilResponse actualizar(@AuthenticationPrincipal Integer idUsuario,
                                     @Valid @RequestBody PerfilRequest req) {
        return service.actualizar(idUsuario, req);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@AuthenticationPrincipal Integer idUsuario,
                                @Valid @RequestBody CambioPasswordRequest req) {
        service.cambiarPassword(idUsuario, req);
    }
}