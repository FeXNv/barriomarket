package com.fexsolutions.barriomarket.controller;

import com.fexsolutions.barriomarket.dto.CategoriaRequest;
import com.fexsolutions.barriomarket.dto.CategoriaResponse;
import com.fexsolutions.barriomarket.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    @GetMapping
    public List<CategoriaResponse> listar(@AuthenticationPrincipal Integer idUsuario) {
        return service.listar(idUsuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(@AuthenticationPrincipal Integer idUsuario,
                                   @Valid @RequestBody CategoriaRequest req) {
        return service.crear(idUsuario, req.nombre());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Integer idUsuario, @PathVariable Integer id) {
        service.eliminar(idUsuario, id);
    }
}