package com.barberia.reportes;

import com.barberia.dao.ReporteDAO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Exportador de reportes a PDF (Apache PDFBox) y Excel (Apache POI).
 */
public class ReporteExporter {

    private static final Logger log = LoggerFactory.getLogger(ReporteExporter.class);

    /**
     * Exporta reporte anual completo a PDF.
     */
    public static void exportarPDF(File archivo, int anio, ReporteDAO dao) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            Map<Integer, BigDecimal> gananciasMes = dao.getGananciasPorMes(anio);
            LocalDate inicio = LocalDate.of(anio, 1, 1);
            LocalDate fin = LocalDate.of(anio, 12, 31);
            BigDecimal total = dao.getTotalGanancias(inicio, fin);
            List<Object[]> citas = dao.getCitasDetalladas(inicio, fin);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float yPos = 800;
                float margin = 50;
                float width = page.getMediaBox().getWidth() - 2 * margin;

                // Título
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                cs.setNonStrokingColor(new java.awt.Color(40, 40, 80));
                cs.beginText();
                cs.newLineAtOffset(margin, yPos);
                cs.showText("Reporte Anual - Barbería Juan - " + anio);
                cs.endText();
                yPos -= 30;

                // Total anual
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                cs.beginText();
                cs.newLineAtOffset(margin, yPos);
                cs.showText("Total generado en " + anio + ": $" + total.setScale(2, java.math.RoundingMode.HALF_UP));
                cs.endText();
                yPos -= 30;

                // Línea separadora
                cs.setStrokingColor(new java.awt.Color(100, 100, 180));
                cs.moveTo(margin, yPos);
                cs.lineTo(margin + width, yPos);
                cs.stroke();
                yPos -= 20;

                // Ganancias mensuales
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.beginText();
                cs.newLineAtOffset(margin, yPos);
                cs.showText("Ganancias por Mes:");
                cs.endText();
                yPos -= 18;

                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                for (int mes = 1; mes <= 12; mes++) {
                    BigDecimal monto = gananciasMes.getOrDefault(mes, BigDecimal.ZERO);
                    String nombreMes = Month.of(mes).getDisplayName(TextStyle.FULL, new Locale("es"));
                    cs.beginText();
                    cs.newLineAtOffset(margin + 10, yPos);
                    cs.showText(String.format("%-20s $%s", nombreMes, monto.setScale(2, java.math.RoundingMode.HALF_UP)));
                    cs.endText();
                    yPos -= 16;
                }

                yPos -= 10;
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.beginText();
                cs.newLineAtOffset(margin, yPos);
                cs.showText("Resumen de citas: " + citas.size() + " citas registradas en " + anio);
                cs.endText();

                // Pie de página
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 9);
                cs.setNonStrokingColor(java.awt.Color.GRAY);
                cs.beginText();
                cs.newLineAtOffset(margin, 30);
                cs.showText("Generado el " + LocalDate.now() + " - Sistema Barbería Juan");
                cs.endText();
            }

            // Segunda página: tabla de citas (primeras 30)
            if (!citas.isEmpty()) {
                PDPage page2 = new PDPage(PDRectangle.A4);
                doc.addPage(page2);
                try (PDPageContentStream cs2 = new PDPageContentStream(doc, page2)) {
                    float y = 780;
                    cs2.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                    cs2.setNonStrokingColor(new java.awt.Color(40, 40, 80));
                    cs2.beginText();
                    cs2.newLineAtOffset(50, y);
                    cs2.showText("Detalle de citas - " + anio + " (máx. 30 registros)");
                    cs2.endText();
                    y -= 25;

                    cs2.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9);
                    cs2.beginText();
                    cs2.newLineAtOffset(50, y);
                    cs2.showText(String.format("%-6s %-20s %-15s %-15s %-12s %-10s",
                            "#", "Cliente", "Servicio", "Fecha", "Estado", "Monto"));
                    cs2.endText();
                    y -= 15;

                    cs2.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                    int limit = Math.min(citas.size(), 30);
                    for (int i = 0; i < limit; i++) {
                        Object[] row = citas.get(i);
                        cs2.beginText();
                        cs2.newLineAtOffset(50, y);
                        String line = String.format("%-6s %-20s %-15s %-15s %-12s %-10s",
                                row[0], truncate(row[1].toString(), 18),
                                truncate(row[2].toString(), 13),
                                row[3].toString().substring(0, 16),
                                row[4], "$" + row[5]);
                        cs2.showText(line);
                        cs2.endText();
                        y -= 13;
                        if (y < 50) break;
                    }
                }
            }

            doc.save(archivo);
            log.info("PDF exportado: {}", archivo.getAbsolutePath());
        }
    }

    /**
     * Exporta reporte anual completo a Excel (.xlsx).
     */
    public static void exportarExcel(File archivo, int anio, ReporteDAO dao) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // === Hoja 1: Ganancias Mensuales ===
            Sheet sheetMes = wb.createSheet("Ganancias Mensuales");
            CellStyle headerStyle = crearEstiloHeader(wb);
            CellStyle currencyStyle = crearEstiloCurrency(wb);

            Row header = sheetMes.createRow(0);
            createCell(header, 0, "Mes", headerStyle);
            createCell(header, 1, "Ganancias", headerStyle);

            Map<Integer, BigDecimal> gananciasMes = dao.getGananciasPorMes(anio);
            BigDecimal totalAnual = BigDecimal.ZERO;
            for (int mes = 1; mes <= 12; mes++) {
                BigDecimal monto = gananciasMes.getOrDefault(mes, BigDecimal.ZERO);
                Row row = sheetMes.createRow(mes);
                String nombreMes = Month.of(mes).getDisplayName(TextStyle.FULL, new Locale("es"));
                row.createCell(0).setCellValue(nombreMes);
                Cell cell = row.createCell(1);
                cell.setCellValue(monto.doubleValue());
                cell.setCellStyle(currencyStyle);
                totalAnual = totalAnual.add(monto);
            }
            Row totalRow = sheetMes.createRow(13);
            createCell(totalRow, 0, "TOTAL ANUAL", headerStyle);
            Cell totalCell = totalRow.createCell(1);
            totalCell.setCellValue(totalAnual.doubleValue());
            totalCell.setCellStyle(currencyStyle);

            sheetMes.autoSizeColumn(0);
            sheetMes.autoSizeColumn(1);

            // === Hoja 2: Citas Detalladas ===
            Sheet sheetCitas = wb.createSheet("Citas Detalladas");
            Row hCitas = sheetCitas.createRow(0);
            String[] cols = {"#", "Cliente", "Servicio", "Fecha/Hora", "Estado", "Monto", "Canal"};
            for (int i = 0; i < cols.length; i++) createCell(hCitas, i, cols[i], headerStyle);

            List<Object[]> citas = dao.getCitasDetalladas(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31));
            int rowNum = 1;
            for (Object[] cita : citas) {
                Row r = sheetCitas.createRow(rowNum++);
                r.createCell(0).setCellValue(cita[0] != null ? cita[0].toString() : "");
                r.createCell(1).setCellValue(cita[1] != null ? cita[1].toString() : "");
                r.createCell(2).setCellValue(cita[2] != null ? cita[2].toString() : "");
                r.createCell(3).setCellValue(cita[3] != null ? cita[3].toString() : "");
                r.createCell(4).setCellValue(cita[4] != null ? cita[4].toString() : "");
                if (cita[5] instanceof BigDecimal) {
                    Cell c = r.createCell(5);
                    c.setCellValue(((BigDecimal) cita[5]).doubleValue());
                    c.setCellStyle(currencyStyle);
                }
                r.createCell(6).setCellValue(cita[6] != null ? cita[6].toString() : "");
            }

            for (int i = 0; i < 7; i++) sheetCitas.autoSizeColumn(i);

            // === Hoja 3: Servicios Populares ===
            Sheet sheetSvcs = wb.createSheet("Servicios Populares");
            Row hSvcs = sheetSvcs.createRow(0);
            createCell(hSvcs, 0, "Servicio", headerStyle);
            createCell(hSvcs, 1, "Total Citas", headerStyle);
            createCell(hSvcs, 2, "Total Ingresos", headerStyle);

            List<Object[]> populares = dao.getServiciosPopulares(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31));
            int rn = 1;
            for (Object[] p : populares) {
                Row r = sheetSvcs.createRow(rn++);
                r.createCell(0).setCellValue(p[0].toString());
                r.createCell(1).setCellValue((int) p[1]);
                Cell c = r.createCell(2);
                c.setCellValue(p[2] != null ? ((BigDecimal) p[2]).doubleValue() : 0);
                c.setCellStyle(currencyStyle);
            }
            for (int i = 0; i < 3; i++) sheetSvcs.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                wb.write(fos);
            }
            log.info("Excel exportado: {}", archivo.getAbsolutePath());
        }
    }

    private static CellStyle crearEstiloHeader(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static CellStyle crearEstiloCurrency(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        return style;
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 2) + ".." : s;
    }
}
