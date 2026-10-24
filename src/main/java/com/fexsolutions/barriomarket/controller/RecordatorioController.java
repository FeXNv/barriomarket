package com.fexsolutions.barriomarket.controller;

import com.fexsolutions.barriomarket.dto.RecordatorioResponse;
import com.fexsolutions.barriomarket.service.RecordatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recordatorios")
@RequiredArgsConstructor
public class RecordatorioController {

    private final RecordatorioService service;

    @GetMapping
    public List<RecordatorioResponse> pendientes(@AuthenticationPrincipal Integer idUsuario) {
        return service.pendientes(idUsuario);
    }

    @PatchMapping("/{id}/leido")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLeido(@AuthenticationPrincipal Integer idUsuario, @PathVariable Integer id) {
        service.marcarLeido(idUsuario, id);
    }
}