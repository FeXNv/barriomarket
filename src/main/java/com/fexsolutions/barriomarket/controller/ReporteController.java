package com.fexsolutions.barriomarket.controller;

import com.fexsolutions.barriomarket.dto.FilaVenta;
import com.fexsolutions.barriomarket.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReporteService service;

    @GetMapping("/ventas")
    public List<FilaVenta> ventas(@AuthenticationPrincipal Integer idUsuario,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.filas(idUsuario, desde, hasta);
    }

    @GetMapping("/ventas/excel")
    public ResponseEntity<byte[]> excel(@AuthenticationPrincipal Integer idUsuario,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        byte[] bytes = service.excel(idUsuario, desde, hasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ventas.xlsx\"")
                .contentType(MediaType.parseMediaType(XLSX))
                .body(bytes);
    }
}