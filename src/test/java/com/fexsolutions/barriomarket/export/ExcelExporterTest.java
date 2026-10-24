package com.fexsolutions.barriomarket.export;

import com.fexsolutions.barriomarket.dto.FilaVenta;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelExporterTest {

    private final ExcelExporter exporter = new ExcelExporter();

    private FilaVenta fila(int id, String producto, String cantidad, String precio, String subtotal) {
        return new FilaVenta(id, LocalDateTime.of(2026, 10, 24, 16, 28), producto,
                new BigDecimal(cantidad), new BigDecimal(precio), new BigDecimal(subtotal));
    }

    private Sheet leer(byte[] bytes) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            // se copia lo necesario antes de cerrar el libro
            return wb.getSheetAt(0) == null ? null : cargar(wb);
        }
    }

    private Sheet cargar(XSSFWorkbook wb) throws Exception {
        XSSFWorkbook copia = new XSSFWorkbook();
        Sheet destino = copia.createSheet("Ventas");
        Sheet origen = wb.getSheetAt(0);
        for (int i = 0; i <= origen.getLastRowNum(); i++) {
            Row o = origen.getRow(i);
            Row d = destino.createRow(i);
            for (int c = 0; c < o.getLastCellNum(); c++) {
                if (o.getCell(c) == null) continue;
                switch (o.getCell(c).getCellType()) {
                    case NUMERIC -> d.createCell(c).setCellValue(o.getCell(c).getNumericCellValue());
                    default -> d.createCell(c).setCellValue(o.getCell(c).getStringCellValue());
                }
            }
        }
        return destino;
    }

    @Test
    void generaEncabezadoUnaFilaPorDetalleYFilaDeTotal() throws Exception {
        byte[] bytes = exporter.generar(List.of(
                fila(1, "Arroz", "2", "5.00", "10.00"),
                fila(2, "Leche", "3", "2.00", "6.00")));

        Sheet hoja = leer(bytes);

        assertThat(hoja.getLastRowNum()).isEqualTo(3); // encabezado + 2 filas + total
        assertThat(hoja.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Venta");
        assertThat(hoja.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Arroz");
        assertThat(hoja.getRow(1).getCell(5).getNumericCellValue()).isEqualTo(10.0);
        assertThat(hoja.getRow(3).getCell(4).getStringCellValue()).isEqualTo("TOTAL");
        assertThat(hoja.getRow(3).getCell(5).getNumericCellValue()).isEqualTo(16.0);
    }

    @Test
    void sinFilasGeneraSoloEncabezadoYTotalEnCero() throws Exception {
        Sheet hoja = leer(exporter.generar(List.of()));

        assertThat(hoja.getLastRowNum()).isEqualTo(1);
        assertThat(hoja.getRow(1).getCell(5).getNumericCellValue()).isEqualTo(0.0);
    }
}