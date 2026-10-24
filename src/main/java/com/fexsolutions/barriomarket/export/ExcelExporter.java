package com.fexsolutions.barriomarket.export;

import com.fexsolutions.barriomarket.dto.FilaVenta;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ExcelExporter {

    private static final String[] ENCABEZADOS =
            {"Venta", "Fecha", "Producto", "Cantidad", "Precio unitario", "Subtotal"};
    private static final int[] ANCHOS = {10, 18, 30, 12, 16, 14};
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generar(List<FilaVenta> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Ventas");

            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle estiloEncabezado = libro.createCellStyle();
            estiloEncabezado.setFont(negrita);

            Row encabezado = hoja.createRow(0);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                encabezado.createCell(i).setCellValue(ENCABEZADOS[i]);
                encabezado.getCell(i).setCellStyle(estiloEncabezado);
                hoja.setColumnWidth(i, ANCHOS[i] * 256);
            }

            BigDecimal total = BigDecimal.ZERO;
            int numeroFila = 1;
            for (FilaVenta f : filas) {
                Row fila = hoja.createRow(numeroFila++);
                fila.createCell(0).setCellValue(f.idVenta());
                fila.createCell(1).setCellValue(f.fecha().format(FECHA));
                fila.createCell(2).setCellValue(f.producto());
                fila.createCell(3).setCellValue(f.cantidad().doubleValue());
                fila.createCell(4).setCellValue(f.precioUnitario().doubleValue());
                fila.createCell(5).setCellValue(f.subtotal().doubleValue());
                total = total.add(f.subtotal());
            }

            Row filaTotal = hoja.createRow(numeroFila);
            filaTotal.createCell(4).setCellValue("TOTAL");
            filaTotal.getCell(4).setCellStyle(estiloEncabezado);
            filaTotal.createCell(5).setCellValue(total.doubleValue());
            filaTotal.getCell(5).setCellStyle(estiloEncabezado);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel", e);
        }
    }
}