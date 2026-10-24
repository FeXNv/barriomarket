package com.fexsolutions.barriomarket.service;

import com.fexsolutions.barriomarket.export.ExcelExporter;
import com.fexsolutions.barriomarket.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final Integer USUARIO = 5;

    @Mock VentaRepository ventas;
    private ReporteService service;

    @BeforeEach
    void preparar() {
        service = new ReporteService(ventas, new ExcelExporter());
    }

    @Test
    void elRangoDeFechasIncluyeElDiaFinalCompleto() {
        when(ventas.filasReporte(USUARIO,
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0))).thenReturn(List.of());

        service.filas(USUARIO, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        verify(ventas).filasReporte(USUARIO,
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0));
    }

    @Test
    void desdeMayorQueHastaEsInvalido() {
        assertThatThrownBy(() -> service.filas(USUARIO, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rangoMayorA366DiasEsInvalido() {
        assertThatThrownBy(() -> service.filas(USUARIO, LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}