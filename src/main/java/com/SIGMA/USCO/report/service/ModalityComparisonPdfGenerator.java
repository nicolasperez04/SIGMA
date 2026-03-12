package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.report.dto.ModalityTypeComparisonReportDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Servicio para generar PDF del reporte comparativo de modalidades por tipo.
 * RF-48 — Comparativa de Modalidades por Tipo de Grado.
 *
 * Formato institucional USCO: logo + membrete, líneas roja/dorada,
 * encabezado interno en páginas interiores, pie institucional.
 */
@Service
public class ModalityComparisonPdfGenerator {

    // ── Paleta institucional ──────────────────────────────────────────────────
    private static final BaseColor INSTITUTIONAL_RED  = new BaseColor(143, 30, 30);
    private static final BaseColor INSTITUTIONAL_GOLD = new BaseColor(213, 203, 160);
    private static final BaseColor LIGHT_GOLD         = new BaseColor(245, 242, 235);
    private static final BaseColor TEXT_BLACK         = BaseColor.BLACK;
    private static final BaseColor TEXT_GRAY          = new BaseColor(80, 80, 80);
    private static final BaseColor WHITE              = BaseColor.WHITE;

    // ── Fuentes ───────────────────────────────────────────────────────────────
    private static final Font TITLE_FONT        = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  20, INSTITUTIONAL_RED);
    private static final Font SECTION_FONT      = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  12, INSTITUTIONAL_RED);
    private static final Font SUBHEADER_FONT    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  11, INSTITUTIONAL_RED);
    private static final Font BOLD_FONT         = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, TEXT_BLACK);
    private static final Font NORMAL_FONT       = FontFactory.getFont(FontFactory.HELVETICA,        10, TEXT_BLACK);
    private static final Font SMALL_FONT        = FontFactory.getFont(FontFactory.HELVETICA,         9, TEXT_GRAY);
    private static final Font TINY_FONT         = FontFactory.getFont(FontFactory.HELVETICA,         8, TEXT_GRAY);
    private static final Font HEADER_TABLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, WHITE);
    private static final Font INFO_LABEL_FONT   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, TEXT_GRAY);
    private static final Font INFO_VALUE_FONT   = FontFactory.getFont(FontFactory.HELVETICA,         9, TEXT_BLACK);

    private static final DateTimeFormatter DATE_FULL    = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy, HH:mm");
    private static final DateTimeFormatter DATE_COMPACT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =========================================================================
    //  PUNTO DE ENTRADA
    // =========================================================================

    public ByteArrayOutputStream generatePDF(ModalityTypeComparisonReportDTO report)
            throws DocumentException, IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new PageEventHelper(report));
        document.open();

        // 1. Portada institucional
        addCoverPage(document, report);

        // 2. Resumen ejecutivo
        document.newPage();
        addInternalHeader(document, report);
        addExecutiveSummary(document, report);

        // 3. Análisis visual comparativo
        document.newPage();
        addInternalHeader(document, report);
        addVisualComparison(document, report);

        // 4. Estadísticas detalladas por tipo
        document.newPage();
        addInternalHeader(document, report);
        addDetailedStatistics(document, report);

        // 5. Distribución de estudiantes (opcional)
        if (report.getStudentDistributionByType() != null
                && !report.getStudentDistributionByType().isEmpty()) {
            document.newPage();
            addInternalHeader(document, report);
            addStudentDistribution(document, report);
        }

        // 6. Análisis de eficiencia
        document.newPage();
        addInternalHeader(document, report);
        addEfficiencyAnalysis(document, report);

        // 7. Comparación histórica (opcional)
        if (report.getHistoricalComparison() != null && !report.getHistoricalComparison().isEmpty()) {
            document.newPage();
            addInternalHeader(document, report);
            addHistoricalComparison(document, report);
        }

        // 8. Análisis de tendencias (opcional)
        if (report.getTrendsAnalysis() != null) {
            document.newPage();
            addInternalHeader(document, report);
            addTrendsAnalysis(document, report);
        }

        // 9. Conclusiones + pie institucional
        document.newPage();
        addInternalHeader(document, report);
        addConclusions(document, report);
        addFooterSection(document, report);

        document.close();
        return out;
    }

    // =========================================================================
    //  PORTADA INSTITUCIONAL
    // =========================================================================

    private void addCoverPage(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException, IOException {

        // Header con logo
        InstitutionalPdfHeader.addHeader(
                document,
                "Facultad de Ingeniería",
                report.getAcademicProgramName()
                        + (report.getAcademicProgramCode() != null
                            ? " — Cód. " + report.getAcademicProgramCode() : ""),
                "Reporte Comparativo de Modalidades por Tipo de Grado"
        );

        addSpacingParagraph(document, 10f);

        // Caja central del título
        PdfPTable titleBox = new PdfPTable(1);
        titleBox.setWidthPercentage(90);
        titleBox.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleBox.setSpacingAfter(18f);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(INSTITUTIONAL_RED);
        titleCell.setPadding(16f);
        titleCell.setBorder(Rectangle.NO_BORDER);

        Paragraph titlePara = new Paragraph(
                "REPORTE COMPARATIVO DE\nMODALIDADES POR TIPO DE GRADO",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, WHITE));
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(titlePara);

        Paragraph progPara = new Paragraph(
                report.getAcademicProgramName().toUpperCase(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, INSTITUTIONAL_GOLD));
        progPara.setAlignment(Element.ALIGN_CENTER);
        progPara.setSpacingBefore(8f);
        titleCell.addElement(progPara);

        if (report.getYear() != null) {
            String periodo = "Periodo: " + report.getYear()
                    + (report.getSemester() != null ? " — Semestre " + report.getSemester() : "");
            Paragraph periodPara = new Paragraph(
                    periodo,
                    FontFactory.getFont(FontFactory.HELVETICA, 10, INSTITUTIONAL_GOLD));
            periodPara.setAlignment(Element.ALIGN_CENTER);
            periodPara.setSpacingBefore(4f);
            titleCell.addElement(periodPara);
        }

        titleBox.addCell(titleCell);
        document.add(titleBox);

        // Tabla de información de portada
        PdfPTable infoBox = new PdfPTable(2);
        infoBox.setWidthPercentage(80);
        infoBox.setHorizontalAlignment(Element.ALIGN_CENTER);
        infoBox.setSpacingAfter(20f);
        try { infoBox.setWidths(new float[]{42f, 58f}); } catch (DocumentException ignored) {}

        addCoverInfoRow(infoBox, "Programa:", report.getAcademicProgramName());
        if (report.getAcademicProgramCode() != null) {
            addCoverInfoRow(infoBox, "Código:", report.getAcademicProgramCode());
        }
        addCoverInfoRow(infoBox, "Fecha de generación:",
                report.getGeneratedAt().format(DATE_FULL));
        addCoverInfoRow(infoBox, "Generado por:", report.getGeneratedBy().split(" \\(")[0]);
        addCoverInfoRow(infoBox, "Tipos de modalidad:",
                String.valueOf(report.getSummary().getTotalModalityTypes()));
        addCoverInfoRow(infoBox, "Total de modalidades:",
                String.valueOf(report.getSummary().getTotalModalities()));
        document.add(infoBox);

        // Líneas de cierre de portada
        InstitutionalPdfHeader.addRedLine(document);
        InstitutionalPdfHeader.addGoldLine(document);

        addSpacingParagraph(document, 16f);
        Paragraph footer = new Paragraph(
                "Sistema Integral de Gestión de Modalidades de Grado — SIGMA\n"
                        + "Universidad Surcolombiana",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.newPage();
    }

    /** Fila de portada con etiqueta en dorado y valor en blanco. */
    private void addCoverInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, INFO_LABEL_FONT));
        labelCell.setBackgroundColor(LIGHT_GOLD);
        labelCell.setPadding(7f);
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderColor(INSTITUTIONAL_GOLD);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "—", INFO_VALUE_FONT));
        valueCell.setBackgroundColor(WHITE);
        valueCell.setPadding(7f);
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(INSTITUTIONAL_GOLD);
        table.addCell(valueCell);
    }

    // =========================================================================
    //  ENCABEZADO INTERNO (páginas interiores)
    // =========================================================================

    private void addInternalHeader(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        PdfPTable strip = new PdfPTable(2);
        strip.setWidthPercentage(100);
        strip.setSpacingAfter(8f);
        try { strip.setWidths(new float[]{70f, 30f}); } catch (DocumentException ignored) {}

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.BOTTOM);
        leftCell.setBorderColorBottom(INSTITUTIONAL_RED);
        leftCell.setBorderWidthBottom(2f);
        leftCell.setPaddingBottom(4f);

        Paragraph univ = new Paragraph("UNIVERSIDAD SURCOLOMBIANA — SIGMA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INSTITUTIONAL_RED));
        leftCell.addElement(univ);
        Paragraph prog = new Paragraph(report.getAcademicProgramName(),
                FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_GRAY));
        leftCell.addElement(prog);
        strip.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.BOTTOM);
        rightCell.setBorderColorBottom(INSTITUTIONAL_GOLD);
        rightCell.setBorderWidthBottom(2f);
        rightCell.setPaddingBottom(4f);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph dateP = new Paragraph(
                "Reporte Comparativo — " + report.getGeneratedAt().format(DATE_COMPACT),
                FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_GRAY));
        dateP.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(dateP);
        strip.addCell(rightCell);

        document.add(strip);
    }

    // =========================================================================
    //  SECCIONES DE CONTENIDO
    // =========================================================================

    private void addExecutiveSummary(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "1. RESUMEN EJECUTIVO");

        ModalityTypeComparisonReportDTO.ComparisonSummaryDTO summary = report.getSummary();

        // Tarjetas métricas principales (fila 1)
        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        try { summaryTable.setWidths(new int[]{25, 25, 25, 25}); } catch (DocumentException ignored) {}
        summaryTable.setSpacingAfter(12f);

        addMetricCard(summaryTable, "Tipos de Modalidad",
                String.valueOf(summary.getTotalModalityTypes()), INSTITUTIONAL_GOLD);
        addMetricCard(summaryTable, "Total Modalidades",
                String.valueOf(summary.getTotalModalities()), INSTITUTIONAL_RED);
        addMetricCard(summaryTable, "Total Estudiantes",
                String.valueOf(summary.getTotalStudents()), INSTITUTIONAL_GOLD);
        addMetricCard(summaryTable, "Prom. por Tipo",
                String.format("%.1f", summary.getAverageModalitiesPerType()), INSTITUTIONAL_RED);
        document.add(summaryTable);

        // Tarjetas métricas secundarias (fila 2)
        PdfPTable secondRow = new PdfPTable(2);
        secondRow.setWidthPercentage(100);
        secondRow.setSpacingAfter(18f);

        addWideMetricCard(secondRow, "Promedio de Estudiantes por Tipo",
                String.format("%.1f estudiantes", summary.getAverageStudentsPerType()),
                LIGHT_GOLD, INSTITUTIONAL_RED);

        if (summary.getMostPopularType() != null && summary.getTotalModalities() > 0) {
            double pct = (double) summary.getMostPopularTypeCount() / summary.getTotalModalities() * 100;
            addWideMetricCard(secondRow, "Concentración en Tipo Principal",
                    String.format("%.1f%% en %s", pct, summary.getMostPopularType()),
                    LIGHT_GOLD, INSTITUTIONAL_GOLD);
        }
        document.add(secondRow);

        // Tipo más popular
        if (summary.getMostPopularType() != null) {
            addHighlightBox(document,
                    "TIPO MÁS POPULAR: " + summary.getMostPopularType()
                            + " (" + summary.getMostPopularTypeCount() + " modalidades)",
                    INSTITUTIONAL_GOLD, WHITE, 1.5f);
        }

        // Tipo menos popular
        if (summary.getLeastPopularType() != null && summary.getLeastPopularTypeCount() > 0) {
            addHighlightBox(document,
                    "TIPO MENOS POPULAR: " + summary.getLeastPopularType()
                            + " (" + summary.getLeastPopularTypeCount() + " modalidades)",
                    LIGHT_GOLD, INSTITUTIONAL_RED, 1f);
        }
    }

    private void addVisualComparison(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "2. ANÁLISIS VISUAL COMPARATIVO");

        Paragraph intro = new Paragraph(
                "Comparación de la distribución de modalidades y estudiantes por tipo:",
                NORMAL_FONT);
        intro.setSpacingAfter(14f);
        document.add(intro);

        List<ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO> stats =
                report.getModalityTypeStatistics();

        int totalModalities = stats.stream()
                .mapToInt(ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO::getTotalModalities).sum();
        int totalStudents = stats.stream()
                .mapToInt(ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO::getTotalStudents).sum();

        addSubsectionTitle(document, "2.1  Modalidades por tipo");
        for (ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO stat : stats) {
            addComparisonBar(document, stat.getModalityTypeName(),
                    stat.getTotalModalities(), totalModalities, "modalidades", INSTITUTIONAL_RED);
        }

        addSpacingParagraph(document, 10f);
        addSubsectionTitle(document, "2.2  Estudiantes por tipo");
        for (ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO stat : stats) {
            addComparisonBar(document, stat.getModalityTypeName(),
                    stat.getTotalStudents(), totalStudents, "estudiantes", INSTITUTIONAL_GOLD);
        }
    }

    private void addDetailedStatistics(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "3. ESTADÍSTICAS DETALLADAS POR TIPO DE MODALIDAD");

        List<ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO> statistics =
                report.getModalityTypeStatistics();
        int totalModalities = report.getSummary().getTotalModalities();

        for (int i = 0; i < statistics.size(); i++) {
            ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO stat = statistics.get(i);

            // Título del tipo (fondo rojo institucional)
            PdfPTable typeHeader = new PdfPTable(1);
            typeHeader.setWidthPercentage(100);
            typeHeader.setSpacingBefore(i > 0 ? 18f : 4f);
            typeHeader.setSpacingAfter(4f);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(INSTITUTIONAL_RED);
            headerCell.setPadding(9f);
            headerCell.setBorder(Rectangle.NO_BORDER);
            headerCell.addElement(new Paragraph(
                    (i + 1) + ". " + stat.getModalityTypeName(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, WHITE)));
            typeHeader.addCell(headerCell);
            document.add(typeHeader);

            if (stat.getDescription() != null && !stat.getDescription().isEmpty()) {
                Paragraph desc = new Paragraph(stat.getDescription(), SMALL_FONT);
                desc.setIndentationLeft(10f);
                desc.setSpacingAfter(8f);
                document.add(desc);
            }

            // Barras de métricas
            PdfPTable metricsTable = new PdfPTable(2);
            metricsTable.setWidthPercentage(100);
            metricsTable.setSpacingAfter(8f);
            try { metricsTable.setWidths(new int[]{60, 40}); } catch (DocumentException ignored) {}

            addMetricRowWithBar(metricsTable, "Total Modalidades",
                    stat.getTotalModalities(), totalModalities, "modalidades");
            addMetricRowWithBar(metricsTable, "Total Estudiantes",
                    stat.getTotalStudents(), report.getSummary().getTotalStudents(), "estudiantes");
            document.add(metricsTable);

            // Estadísticas secundarias (4 celdas)
            PdfPTable statsTable = new PdfPTable(4);
            statsTable.setWidthPercentage(100);
            try { statsTable.setWidths(new int[]{25, 25, 25, 25}); } catch (DocumentException ignored) {}
            statsTable.setSpacingAfter(8f);

            addStatCellEnhanced(statsTable, "Porcentaje Total",
                    String.format("%.1f%%", stat.getPercentageOfTotal()), INSTITUTIONAL_GOLD);
            addStatCellEnhanced(statsTable, "Prom. Est./Mod.",
                    String.format("%.2f", stat.getAverageStudentsPerModality()), LIGHT_GOLD);
            addStatCellEnhanced(statsTable, "Tipo predominante",
                    stat.getIndividualModalities() > stat.getGroupModalities()
                            ? "Individual" : "Grupal", INSTITUTIONAL_GOLD);
            addStatCellEnhanced(statsTable, "Director",
                    stat.getRequiresDirector() ? "Requerido" : "No requiere", LIGHT_GOLD);
            document.add(statsTable);

            // Información de directores si aplica
            if (stat.getRequiresDirector()) {
                int withDir   = stat.getModalitiesWithDirector();
                int withoutDir = stat.getModalitiesWithoutDirector();
                int totalDir  = withDir + withoutDir;

                PdfPTable dirTable = new PdfPTable(2);
                dirTable.setWidthPercentage(95);
                dirTable.setSpacingBefore(4f);
                dirTable.setSpacingAfter(8f);

                dirTable.addCell(createDirectorCell("Con Director: " + withDir,
                        withDir, totalDir,
                        new BaseColor(232, 245, 233), new BaseColor(76, 175, 80)));
                dirTable.addCell(createDirectorCell(
                        (withoutDir > 0 ? "Sin Director: " : "Sin Director: ") + withoutDir,
                        withoutDir, totalDir,
                        withoutDir > 0 ? new BaseColor(255, 243, 224) : new BaseColor(248, 249, 250),
                        withoutDir > 0 ? new BaseColor(255, 152, 0) : TEXT_GRAY));
                document.add(dirTable);
            }

            // Distribución por estado
            if (stat.getDistributionByStatus() != null && !stat.getDistributionByStatus().isEmpty()) {
                Paragraph statusLbl = new Paragraph("Distribución por Estado:", BOLD_FONT);
                statusLbl.setSpacingBefore(6f);
                statusLbl.setSpacingAfter(4f);
                document.add(statusLbl);
                addStatusDistributionBars(document, stat.getDistributionByStatus(), stat.getTotalModalities());
            }

            // Tendencia (si existe)
            if (stat.getTrend() != null) {
                BaseColor tColor; String tIcon; String tText;
                switch (stat.getTrend()) {
                    case "INCREASING" -> { tColor = new BaseColor(76, 175, 80);   tIcon = "↗"; tText = "EN CRECIMIENTO"; }
                    case "DECREASING" -> { tColor = new BaseColor(244, 67, 54);   tIcon = "↘"; tText = "EN DECLIVE"; }
                    default           -> { tColor = INSTITUTIONAL_GOLD;           tIcon = "→"; tText = "ESTABLE"; }
                }

                PdfPTable trendBox = new PdfPTable(1);
                trendBox.setWidthPercentage(95);
                trendBox.setSpacingBefore(4f);
                trendBox.setSpacingAfter(8f);

                PdfPCell tCell = new PdfPCell();
                tCell.setPadding(7f);
                tCell.setBorder(Rectangle.BOX);
                tCell.setBorderWidth(1.5f);
                tCell.setBorderColor(tColor);

                BaseColor tBg = new BaseColor(
                        tColor.getRed()   + (255 - tColor.getRed())   * 9 / 10,
                        tColor.getGreen() + (255 - tColor.getGreen()) * 9 / 10,
                        tColor.getBlue()  + (255 - tColor.getBlue())  * 9 / 10);
                tCell.setBackgroundColor(tBg);

                Paragraph tPara = new Paragraph();
                tPara.add(new Chunk(tIcon + " Tendencia: ",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, tColor)));
                tPara.add(new Chunk(tText,
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, tColor)));
                tCell.addElement(tPara);
                trendBox.addCell(tCell);
                document.add(trendBox);
            }

            // Separador entre tipos (excepto el último)
            if (i < statistics.size() - 1) {
                InstitutionalPdfHeader.addGoldLine(document);
            }
        }
    }

    private void addStudentDistribution(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "4. DISTRIBUCIÓN DE ESTUDIANTES POR TIPO");

        Paragraph intro = new Paragraph(
                "Cantidad de estudiantes únicos por tipo de modalidad:", NORMAL_FONT);
        intro.setSpacingAfter(14f);
        document.add(intro);

        Map<String, Integer> distribution = report.getStudentDistributionByType();
        int maxStudents   = distribution.values().stream().max(Integer::compare).orElse(1);
        int totalStudents = distribution.values().stream().mapToInt(Integer::intValue).sum();

        List<Map.Entry<String, Integer>> sorted = distribution.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(java.util.stream.Collectors.toList());

        for (Map.Entry<String, Integer> entry : sorted) {
            PdfPTable barContainer = new PdfPTable(1);
            barContainer.setWidthPercentage(100);
            barContainer.setSpacingAfter(10f);

            // Título del tipo
            PdfPCell hdrCell = new PdfPCell();
            hdrCell.setBackgroundColor(INSTITUTIONAL_RED);
            hdrCell.setPadding(6f);
            hdrCell.setBorder(Rectangle.NO_BORDER);
            hdrCell.addElement(new Paragraph(entry.getKey(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, WHITE)));
            barContainer.addCell(hdrCell);

            // Barra de distribución
            float barWidth   = (float) entry.getValue() / maxStudents * 85;
            float emptyWidth = 100 - barWidth;

            PdfPTable inner = new PdfPTable(2);
            inner.setWidthPercentage(100);
            try { inner.setWidths(new float[]{Math.max(barWidth, 0.1f), Math.max(emptyWidth, 0.1f)}); }
            catch (DocumentException ignored) {}

            PdfPCell filled = new PdfPCell(new Phrase(
                    entry.getValue() + " estudiantes",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, WHITE)));
            filled.setBackgroundColor(INSTITUTIONAL_GOLD);
            filled.setBorder(Rectangle.NO_BORDER);
            filled.setPadding(8f);
            inner.addCell(filled);

            double pct = (double) entry.getValue() / totalStudents * 100;
            PdfPCell empty = new PdfPCell(new Phrase(
                    String.format("%.1f%% del total", pct),
                    FontFactory.getFont(FontFactory.HELVETICA, 9, INSTITUTIONAL_RED)));
            empty.setBackgroundColor(LIGHT_GOLD);
            empty.setBorder(Rectangle.NO_BORDER);
            empty.setPadding(8f);
            inner.addCell(empty);

            PdfPCell barCell = new PdfPCell();
            barCell.setPadding(0);
            barCell.setBorder(Rectangle.BOX);
            barCell.setBorderColor(LIGHT_GOLD);
            barCell.setBorderWidth(0.5f);
            barCell.addElement(inner);
            barContainer.addCell(barCell);

            document.add(barContainer);
        }

        // Total
        PdfPTable totalBox = new PdfPTable(1);
        totalBox.setWidthPercentage(100);
        totalBox.setSpacingBefore(12f);

        PdfPCell totalCell = new PdfPCell();
        totalCell.setBackgroundColor(INSTITUTIONAL_GOLD);
        totalCell.setPadding(10f);
        totalCell.setBorder(Rectangle.NO_BORDER);
        Paragraph totalPara = new Paragraph("TOTAL DE ESTUDIANTES: " + totalStudents,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, WHITE));
        totalPara.setAlignment(Element.ALIGN_CENTER);
        totalCell.addElement(totalPara);
        totalBox.addCell(totalCell);
        document.add(totalBox);
    }

    private void addEfficiencyAnalysis(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "5. ANÁLISIS DE EFICIENCIA");

        List<ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO> stats =
                report.getModalityTypeStatistics();

        PdfPTable effTable = new PdfPTable(4);
        effTable.setWidthPercentage(100);
        effTable.setSpacingAfter(18f);
        try { effTable.setWidths(new int[]{35, 20, 25, 20}); } catch (DocumentException ignored) {}

        addTableHeader(effTable, "Tipo de Modalidad");
        addTableHeader(effTable, "Modalidades");
        addTableHeader(effTable, "Prom. Est./Mod.");
        addTableHeader(effTable, "Eficiencia");

        double avgEff = stats.stream()
                .mapToDouble(ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO::getAverageStudentsPerModality)
                .average().orElse(0);

        boolean alternate = false;
        for (ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO stat : stats) {
            BaseColor rowBg = alternate ? LIGHT_GOLD : WHITE;

            PdfPCell nameCell = new PdfPCell(new Phrase(stat.getModalityTypeName(), NORMAL_FONT));
            nameCell.setBackgroundColor(rowBg);
            nameCell.setPadding(8f);
            effTable.addCell(nameCell);

            PdfPCell modCell = new PdfPCell(new Phrase(
                    String.valueOf(stat.getTotalModalities()), BOLD_FONT));
            modCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            modCell.setBackgroundColor(rowBg);
            modCell.setPadding(8f);
            effTable.addCell(modCell);

            PdfPCell avgCell = new PdfPCell(new Phrase(
                    String.format("%.2f", stat.getAverageStudentsPerModality()), BOLD_FONT));
            avgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            avgCell.setBackgroundColor(rowBg);
            avgCell.setPadding(8f);
            effTable.addCell(avgCell);

            double eff = stat.getAverageStudentsPerModality();
            String effText; BaseColor effColor;
            if (eff > avgEff * 1.1) {
                effText = "Alta";   effColor = new BaseColor(76, 175, 80);
            } else if (eff < avgEff * 0.9) {
                effText = "Baja";   effColor = new BaseColor(255, 152, 0);
            } else {
                effText = "Normal"; effColor = INSTITUTIONAL_GOLD;
            }

            PdfPCell effCell = new PdfPCell(new Phrase(effText,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, effColor)));
            effCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            effCell.setBackgroundColor(rowBg);
            effCell.setPadding(8f);
            effTable.addCell(effCell);

            alternate = !alternate;
        }
        document.add(effTable);

        // Resumen
        PdfPTable sumEff = new PdfPTable(2);
        sumEff.setWidthPercentage(80);
        sumEff.setSpacingBefore(8f);
        addSummaryRow(sumEff, "Promedio general de estudiantes por modalidad:",
                String.format("%.2f", avgEff), BOLD_FONT);

        ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO mostEfficient = stats.stream()
                .max((s1, s2) -> Double.compare(
                        s1.getAverageStudentsPerModality(), s2.getAverageStudentsPerModality()))
                .orElse(null);
        if (mostEfficient != null) {
            addSummaryRow(sumEff, "Tipo más eficiente:",
                    mostEfficient.getModalityTypeName()
                            + " (" + String.format("%.2f", mostEfficient.getAverageStudentsPerModality()) + ")",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new BaseColor(76, 175, 80)));
        }
        document.add(sumEff);
    }

    private void addHistoricalComparison(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "6. COMPARACIÓN HISTÓRICA POR PERIODOS");

        List<ModalityTypeComparisonReportDTO.PeriodComparisonDTO> periods = report.getHistoricalComparison();

        PdfPTable compTable = new PdfPTable(periods.size() + 1);
        compTable.setWidthPercentage(100);
        compTable.setSpacingAfter(20f);

        addTableHeader(compTable, "Tipo de Modalidad");
        for (ModalityTypeComparisonReportDTO.PeriodComparisonDTO p : periods) {
            addTableHeader(compTable, p.getPeriodLabel());
        }

        for (String typeName : report.getStudentDistributionByType().keySet()) {
            PdfPCell typeCell = new PdfPCell(new Phrase(typeName, SMALL_FONT));
            typeCell.setBackgroundColor(LIGHT_GOLD);
            typeCell.setPadding(5f);
            compTable.addCell(typeCell);

            for (ModalityTypeComparisonReportDTO.PeriodComparisonDTO p : periods) {
                int cnt = p.getModalitiesByType().getOrDefault(typeName, 0);
                int stu = p.getStudentsByType().getOrDefault(typeName, 0);
                Phrase ph = new Phrase();
                ph.add(new Chunk(cnt + " modalidades\n",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
                ph.add(new Chunk(stu + " estudiantes",
                        FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_GRAY)));
                PdfPCell dc = new PdfPCell(ph);
                dc.setHorizontalAlignment(Element.ALIGN_CENTER);
                dc.setPadding(5f);
                compTable.addCell(dc);
            }
        }

        // Fila de totales
        PdfPCell totalLbl = new PdfPCell(new Phrase("TOTALES", HEADER_TABLE_FONT));
        totalLbl.setBackgroundColor(INSTITUTIONAL_RED);
        totalLbl.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalLbl.setPadding(5f);
        compTable.addCell(totalLbl);

        for (ModalityTypeComparisonReportDTO.PeriodComparisonDTO p : periods) {
            Phrase ph = new Phrase();
            ph.add(new Chunk(p.getTotalModalitiesInPeriod() + " modalidades\n",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, WHITE)));
            ph.add(new Chunk(p.getTotalStudentsInPeriod() + " estudiantes",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, WHITE)));
            PdfPCell tc = new PdfPCell(ph);
            tc.setBackgroundColor(INSTITUTIONAL_RED);
            tc.setHorizontalAlignment(Element.ALIGN_CENTER);
            tc.setPadding(5f);
            compTable.addCell(tc);
        }

        document.add(compTable);
    }

    private void addTrendsAnalysis(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "7. ANÁLISIS DE TENDENCIAS");

        ModalityTypeComparisonReportDTO.TrendsAnalysisDTO trends = report.getTrendsAnalysis();

        BaseColor tColor; String tIcon;
        switch (trends.getOverallTrend()) {
            case "GROWING"   -> { tColor = INSTITUTIONAL_GOLD; tIcon = "↗"; }
            case "DECLINING" -> { tColor = INSTITUTIONAL_RED;  tIcon = "↘"; }
            default          -> { tColor = INSTITUTIONAL_RED;  tIcon = "→"; }
        }

        PdfPTable overallBox = new PdfPTable(1);
        overallBox.setWidthPercentage(100);
        overallBox.setSpacingAfter(14f);

        PdfPCell trendCell = new PdfPCell();
        trendCell.setBackgroundColor(tColor);
        trendCell.setPadding(10f);
        trendCell.setBorder(Rectangle.NO_BORDER);
        Paragraph tP = new Paragraph();
        tP.add(new Chunk(tIcon + " TENDENCIA GENERAL: ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, WHITE)));
        tP.add(new Chunk(getTrendLabel(trends.getOverallTrend()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, WHITE)));
        trendCell.addElement(tP);
        overallBox.addCell(trendCell);
        document.add(overallBox);

        if (trends.getGrowingTypes()  != null && !trends.getGrowingTypes().isEmpty()) {
            addTrendSection(document, "↗ TIPOS EN CRECIMIENTO",
                    trends.getGrowingTypes(), trends.getGrowthRateByType(), INSTITUTIONAL_GOLD);
        }
        if (trends.getDecliningTypes() != null && !trends.getDecliningTypes().isEmpty()) {
            addTrendSection(document, "↘ TIPOS EN DECLIVE",
                    trends.getDecliningTypes(), trends.getGrowthRateByType(), INSTITUTIONAL_RED);
        }
        if (trends.getStableTypes() != null && !trends.getStableTypes().isEmpty()) {
            addTrendSection(document, "→ TIPOS ESTABLES",
                    trends.getStableTypes(), trends.getGrowthRateByType(), TEXT_GRAY);
        }

        if (trends.getMostImprovedType() != null) {
            Double rate = trends.getGrowthRateByType().get(trends.getMostImprovedType());
            addHighlightBox(document,
                    "Mayor Mejora: " + trends.getMostImprovedType()
                            + " (+" + String.format("%.2f", rate) + "%)",
                    LIGHT_GOLD, INSTITUTIONAL_RED, 1f);
        }

        if (trends.getMostDeclinedType() != null) {
            Double rate = trends.getGrowthRateByType().get(trends.getMostDeclinedType());
            addHighlightBox(document,
                    "Mayor Declive: " + trends.getMostDeclinedType()
                            + " (" + String.format("%.2f", rate) + "%)",
                    new BaseColor(255, 230, 230), INSTITUTIONAL_RED, 1f);
        }
    }

    private void addConclusions(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "8. CONCLUSIONES Y RECOMENDACIONES");

        List<String> conclusions = generateConclusions(report);
        for (int i = 0; i < conclusions.size(); i++) {
            Paragraph p = new Paragraph((i + 1) + ". " + conclusions.get(i), NORMAL_FONT);
            p.setSpacingAfter(10f);
            p.setIndentationLeft(20f);
            document.add(p);
        }
    }

    /** Pie institucional de cierre en la última página. */
    private void addFooterSection(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        addSpacingParagraph(document, 20f);
        InstitutionalPdfHeader.addRedLine(document);
        InstitutionalPdfHeader.addGoldLine(document);
        addSpacingParagraph(document, 8f);

        // Nota informativa
        PdfPTable noteBox = new PdfPTable(1);
        noteBox.setWidthPercentage(100);
        noteBox.setSpacingBefore(6f);

        PdfPCell noteCell = new PdfPCell();
        noteCell.setBackgroundColor(LIGHT_GOLD);
        noteCell.setPadding(10f);
        noteCell.setBorder(Rectangle.BOX);
        noteCell.setBorderColor(INSTITUTIONAL_GOLD);

        Paragraph noteText = new Paragraph();
        noteText.add(new Chunk("NOTA: ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_GRAY)));
        noteText.add(new Chunk(
                "Este reporte fue generado automáticamente por el Sistema SIGMA. "
                        + "Los datos corresponden al programa académico "
                        + report.getAcademicProgramName()
                        + " y están filtrados según los criterios especificados. "
                        + "Para consultas o análisis adicionales, contacte con la coordinación del programa.",
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_GRAY)));
        noteCell.addElement(noteText);
        noteBox.addCell(noteCell);
        document.add(noteBox);

        addSpacingParagraph(document, 8f);
        Paragraph pie = new Paragraph(
                "Sistema Integral de Gestión de Modalidades de Grado — SIGMA\n"
                        + "Universidad Surcolombiana · Facultad de Ingeniería",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY));
        pie.setAlignment(Element.ALIGN_CENTER);
        document.add(pie);
    }

    // =========================================================================
    //  HELPERS VISUALES
    // =========================================================================

    /** Título de sección con línea dorada inferior. */
    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, SECTION_FONT);
        p.setSpacingBefore(10f);
        p.setSpacingAfter(4f);
        document.add(p);
        InstitutionalPdfHeader.addGoldLine(document);
        addSpacingParagraph(document, 6f);
    }

    /** Subtítulo de sección sin línea. */
    private void addSubsectionTitle(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, SUBHEADER_FONT);
        p.setSpacingBefore(8f);
        p.setSpacingAfter(8f);
        document.add(p);
    }

    /** Caja de resaltado con borde de color. */
    private void addHighlightBox(Document document, String text,
            BaseColor bg, BaseColor borderColor, float borderWidth) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(8f);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setPadding(10f);
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(borderColor);
        c.setBorderWidth(borderWidth);
        c.addElement(new Paragraph(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, borderColor)));
        t.addCell(c);
        document.add(t);
    }

    /** Tarjeta métrica cuadrada con valor grande. */
    private void addMetricCard(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell card = new PdfPCell();
        card.setPadding(14f);
        card.setBorderColor(color);
        card.setBorderWidth(1.5f);
        card.setBackgroundColor(WHITE);

        Paragraph valP = new Paragraph(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, color));
        valP.setAlignment(Element.ALIGN_CENTER);
        valP.setSpacingAfter(4f);
        card.addElement(valP);

        Paragraph lblP = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_GRAY));
        lblP.setAlignment(Element.ALIGN_CENTER);
        card.addElement(lblP);
        table.addCell(card);
    }

    /** Tarjeta métrica ancha (ocupa media fila). */
    private void addWideMetricCard(PdfPTable table, String label, String value,
            BaseColor bgColor, BaseColor valueColor) {
        PdfPCell card = new PdfPCell();
        card.setPadding(12f);
        card.setBorderColor(INSTITUTIONAL_GOLD);
        card.setBorderWidth(1.5f);
        card.setBackgroundColor(bgColor);

        Paragraph lblP = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_GRAY));
        lblP.setAlignment(Element.ALIGN_CENTER);
        lblP.setSpacingAfter(5f);
        card.addElement(lblP);

        Paragraph valP = new Paragraph(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, valueColor));
        valP.setAlignment(Element.ALIGN_CENTER);
        card.addElement(valP);
        table.addCell(card);
    }

    /** Barra de comparación horizontal proporcional al total. */
    private void addComparisonBar(Document document, String label, int value, int totalValue,
            String unit, BaseColor color) throws DocumentException {

        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(7f);

        PdfPCell mainCell = new PdfPCell();
        mainCell.setPadding(0);
        mainCell.setBorder(Rectangle.BOX);
        mainCell.setBorderColor(INSTITUTIONAL_GOLD);
        mainCell.setBorderWidth(0.5f);

        PdfPTable inner = new PdfPTable(2);
        inner.setWidthPercentage(100);
        try { inner.setWidths(new float[]{30f, 70f}); } catch (DocumentException ignored) {}

        PdfPCell lblCell = new PdfPCell(new Phrase(label, SMALL_FONT));
        lblCell.setPadding(6f);
        lblCell.setBorder(Rectangle.NO_BORDER);
        lblCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        inner.addCell(lblCell);

        float pct = totalValue > 0 ? (float) value / totalValue * 100 : 0;
        String barText = value + " " + unit + " (" + String.format("%.1f%%", pct) + ")";
        PdfPCell barCell = new PdfPCell();
        barCell.setPadding(0);
        barCell.setBorder(Rectangle.NO_BORDER);
        barCell.addElement(createProgressBarTable(pct, barText, color));
        inner.addCell(barCell);

        mainCell.addElement(inner);
        outer.addCell(mainCell);
        document.add(outer);
    }

    private PdfPTable createProgressBarTable(float percentage, String text, BaseColor color) {
        PdfPTable bar = new PdfPTable(2);
        bar.setWidthPercentage(100);
        float barW   = Math.max(percentage * 0.85f, 0.1f);
        float emptyW = Math.max(100 - barW, 0.1f);
        try { bar.setWidths(new float[]{barW, emptyW}); } catch (DocumentException ignored) {}

        PdfPCell filled = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, WHITE)));
        filled.setBackgroundColor(color);
        filled.setBorder(Rectangle.NO_BORDER);
        filled.setPadding(5f);
        filled.setHorizontalAlignment(Element.ALIGN_CENTER);
        filled.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bar.addCell(filled);

        PdfPCell empty = new PdfPCell(new Phrase(
                String.format("%.0f%%", percentage),
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_GRAY)));
        empty.setBackgroundColor(LIGHT_GOLD);
        empty.setBorder(Rectangle.NO_BORDER);
        empty.setPadding(5f);
        empty.setHorizontalAlignment(Element.ALIGN_LEFT);
        empty.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bar.addCell(empty);

        return bar;
    }

    private void addMetricRowWithBar(PdfPTable table, String label, int value, int total, String unit)
            throws DocumentException {
        PdfPCell lblCell = new PdfPCell(new Phrase(label + ":", SMALL_FONT));
        lblCell.setBackgroundColor(LIGHT_GOLD);
        lblCell.setPadding(6f);
        lblCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(lblCell);

        PdfPCell barCell = new PdfPCell();
        barCell.setPadding(2f);
        barCell.setBorder(Rectangle.BOX);
        barCell.setBorderColor(INSTITUTIONAL_GOLD);
        float pct = total > 0 ? (float) value / total * 100 : 0;
        barCell.addElement(createProgressBarTable(pct, value + " " + unit, INSTITUTIONAL_RED));
        table.addCell(barCell);
    }

    private void addStatusDistributionBars(Document document,
            Map<String, Integer> distribution, int total) throws DocumentException {

        List<Map.Entry<String, Integer>> sorted = distribution.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(java.util.stream.Collectors.toList());

        PdfPTable statusTable = new PdfPTable(3);
        statusTable.setWidthPercentage(95);
        statusTable.setSpacingAfter(10f);
        try { statusTable.setWidths(new int[]{40, 15, 45}); } catch (DocumentException ignored) {}

        for (Map.Entry<String, Integer> entry : sorted) {
            float pct = total > 0 ? (float) entry.getValue() / total * 100 : 0;

            PdfPCell sCell = new PdfPCell(new Phrase(entry.getKey(), SMALL_FONT));
            sCell.setPadding(5f);
            sCell.setBorder(Rectangle.NO_BORDER);
            statusTable.addCell(sCell);

            PdfPCell cCell = new PdfPCell(new Phrase(String.valueOf(entry.getValue()), BOLD_FONT));
            cCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cCell.setPadding(5f);
            cCell.setBorder(Rectangle.NO_BORDER);
            statusTable.addCell(cCell);

            PdfPCell bCell = new PdfPCell();
            bCell.setPadding(2f);
            bCell.setBorder(Rectangle.NO_BORDER);
            bCell.addElement(createMiniProgressBar(pct, INSTITUTIONAL_GOLD));
            statusTable.addCell(bCell);
        }
        document.add(statusTable);
    }

    private PdfPTable createMiniProgressBar(float pct, BaseColor color) {
        PdfPTable bar = new PdfPTable(2);
        bar.setWidthPercentage(100);
        float barW   = Math.max(pct, 0.1f);
        float emptyW = Math.max(100 - barW, 0.1f);
        try { bar.setWidths(new float[]{barW, emptyW}); } catch (DocumentException ignored) {}

        PdfPCell filled = new PdfPCell();
        filled.setBackgroundColor(color);
        filled.setBorder(Rectangle.NO_BORDER);
        filled.setMinimumHeight(10f);
        bar.addCell(filled);

        PdfPCell empty = new PdfPCell(new Phrase(
                " " + String.format("%.1f%%", pct),
                FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_GRAY)));
        empty.setBackgroundColor(new BaseColor(240, 240, 240));
        empty.setBorder(Rectangle.NO_BORDER);
        empty.setMinimumHeight(10f);
        empty.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bar.addCell(empty);
        return bar;
    }

    private PdfPCell createDirectorCell(String text, int value, int total,
            BaseColor bgColor, BaseColor textColor) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8f);
        cell.setBackgroundColor(bgColor);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(INSTITUTIONAL_GOLD);
        cell.setBorderWidth(0.5f);
        cell.addElement(new Paragraph(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, textColor)));
        if (total > 0) {
            float pct = (float) value / total * 100;
            cell.addElement(createMiniProgressBar(pct, textColor));
        }
        return cell;
    }

    private void addStatCellEnhanced(PdfPTable table, String label, String value, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8f);
        cell.setBackgroundColor(bgColor);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(INSTITUTIONAL_GOLD);
        cell.setBorderWidth(0.5f);

        Paragraph lblP = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_GRAY));
        lblP.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(lblP);

        Paragraph valP = new Paragraph(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, INSTITUTIONAL_RED));
        valP.setAlignment(Element.ALIGN_CENTER);
        valP.setSpacingBefore(2f);
        cell.addElement(valP);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell header = new PdfPCell(new Phrase(text, HEADER_TABLE_FONT));
        header.setBackgroundColor(INSTITUTIONAL_RED);
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setPadding(8f);
        table.addCell(header);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font valueFont) {
        PdfPCell lc = new PdfPCell(new Phrase(label, NORMAL_FONT));
        lc.setBackgroundColor(LIGHT_GOLD);
        lc.setPadding(8f);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setHorizontalAlignment(Element.ALIGN_CENTER);
        vc.setPadding(8f);
        table.addCell(vc);
    }

    private void addTrendSection(Document document, String title,
            List<String> types, Map<String, Double> rates, BaseColor color) throws DocumentException {

        Paragraph sTitle = new Paragraph(title, SUBHEADER_FONT);
        sTitle.setSpacingBefore(10f);
        sTitle.setSpacingAfter(5f);
        document.add(sTitle);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(90);
        try { t.setWidths(new int[]{70, 30}); } catch (DocumentException ignored) {}
        t.setSpacingAfter(10f);

        for (String type : types) {
            PdfPCell tc = new PdfPCell(new Phrase(type, NORMAL_FONT));
            tc.setPadding(5f);
            t.addCell(tc);

            Double rate = rates.get(type);
            String rateStr = rate != null ? String.format("%+.2f%%", rate) : "N/A";
            PdfPCell rc = new PdfPCell(new Phrase(rateStr, BOLD_FONT));
            rc.setBackgroundColor(color);
            rc.setHorizontalAlignment(Element.ALIGN_CENTER);
            rc.setPadding(5f);
            t.addCell(rc);
        }
        document.add(t);
    }

    private void addSpacingParagraph(Document document, float height) throws DocumentException {
        Paragraph sp = new Paragraph(" ");
        sp.setSpacingBefore(height / 2f);
        sp.setSpacingAfter(height / 2f);
        document.add(sp);
    }

    private String getTrendLabel(String trend) {
        return switch (trend) {
            case "GROWING"   -> "EN CRECIMIENTO";
            case "DECLINING" -> "EN DECLIVE";
            default          -> "ESTABLE";
        };
    }

    private List<String> generateConclusions(ModalityTypeComparisonReportDTO report) {
        List<String> conclusions = new java.util.ArrayList<>();
        ModalityTypeComparisonReportDTO.ComparisonSummaryDTO summary = report.getSummary();

        conclusions.add("El programa ofrece " + summary.getTotalModalityTypes()
                + " tipos diferentes de modalidades de grado, con un total de "
                + summary.getTotalModalities() + " modalidades activas.");

        if (summary.getMostPopularType() != null) {
            conclusions.add("El tipo de modalidad más popular es \""
                    + summary.getMostPopularType() + "\", con "
                    + summary.getMostPopularTypeCount()
                    + " modalidades, lo que evidencia una preferencia significativa de los estudiantes.");
        }

        conclusions.add("En promedio, cada tipo de modalidad agrupa "
                + summary.getAverageModalitiesPerType()
                + " modalidades y " + summary.getAverageStudentsPerType() + " estudiantes.");

        if (report.getTrendsAnalysis() != null) {
            ModalityTypeComparisonReportDTO.TrendsAnalysisDTO trends = report.getTrendsAnalysis();
            switch (trends.getOverallTrend()) {
                case "GROWING" ->
                    conclusions.add("La tendencia general del programa es de crecimiento, con "
                            + trends.getGrowingTypes().size() + " tipos de modalidad en expansión.");
                case "DECLINING" ->
                    conclusions.add("Se observa una tendencia general de declive, "
                            + "sugiriendo la necesidad de revisar la oferta académica.");
                default ->
                    conclusions.add("El programa muestra una tendencia estable en la distribución de tipos de modalidad.");
            }
        }

        conclusions.add("Se recomienda continuar monitoreando las preferencias estudiantiles "
                + "y ajustar la oferta de modalidades según la demanda observada.");

        return conclusions;
    }

    // =========================================================================
    //  PIE DE PÁGINA (PageEvent)
    // =========================================================================

    private static class PageEventHelper extends PdfPageEventHelper {

        private final ModalityTypeComparisonReportDTO report;
        private static final Font FOOTER_FONT =
                FontFactory.getFont(FontFactory.HELVETICA, 7, new BaseColor(80, 80, 80));

        PageEventHelper(ModalityTypeComparisonReportDTO report) {
            this.report = report;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            // Línea dorada superior del pie
            cb.setColorStroke(new BaseColor(213, 203, 160));
            cb.setLineWidth(1f);
            cb.moveTo(document.leftMargin(), document.bottom() - 2);
            cb.lineTo(document.right() + document.leftMargin(), document.bottom() - 2);
            cb.stroke();

            // Texto del pie centrado
            Phrase footer = new Phrase(
                    "Pág. " + writer.getPageNumber()
                            + "  |  " + report.getAcademicProgramName()
                            + "  |  Reporte Comparativo de Modalidades"
                            + "  |  " + report.getGeneratedAt().format(
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    FOOTER_FONT);

            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 12, 0);
        }
    }
}

