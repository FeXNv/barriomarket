package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.FilaVenta;
import com.fexsolutions.barriomarket.export.ExcelExporter;
import com.fexsolutions.barriomarket.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final VentaRepository ventas;
    private final ExcelExporter excel;

    public List<FilaVenta> filas(Integer idUsuario, LocalDate desde, LocalDate hasta) {
        throw new UnsupportedOperationException();
    }

    public byte[] excel(Integer idUsuario, LocalDate desde, LocalDate hasta) {
        throw new UnsupportedOperationException();
    }
}