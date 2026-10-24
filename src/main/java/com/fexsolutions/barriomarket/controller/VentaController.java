package com.fexsolutions.barriomarket.controller;

import com.fexsolutions.barriomarket.dto.VentaRequest;
import com.fexsolutions.barriomarket.dto.VentaResponse;
import com.fexsolutions.barriomarket.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaResponse registrar(@AuthenticationPrincipal Integer idUsuario,
                                   @Valid @RequestBody VentaRequest req) {
        return service.registrar(idUsuario, req);
    }
}