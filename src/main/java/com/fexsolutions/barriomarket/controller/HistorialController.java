package com.fexsolutions.barriomarket.controller;

import com.fexsolutions.barriomarket.dto.ActualizacionStockResponse;
import com.fexsolutions.barriomarket.service.HistorialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/historial")
@RequiredArgsConstructor
public class HistorialController {

    private final HistorialService servicio;

    @GetMapping
    public List<ActualizacionStockResponse> ultimas(@AuthenticationPrincipal Integer idUsuario,
                                                    @RequestParam(required = false) Integer limite) {
        return servicio.ultimas(idUsuario, limite);
    }
}