package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.dto.FilaVenta;
import com.fexsolutions.barriomarket.export.ExcelExporter;
import com.fexsolutions.barriomarket.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteService {

    private static final int DIAS_POR_DEFECTO = 30;
    private static final long DIAS_MAXIMOS = 366;

    private final VentaRepository ventas;
    private final ExcelExporter excel;

    @Transactional(readOnly = true)
    public List<FilaVenta> filas(Integer idUsuario, LocalDate desde, LocalDate hasta) {
        LocalDate fin = (hasta != null) ? hasta : LocalDate.now();
        LocalDate inicio = (desde != null) ? desde : fin.minusDays(DIAS_POR_DEFECTO);

        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > DIAS_MAXIMOS) {
            throw new IllegalArgumentException("El rango máximo es de " + DIAS_MAXIMOS + " días");
        }
        return ventas.filasReporte(idUsuario, inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public byte[] excel(Integer idUsuario, LocalDate desde, LocalDate hasta) {
        List<FilaVenta> filas = filas(idUsuario, desde, hasta);
        log.info("Exportando Excel usuario={} filas={}", idUsuario, filas.size());
        return excel.generar(filas);
    }
}