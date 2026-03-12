package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.report.dto.CompletedModalitiesReportDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Servicio para generar PDF del reporte de modalidades completadas
 * Diseño profesional e institucional con análisis completo de resultados
 */
@Service
public class CompletedModalitiesPdfGenerator {

    // COLORES INSTITUCIONALES - USO EXCLUSIVO
    private static final BaseColor INSTITUTIONAL_RED = new BaseColor(143, 30, 30); // #8F1E1E - Color primario
    private static final BaseColor INSTITUTIONAL_GOLD = new BaseColor(213, 203, 160); // #D5CBA0 - Color secundario
    private static final BaseColor WHITE = BaseColor.WHITE; // Color primario
    private static final BaseColor LIGHT_GOLD = new BaseColor(245, 242, 235); // Tono muy claro de dorado para fondos sutiles
    private static final BaseColor TEXT_BLACK = BaseColor.BLACK; // Texto principal
    private static final BaseColor TEXT_GRAY = new BaseColor(80, 80, 80); // Texto secundario

    // Fuentes con colores institucionales
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, INSTITUTIONAL_RED);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, INSTITUTIONAL_RED);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, INSTITUTIONAL_RED);
    private static final Font SUBHEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, INSTITUTIONAL_RED);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_BLACK);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_BLACK);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_GRAY);
    private static final Font TINY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_GRAY);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, WHITE);
    private static final Font TABLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_BLACK);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ByteArrayOutputStream generatePDF(CompletedModalitiesReportDTO report) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        writer.setPageEvent(new CompletedModalitiesPageEventHelper(report));
        // IOException propagada desde addCoverPage (carga del logo institucional)
        document.open();

        // Validación de datos
        if (report == null) {
            document.add(new Paragraph("No hay datos para generar el reporte.", NORMAL_FONT));
            document.close();
            return outputStream;
        }
        if (report.getCompletedModalities() == null || report.getCompletedModalities().isEmpty()) {
            document.add(new Paragraph("No hay modalidades completadas para mostrar.", NORMAL_FONT));
            document.close();
            return outputStream;
        }

        // 1. Portada
        addCoverPage(document, report);

        // 2. Filtros y Resumen Ejecutivo
        document.newPage();
        addInternalHeader(document, report);
        addFiltersAndExecutiveSummary(document, report);

        // 3. Estadísticas Generales
        document.newPage();
        addInternalHeader(document, report);
        addGeneralStatistics(document, report);

        // 4. Análisis por Resultado
        document.newPage();
        addInternalHeader(document, report);
        addResultAnalysis(document, report);

        // 5. Análisis por Tipo de Modalidad
        document.newPage();
        addInternalHeader(document, report);
        addModalityTypeAnalysis(document, report);

        // 6. Listado Detallado de Modalidades Completadas
        document.newPage();
        addInternalHeader(document, report);
        addCompletedModalitiesListing(document, report);

        // 7. Análisis Temporal
        document.newPage();
        addInternalHeader(document, report);
        addTemporalAnalysis(document, report);

        // 8. Desempeño de Directores
        document.newPage();
        addInternalHeader(document, report);
        addDirectorPerformance(document, report);

        // 9. Análisis de Distinciones Académicas
        document.newPage();
        addInternalHeader(document, report);
        addDistinctionAnalysis(document, report);

        // 10. Cierre institucional
        addInstitutionalClosing(document, report);

        document.close();
        return outputStream;
    }

    /**
     * Portada del reporte
     */
    private void addCoverPage(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException, IOException {

        // 1. Encabezado institucional con logo
        InstitutionalPdfHeader.addHeader(
                document,
                "Facultad de Ingeniería",
                report.getAcademicProgramName() + (report.getAcademicProgramCode() != null
                        ? " — Cód. " + report.getAcademicProgramCode() : ""),
                "Reporte de Modalidades Completadas"
        );

        // 2. Caja de título roja institucional
        PdfPTable titleBox = new PdfPTable(1);
        titleBox.setWidthPercentage(100);
        titleBox.setSpacingAfter(14);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(new BaseColor(143, 30, 30));
        titleCell.setPadding(16);
        titleCell.setBorder(Rectangle.NO_BORDER);

        Paragraph titlePara = new Paragraph("REPORTE DE MODALIDADES COMPLETADAS\nAnálisis de Resultados Académicos",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.WHITE));
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(titlePara);
        titleBox.addCell(titleCell);
        document.add(titleBox);

        // 3. Línea dorada decorativa
        InstitutionalPdfHeader.addGoldLine(document);

        // 4. Tabla de información con bordes dorados
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(80);
        infoTable.setWidths(new float[]{42f, 58f});
        infoTable.setSpacingBefore(18);
        infoTable.setSpacingAfter(20);
        infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addCoverInfoRow(infoTable, "Fecha de generación:", report.getGeneratedAt().format(DATETIME_FORMATTER));
        addCoverInfoRow(infoTable, "Generado por:", report.getGeneratedBy());

        if (report.getExecutiveSummary() != null) {
            addCoverInfoRow(infoTable, "Total completadas:", String.valueOf(report.getExecutiveSummary().getTotalCompleted()));
            addCoverInfoRow(infoTable, "Exitosas:", String.valueOf(report.getExecutiveSummary().getTotalSuccessful()));
            addCoverInfoRow(infoTable, "Fallidas:", String.valueOf(report.getExecutiveSummary().getTotalFailed()));
            addCoverInfoRow(infoTable, "Tasa de éxito:", String.format("%.1f%%", report.getExecutiveSummary().getSuccessRate()));
        }
        document.add(infoTable);

        // 5. Líneas de cierre institucionales
        InstitutionalPdfHeader.addRedLine(document);
        InstitutionalPdfHeader.addGoldLine(document);

        // 6. Texto informativo
        Paragraph spacing = new Paragraph(" ");
        spacing.setSpacingAfter(10f);
        document.add(spacing);

        Paragraph disclaimer = new Paragraph(
            "Este reporte presenta un análisis completo de las modalidades de grado finalizadas, " +
            "incluyendo tanto las exitosas como las fallidas. Se incluyen estadísticas de calificaciones, " +
            "tiempos de completitud, distinciones académicas, desempeño de directores y tendencias temporales. " +
            "La información es generada automáticamente por el sistema SIGMA.",
            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, new BaseColor(80, 80, 80))
        );
        disclaimer.setAlignment(Element.ALIGN_JUSTIFIED);
        disclaimer.setIndentationLeft(50);
        disclaimer.setIndentationRight(50);
        document.add(disclaimer);

        Paragraph spacing2 = new Paragraph(" ");
        spacing2.setSpacingAfter(10f);
        document.add(spacing2);

        Paragraph closing = new Paragraph(
                "Sistema Integral de Gestión de Modalidades de Grado — SIGMA\n" +
                "Universidad Surcolombiana | Facultad de Ingeniería | Neiva – Huila",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new BaseColor(80, 80, 80)));
        closing.setAlignment(Element.ALIGN_CENTER);
        document.add(closing);
    }

    /**
     * Fila de información en la portada con estilo institucional.
     */
    private void addCoverInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new BaseColor(143, 30, 30))));
        labelCell.setBackgroundColor(new BaseColor(245, 242, 235));
        labelCell.setPadding(8f);
        labelCell.setBorderColor(new BaseColor(213, 203, 160));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "—",
                FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK)));
        valueCell.setPadding(8f);
        valueCell.setBorderColor(new BaseColor(213, 203, 160));
        table.addCell(valueCell);
    }

    /**
     * Filtros y resumen ejecutivo
     */
    private void addFiltersAndExecutiveSummary(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "1. FILTROS APLICADOS Y RESUMEN EJECUTIVO");

        // Filtros aplicados
        if (report.getAppliedFilters() != null) {
            addSubsectionTitle(document, "Filtros Aplicados");

            PdfPTable filterTable = new PdfPTable(1);
            filterTable.setWidthPercentage(100);
            filterTable.setSpacingBefore(10);
            filterTable.setSpacingAfter(20);

            PdfPCell filterCell = new PdfPCell();
            filterCell.setBackgroundColor(LIGHT_GOLD);
            filterCell.setPadding(15);
            filterCell.setBorder(Rectangle.NO_BORDER);

            Paragraph filterText = new Paragraph(
                report.getAppliedFilters().getFilterDescription(),
                NORMAL_FONT
            );
            filterCell.addElement(filterText);
            filterTable.addCell(filterCell);
            document.add(filterTable);
        }

        // Resumen ejecutivo
        if (report.getExecutiveSummary() != null) {
            addSubsectionTitle(document, "Resumen Ejecutivo");

            CompletedModalitiesReportDTO.ExecutiveSummaryDTO summary = report.getExecutiveSummary();

            // Métricas principales en tarjetas
            PdfPTable metricsTable = new PdfPTable(5);
            metricsTable.setWidthPercentage(100);
            metricsTable.setSpacingBefore(10);
            metricsTable.setSpacingAfter(20);

            addMetricCard(metricsTable, "Total Completadas",
                String.valueOf(summary.getTotalCompleted()), INSTITUTIONAL_GOLD);
            addMetricCard(metricsTable, "Exitosas",
                String.valueOf(summary.getTotalSuccessful()), INSTITUTIONAL_GOLD);
            addMetricCard(metricsTable, "Fallidas",
                String.valueOf(summary.getTotalFailed()), INSTITUTIONAL_RED);
            addMetricCard(metricsTable, "Tasa de Éxito",
                String.format("%.1f%%", summary.getSuccessRate()), INSTITUTIONAL_GOLD);
            addMetricCard(metricsTable, "Con Distinción",
                String.valueOf(summary.getWithDistinction()), INSTITUTIONAL_GOLD);

            document.add(metricsTable);

            // Segunda fila de métricas
            PdfPTable metrics2Table = new PdfPTable(4);
            metrics2Table.setWidthPercentage(100);
            metrics2Table.setSpacingBefore(5);
            metrics2Table.setSpacingAfter(15);

            addMetricCard(metrics2Table, "Calificación Promedio",
                String.format("%.2f", summary.getAverageGrade()), INSTITUTIONAL_GOLD);
            addMetricCard(metrics2Table, "Días Promedio",
                String.format("%.0f", summary.getAverageCompletionDays()), INSTITUTIONAL_RED);
            addMetricCard(metrics2Table, "Total Estudiantes",
                String.valueOf(summary.getTotalStudents()), INSTITUTIONAL_GOLD);
            addMetricCard(metrics2Table, "Directores Únicos",
                String.valueOf(summary.getUniqueDirectors()), TEXT_GRAY);

            document.add(metrics2Table);
        }
    }

    /**
     * Estadísticas generales
     */
    private void addGeneralStatistics(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "2. ESTADÍSTICAS GENERALES");

        if (report.getGeneralStatistics() == null) {
            document.add(new Paragraph("No hay estadísticas disponibles.", NORMAL_FONT));
            return;
        }

        CompletedModalitiesReportDTO.GeneralStatisticsDTO stats = report.getGeneralStatistics();

        // NUEVO: Tarjetas de resumen con iconos
        addGeneralStatsSummaryCards(document, stats);

        // Resultados con gráfico visual mejorado
        addSubsectionTitle(document, "Resultados Generales");

        PdfPTable resultsTable = new PdfPTable(4);
        resultsTable.setWidthPercentage(90);
        resultsTable.setSpacingBefore(10);
        resultsTable.setSpacingAfter(20);
        resultsTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addStatsCard(resultsTable, "Aprobadas",
            String.valueOf(stats.getApproved()), INSTITUTIONAL_GOLD);
        addStatsCard(resultsTable, "Reprobadas",
            String.valueOf(stats.getFailed()), INSTITUTIONAL_RED);
        addStatsCard(resultsTable, "Tasa Aprobación",
            String.format("%.1f%%", stats.getApprovalRate()), INSTITUTIONAL_GOLD);
        addStatsCard(resultsTable, "Total",
            String.valueOf(stats.getTotalCompleted()), INSTITUTIONAL_RED);

        document.add(resultsTable);

        // NUEVO: Gráfico visual de tasa de aprobación
        addApprovalRateChart(document, stats);

        // Tiempos de completitud con visualización mejorada
        addSubsectionTitle(document, "Tiempos de Completitud (días)");

        PdfPTable timeTable = new PdfPTable(2);
        timeTable.setWidthPercentage(80);
        timeTable.setWidths(new float[]{2f, 1f});
        timeTable.setSpacingBefore(10);
        timeTable.setSpacingAfter(15);
        timeTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addStatRow(timeTable, "Promedio:",
            String.format("%.0f días", stats.getAverageCompletionDays()));
        addStatRow(timeTable, "Más Rápida:",
            stats.getFastestCompletionDays() != null ?
            stats.getFastestCompletionDays() + " días" : "N/D");
        addStatRow(timeTable, "Más Lenta:",
            stats.getSlowestCompletionDays() != null ?
            stats.getSlowestCompletionDays() + " días" : "N/D");
        addStatRow(timeTable, "Mediana:",
            stats.getMedianCompletionDays() != null ?
            String.format("%.0f días", stats.getMedianCompletionDays()) : "N/D");

        document.add(timeTable);

        // NUEVO: Gráfico de distribución de tiempos
        addTimeDistributionChart(document, stats);

        // Calificaciones con gráfico visual
        addSubsectionTitle(document, "Calificaciones");

        PdfPTable gradeTable = new PdfPTable(2);
        gradeTable.setWidthPercentage(80);
        gradeTable.setWidths(new float[]{2f, 1f});
        gradeTable.setSpacingBefore(10);
        gradeTable.setSpacingAfter(15);
        gradeTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addStatRow(gradeTable, "Promedio General:",
            String.format("%.2f", stats.getAverageGrade()));
        addStatRow(gradeTable, "Calificación Más Alta:",
            stats.getHighestGrade() != null ?
            String.format("%.2f", stats.getHighestGrade()) : "N/D");
        addStatRow(gradeTable, "Calificación Más Baja:",
            stats.getLowestGrade() != null ?
            String.format("%.2f", stats.getLowestGrade()) : "N/D");
        addStatRow(gradeTable, "Mediana:",
            stats.getMedianGrade() != null ?
            String.format("%.2f", stats.getMedianGrade()) : "N/D");

        document.add(gradeTable);

        // NUEVO: Gráfico de distribución de calificaciones
        addGradeDistributionChart(document, stats);

        // Distinciones académicas con visualización mejorada
        addSubsectionTitle(document, "Distinciones Académicas");

        PdfPTable distinctionTable = new PdfPTable(3);
        distinctionTable.setWidthPercentage(90);
        distinctionTable.setSpacingBefore(10);
        distinctionTable.setSpacingAfter(15);
        distinctionTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addStatsCard(distinctionTable, "Meritoria",
            String.valueOf(stats.getWithMeritorious()), INSTITUTIONAL_GOLD);
        addStatsCard(distinctionTable, "Laureada",
            String.valueOf(stats.getWithLaudeate()), INSTITUTIONAL_GOLD);
        addStatsCard(distinctionTable, "Sin Distinción",
            String.valueOf(stats.getWithoutDistinction()), LIGHT_GOLD);

        document.add(distinctionTable);

        // NUEVO: Gráfico de distribución de distinciones
        addDistinctionDistributionChart(document, stats);
    }

    /**
     * Análisis por resultado
     */
    private void addResultAnalysis(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "3. ANÁLISIS POR RESULTADO");

        if (report.getResultAnalysis() == null) {
            document.add(new Paragraph("No hay análisis de resultado disponible.", NORMAL_FONT));
            return;
        }

        CompletedModalitiesReportDTO.ResultAnalysisDTO analysis = report.getResultAnalysis();

        // Comparativa visual
        PdfPTable comparisonTable = new PdfPTable(2);
        comparisonTable.setWidthPercentage(100);
        comparisonTable.setSpacingBefore(10);
        comparisonTable.setSpacingAfter(20);

        // Columna exitosas
        PdfPCell successCell = new PdfPCell();
        successCell.setBackgroundColor(new BaseColor(232, 245, 233));
        successCell.setPadding(15);
        successCell.setBorder(Rectangle.BOX);
        successCell.setBorderColor(INSTITUTIONAL_GOLD);
        successCell.setBorderWidth(2);

        Paragraph successTitle = new Paragraph("✓ EXITOSAS",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, INSTITUTIONAL_GOLD));
        successTitle.setAlignment(Element.ALIGN_CENTER);
        successCell.addElement(successTitle);
        successCell.addElement(new Paragraph("\n"));

        successCell.addElement(new Paragraph("Cantidad: " + analysis.getSuccessfulCount(), BOLD_FONT));
        successCell.addElement(new Paragraph("Tasa: " + String.format("%.1f%%", analysis.getSuccessRate()), NORMAL_FONT));
        successCell.addElement(new Paragraph("Calificación Promedio: " +
            String.format("%.2f", analysis.getAverageSuccessGrade()), NORMAL_FONT));
        successCell.addElement(new Paragraph("Días Promedio: " +
            String.format("%.0f", analysis.getAverageSuccessCompletionDays()), NORMAL_FONT));

        if (analysis.getSuccessFactors() != null && !analysis.getSuccessFactors().isEmpty()) {
            successCell.addElement(new Paragraph("\nFactores de Éxito:",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INSTITUTIONAL_GOLD)));
            for (String factor : analysis.getSuccessFactors()) {
                successCell.addElement(new Paragraph("• " + factor, SMALL_FONT));
            }
        }

        comparisonTable.addCell(successCell);

        // Columna fallidas
        PdfPCell failedCell = new PdfPCell();
        failedCell.setBackgroundColor(new BaseColor(255, 235, 238));
        failedCell.setPadding(15);
        failedCell.setBorder(Rectangle.BOX);
        failedCell.setBorderColor(INSTITUTIONAL_RED);
        failedCell.setBorderWidth(2);

        Paragraph failedTitle = new Paragraph("✗ FALLIDAS",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, INSTITUTIONAL_RED));
        failedTitle.setAlignment(Element.ALIGN_CENTER);
        failedCell.addElement(failedTitle);
        failedCell.addElement(new Paragraph("\n"));

        failedCell.addElement(new Paragraph("Cantidad: " + analysis.getFailedCount(), BOLD_FONT));
        failedCell.addElement(new Paragraph("Tasa: " + String.format("%.1f%%", analysis.getFailureRate()), NORMAL_FONT));
        failedCell.addElement(new Paragraph("Calificación Promedio: " +
            String.format("%.2f", analysis.getAverageFailureGrade()), NORMAL_FONT));
        failedCell.addElement(new Paragraph("Días Promedio: " +
            String.format("%.0f", analysis.getAverageFailureCompletionDays()), NORMAL_FONT));

        if (analysis.getFailureReasons() != null && !analysis.getFailureReasons().isEmpty()) {
            failedCell.addElement(new Paragraph("\nRazones de Fallo:",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INSTITUTIONAL_RED)));
            for (String reason : analysis.getFailureReasons()) {
                failedCell.addElement(new Paragraph("• " + reason, SMALL_FONT));
            }
        }

        comparisonTable.addCell(failedCell);
        document.add(comparisonTable);

        // Veredicto de desempeño
        if (analysis.getPerformanceVerdict() != null) {
            PdfPTable verdictTable = new PdfPTable(1);
            verdictTable.setWidthPercentage(80);
            verdictTable.setSpacingBefore(15);
            verdictTable.setSpacingAfter(15);
            verdictTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell verdictCell = new PdfPCell();
            BaseColor verdictColor = getVerdictColor(analysis.getPerformanceVerdict());
            verdictCell.setBackgroundColor(verdictColor);
            verdictCell.setPadding(12);
            verdictCell.setBorder(Rectangle.NO_BORDER);

            Paragraph verdictText = new Paragraph(
                "DESEMPEÑO: " + translateVerdict(analysis.getPerformanceVerdict()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, WHITE)
            );
            verdictText.setAlignment(Element.ALIGN_CENTER);
            verdictCell.addElement(verdictText);
            verdictTable.addCell(verdictCell);
            document.add(verdictTable);
        }

        // Recomendaciones
        if (analysis.getRecommendations() != null && !analysis.getRecommendations().isEmpty()) {
            addSubsectionTitle(document, "Recomendaciones");

            PdfPTable recTable = new PdfPTable(1);
            recTable.setWidthPercentage(95);
            recTable.setSpacingBefore(10);

            for (String rec : analysis.getRecommendations()) {
                PdfPCell recCell = new PdfPCell();
                recCell.setBackgroundColor(new BaseColor(255, 248, 225));
                recCell.setPadding(10);
                recCell.setBorder(Rectangle.NO_BORDER);
                recCell.setPhrase(new Phrase("→ " + rec, NORMAL_FONT));
                recTable.addCell(recCell);
            }

            document.add(recTable);
        }
    }

    // Continúa en el siguiente mensaje debido a límites de longitud...

    private BaseColor getVerdictColor(String verdict) {
        switch (verdict) {
            case "EXCELLENT": return INSTITUTIONAL_GOLD;
            case "GOOD": return INSTITUTIONAL_GOLD;
            case "REGULAR": return INSTITUTIONAL_RED;
            default: return INSTITUTIONAL_RED;
        }
    }

    private String translateVerdict(String verdict) {
        switch (verdict) {
            case "EXCELLENT": return "EXCELENTE";
            case "GOOD": return "BUENO";
            case "REGULAR": return "REGULAR";
            case "NEEDS_IMPROVEMENT": return "NECESITA MEJORA";
            default: return verdict;
        }
    }

    // Continuará en siguiente archivo...

    /**
     * Análisis por tipo de modalidad
     */
    private void addModalityTypeAnalysis(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "4. ANÁLISIS POR TIPO DE MODALIDAD");

        if (report.getModalityTypeAnalysis() == null || report.getModalityTypeAnalysis().isEmpty()) {
            document.add(new Paragraph("No hay análisis por tipo disponible.", NORMAL_FONT));
            return;
        }

        // NUEVO: Resumen con tarjetas
        addModalityTypesSummaryCards(document, report.getModalityTypeAnalysis());

        // NUEVO: Top 5 modalidades con mejor desempeño
        addTopModalitiesChart(document, report.getModalityTypeAnalysis());

        // Tabla de análisis detallada
        addSubsectionTitle(document, "Detalle por Tipo de Modalidad");

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 1f, 1f, 1f, 1f, 1.2f});
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);

        // Encabezados
        addTableHeader(table, "Tipo de Modalidad");
        addTableHeader(table, "Total");
        addTableHeader(table, "Exitosas");
        addTableHeader(table, "Fallidas");
        addTableHeader(table, "Tasa Éxito");
        addTableHeader(table, "Desempeño");

        // Datos
        boolean alternate = false;
        for (CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO analysis : report.getModalityTypeAnalysis()) {
            addTableCell(table, analysis.getModalityType(), alternate);
            addTableCell(table, String.valueOf(analysis.getTotalCompleted()), alternate);
            addTableCell(table, String.valueOf(analysis.getSuccessful()), alternate);
            addTableCell(table, String.valueOf(analysis.getFailed()), alternate);
            addTableCell(table, String.format("%.1f%%", analysis.getSuccessRate()), alternate);
            addTableCell(table, translatePerformance(analysis.getPerformance()), alternate);

            alternate = !alternate;
        }

        document.add(table);
    }

    private String translatePerformance(String performance) {
        switch (performance) {
            case "EXCELLENT": return "Excelente";
            case "GOOD": return "Bueno";
            case "REGULAR": return "Regular";
            case "POOR": return "Bajo";
            default: return performance;
        }
    }

    /**
     * Listado detallado de modalidades completadas
     */
    private void addCompletedModalitiesListing(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "5. LISTADO DETALLADO DE MODALIDADES COMPLETADAS");

        if (report.getCompletedModalities() == null || report.getCompletedModalities().isEmpty()) {
            document.add(new Paragraph("No hay modalidades para mostrar.", NORMAL_FONT));
            return;
        }

        // Tabla detallada
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.8f, 0.8f, 1.2f, 0.8f, 0.8f, 1.5f, 1f});
        table.setSpacingBefore(10);
        table.setHeaderRows(1);

        // Encabezados
        addTableHeader(table, "Modalidad");
        addTableHeader(table, "Resultado");
        addTableHeader(table, "Estudiantes");
        addTableHeader(table, "Calif.");
        addTableHeader(table, "Días");
        addTableHeader(table, "Director");
        addTableHeader(table, "Distinción");

        // Datos (limitar a primeros 50 para no sobrecargar)
        boolean alternate = false;
        int count = 0;
        for (CompletedModalitiesReportDTO.CompletedModalityDetailDTO detail : report.getCompletedModalities()) {
            if (count++ >= 50) break;

            addTableCell(table, truncate(detail.getModalityTypeName(), 25), alternate);

            PdfPCell resultCell = new PdfPCell(new Phrase(
                "SUCCESS".equals(detail.getResult()) ? "✓" : "✗",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9,
                    "SUCCESS".equals(detail.getResult()) ? INSTITUTIONAL_GOLD : INSTITUTIONAL_RED)
            ));
            resultCell.setBackgroundColor(alternate ? LIGHT_GOLD : WHITE);
            resultCell.setBorder(Rectangle.BOTTOM);
            resultCell.setBorderColor(LIGHT_GOLD);
            resultCell.setBorderWidth(0.3f);
            resultCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            resultCell.setPadding(5);
            table.addCell(resultCell);

            String studentNames = detail.getStudents() != null ?
                detail.getStudents().stream()
                    .map(s -> s.getFullName())
                    .collect(java.util.stream.Collectors.joining(", ")) : "N/D";
            addTableCell(table, truncate(studentNames, 25), alternate);

            addTableCell(table, detail.getFinalGrade() != null ?
                String.format("%.2f", detail.getFinalGrade()) : "N/D", alternate);
            addTableCell(table, detail.getCompletionDays() != null ?
                String.valueOf(detail.getCompletionDays()) : "N/D", alternate);
            addTableCell(table, detail.getDirectorName() != null ?
                truncate(detail.getDirectorName(), 20) : "Sin asignar", alternate);
            addTableCell(table, detail.getAcademicDistinction() != null ?
                translateDistinction(detail.getAcademicDistinction()) : "-", alternate);

            alternate = !alternate;
        }

        document.add(table);

        if (report.getCompletedModalities().size() > 50) {
            Paragraph note = new Paragraph(
                "* Se muestran las primeras 50 modalidades. Total: " + report.getCompletedModalities().size(),
                TINY_FONT
            );
            note.setSpacingBefore(5);
            document.add(note);
        }
    }

    private String translateDistinction(String distinction) {
        switch (distinction) {
            case "MERITORIOUS": return "Meritoria";
            case "LAUREATE": return "Laureada";
            default: return distinction;
        }
    }

    /**
     * Análisis temporal
     */
    private void addTemporalAnalysis(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "6. ANÁLISIS TEMPORAL");

        if (report.getTemporalAnalysis() == null) {
            document.add(new Paragraph("No hay análisis temporal disponible.", NORMAL_FONT));
            return;
        }

        CompletedModalitiesReportDTO.TemporalAnalysisDTO temporal = report.getTemporalAnalysis();

        // NUEVO: Tarjetas de resumen de tendencia
        addTemporalSummaryCards(document, temporal);

        // Indicadores de tendencia mejorados
        PdfPTable trendTable = new PdfPTable(4);
        trendTable.setWidthPercentage(100);
        trendTable.setSpacingBefore(10);
        trendTable.setSpacingAfter(20);

        addStatsCard(trendTable, "Tendencia",
            translateTrend(temporal.getTrend()), getTrendColor(temporal.getTrend()));
        addStatsCard(trendTable, "Tasa de Crecimiento",
            String.format("%.1f%%", temporal.getGrowthRate()), INSTITUTIONAL_GOLD);
        addStatsCard(trendTable, "Mejor Periodo",
            temporal.getBestPeriod() != null ? temporal.getBestPeriod() : "N/D", INSTITUTIONAL_GOLD);
        addStatsCard(trendTable, "Peor Periodo",
            temporal.getWorstPeriod() != null ? temporal.getWorstPeriod() : "N/D", INSTITUTIONAL_RED);

        document.add(trendTable);

        // Tabla de datos por periodo
        if (temporal.getPeriodData() != null && !temporal.getPeriodData().isEmpty()) {
            addSubsectionTitle(document, "Datos por Periodo Académico");

            PdfPTable periodTable = new PdfPTable(6);
            periodTable.setWidthPercentage(100);
            periodTable.setWidths(new float[]{1.2f, 1f, 1f, 1f, 1.2f, 1f});
            periodTable.setSpacingBefore(10);

            addTableHeader(periodTable, "Periodo");
            addTableHeader(periodTable, "Completadas");
            addTableHeader(periodTable, "Exitosas");
            addTableHeader(periodTable, "Fallidas");
            addTableHeader(periodTable, "Tasa Éxito");
            addTableHeader(periodTable, "Calif. Prom.");

            boolean alternate = false;
            for (CompletedModalitiesReportDTO.PeriodDataDTO period : temporal.getPeriodData()) {
                addTableCell(periodTable, period.getPeriod(), alternate);
                addTableCell(periodTable, String.valueOf(period.getCompleted()), alternate);
                addTableCell(periodTable, String.valueOf(period.getSuccessful()), alternate);
                addTableCell(periodTable, String.valueOf(period.getFailed()), alternate);
                addTableCell(periodTable, String.format("%.1f%%", period.getSuccessRate()), alternate);
                addTableCell(periodTable, String.format("%.2f", period.getAverageGrade()), alternate);

                alternate = !alternate;
            }

            document.add(periodTable);

            // NUEVO: Gráfico visual de evolución temporal
            addTemporalEvolutionChart(document, temporal);
        }
    }

    private String translateTrend(String trend) {
        switch (trend) {
            case "IMPROVING": return "Mejorando";
            case "STABLE": return "Estable";
            case "DECLINING": return "Declinando";
            default: return trend;
        }
    }

    private BaseColor getTrendColor(String trend) {
        switch (trend) {
            case "IMPROVING": return INSTITUTIONAL_GOLD;
            case "STABLE": return INSTITUTIONAL_GOLD;
            case "DECLINING": return INSTITUTIONAL_RED;
            default: return LIGHT_GOLD;
        }
    }

    /**
     * Desempeño de directores
     */
    private void addDirectorPerformance(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "7. DESEMPEÑO DE DIRECTORES");

        if (report.getDirectorPerformance() == null) {
            document.add(new Paragraph("No hay datos de desempeño de directores.", NORMAL_FONT));
            return;
        }

        CompletedModalitiesReportDTO.DirectorPerformanceDTO performance = report.getDirectorPerformance();

        // Indicadores generales
        PdfPTable indicatorsTable = new PdfPTable(3);
        indicatorsTable.setWidthPercentage(90);
        indicatorsTable.setSpacingBefore(10);
        indicatorsTable.setSpacingAfter(20);
        indicatorsTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addStatsCard(indicatorsTable, "Total Directores",
            String.valueOf(performance.getTotalDirectors()), INSTITUTIONAL_RED);
        addStatsCard(indicatorsTable, "Tasa Éxito Prom.",
            String.format("%.1f%%", performance.getAverageSuccessRateByDirector()), INSTITUTIONAL_GOLD);
        addStatsCard(indicatorsTable, "Mejor Director",
            performance.getBestDirector() != null ?
            truncate(performance.getBestDirector(), 15) : "N/D", INSTITUTIONAL_GOLD);

        document.add(indicatorsTable);

        // Top directores
        if (performance.getTopDirectors() != null && !performance.getTopDirectors().isEmpty()) {
            addSubsectionTitle(document, "Top 10 Directores por Desempeño");

            PdfPTable directorTable = new PdfPTable(6);
            directorTable.setWidthPercentage(100);
            directorTable.setWidths(new float[]{2.5f, 1f, 1f, 1f, 1.2f, 1f});
            directorTable.setSpacingBefore(10);

            addTableHeader(directorTable, "Director");
            addTableHeader(directorTable, "Total");
            addTableHeader(directorTable, "Exitosas");
            addTableHeader(directorTable, "Fallidas");
            addTableHeader(directorTable, "Tasa Éxito");
            addTableHeader(directorTable, "Distinciones");

            boolean alternate = false;
            for (CompletedModalitiesReportDTO.TopDirectorDTO director : performance.getTopDirectors()) {
                addTableCell(directorTable, truncate(director.getDirectorName(), 30), alternate);
                addTableCell(directorTable, String.valueOf(director.getTotalSupervised()), alternate);
                addTableCell(directorTable, String.valueOf(director.getSuccessful()), alternate);
                addTableCell(directorTable, String.valueOf(director.getFailed()), alternate);
                addTableCell(directorTable, String.format("%.1f%%", director.getSuccessRate()), alternate);
                addTableCell(directorTable, String.valueOf(director.getWithDistinction()), alternate);

                alternate = !alternate;
            }

            document.add(directorTable);
        }
    }

    /**
     * Análisis de distinciones académicas
     */
    private void addDistinctionAnalysis(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "8. ANÁLISIS DE DISTINCIONES ACADÉMICAS");

        if (report.getDistinctionAnalysis() == null) {
            document.add(new Paragraph("No hay análisis de distinciones disponible.", NORMAL_FONT));
            return;
        }

        CompletedModalitiesReportDTO.DistinctionAnalysisDTO distinction = report.getDistinctionAnalysis();

        // Indicadores principales
        PdfPTable indicatorsTable = new PdfPTable(4);
        indicatorsTable.setWidthPercentage(100);
        indicatorsTable.setSpacingBefore(10);
        indicatorsTable.setSpacingAfter(20);

        addStatsCard(indicatorsTable, "Total con Distinción",
            String.valueOf(distinction.getTotalWithDistinction()), INSTITUTIONAL_GOLD);
        addStatsCard(indicatorsTable, "Meritorias",
            String.valueOf(distinction.getMeritorious()), new BaseColor(255, 152, 0));
        addStatsCard(indicatorsTable, "Laureadas",
            String.valueOf(distinction.getLaureate()), INSTITUTIONAL_GOLD);
        addStatsCard(indicatorsTable, "Tasa Distinción",
            String.format("%.1f%%", distinction.getDistinctionRate()), INSTITUTIONAL_GOLD);

        document.add(indicatorsTable);

        // Modalidades con más distinciones
        if (distinction.getModalitiesWithMostDistinctions() != null &&
            !distinction.getModalitiesWithMostDistinctions().isEmpty()) {

            addSubsectionTitle(document, "Modalidades con Más Distinciones");

            PdfPTable modalityTable = new PdfPTable(1);
            modalityTable.setWidthPercentage(90);
            modalityTable.setSpacingBefore(10);
            modalityTable.setSpacingAfter(15);
            modalityTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            for (String modality : distinction.getModalitiesWithMostDistinctions()) {
                PdfPCell cell = new PdfPCell(new Phrase("★ " + modality, NORMAL_FONT));
                cell.setBackgroundColor(new BaseColor(255, 248, 225));
                cell.setPadding(8);
                cell.setBorder(Rectangle.NO_BORDER);
                modalityTable.addCell(cell);
            }

            document.add(modalityTable);
        }

        // Directores con más distinciones
        if (distinction.getDirectorsWithMostDistinctions() != null &&
            !distinction.getDirectorsWithMostDistinctions().isEmpty()) {

            addSubsectionTitle(document, "Directores con Más Distinciones");

            PdfPTable directorTable = new PdfPTable(1);
            directorTable.setWidthPercentage(90);
            directorTable.setSpacingBefore(10);
            directorTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            for (String director : distinction.getDirectorsWithMostDistinctions()) {
                PdfPCell cell = new PdfPCell(new Phrase("★ " + director, NORMAL_FONT));
                cell.setBackgroundColor(LIGHT_GOLD);
                cell.setPadding(8);
                cell.setBorder(Rectangle.NO_BORDER);
                directorTable.addCell(cell);
            }

            document.add(directorTable);
        }
    }

    // ==================== NUEVOS MÉTODOS PARA VISUALIZACIONES MEJORADAS ====================

    /**
     * Agregar tarjetas de resumen de estadísticas generales con iconos
     */
    private void addGeneralStatsSummaryCards(Document document,
                                            CompletedModalitiesReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        PdfPTable cardsTable = new PdfPTable(4);
        cardsTable.setWidthPercentage(100);
        cardsTable.setSpacingBefore(10);
        cardsTable.setSpacingAfter(20);

        // Total completadas
        addSummaryCardWithIcon(cardsTable, "Total Completadas",
                String.valueOf(stats.getTotalCompleted()), INSTITUTIONAL_GOLD);

        // Tasa de aprobación
        addSummaryCardWithIcon(cardsTable, "Tasa Aprobación",
                String.format("%.1f%%", stats.getApprovalRate()), INSTITUTIONAL_GOLD);

        // Calificación promedio
        addSummaryCardWithIcon(cardsTable, "Calificación Promedio",
                String.format("%.2f", stats.getAverageGrade()), INSTITUTIONAL_RED);

        // Días promedio
        addSummaryCardWithIcon(cardsTable, "Días Promedio",
                String.format("%.0f", stats.getAverageCompletionDays()), INSTITUTIONAL_RED);

        document.add(cardsTable);
    }

    /**
     * Agregar tarjeta individual (sin icono - emojis no soportados en iText 5)
     */
    private void addSummaryCardWithIcon(PdfPTable table, String label, String value,
                                        BaseColor color) {
        PdfPCell card = new PdfPCell();
        card.setPadding(15);
        card.setBorderColor(color);
        card.setBorderWidth(2f);
        card.setBackgroundColor(WHITE);
        card.setFixedHeight(70);

        // Valor grande (número principal)
        Paragraph valuePara = new Paragraph(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, color));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        valuePara.setSpacingAfter(5);
        card.addElement(valuePara);

        // Etiqueta descriptiva
        Paragraph labelPara = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_BLACK));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        card.addElement(labelPara);

        table.addCell(card);
    }

    /**
     * Gráfico visual de tasa de aprobación
     */
    private void addApprovalRateChart(Document document,
                                     CompletedModalitiesReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        addSubsectionTitle(document, "Visualización de Tasa de Aprobación");

        int approved = stats.getApproved();
        int failed = stats.getFailed();
        int total = approved + failed;

        if (total == 0) return;

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(20);

        // Barra de aprobados
        addApprovalBar(chartTable, "Aprobadas", approved, total, INSTITUTIONAL_GOLD);

        // Barra de reprobados
        addApprovalBar(chartTable, "Reprobadas", failed, total, INSTITUTIONAL_RED);

        document.add(chartTable);
    }

    /**
     * Agregar barra de aprobación
     */
    private void addApprovalBar(PdfPTable table, String label, int count, int total, BaseColor color) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(4);
        containerCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{1.5f, 4f, 1.5f});
        } catch (DocumentException e) {
            // Ignorar
        }

        // Etiqueta
        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_BLACK)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPadding(3);
        innerTable.addCell(labelCell);

        // Barra
        float percentage = total > 0 ? (float) count / total : 0;
        PdfPCell barCell = createProgressBar(count, percentage, color);
        innerTable.addCell(barCell);

        // Valor y porcentaje
        PdfPCell valueCell = new PdfPCell();
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setPadding(3);

        Paragraph valueContent = new Paragraph();
        valueContent.add(new Chunk(count + " ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, color)));
        valueContent.add(new Chunk("(" + String.format("%.1f%%", percentage * 100) + ")",
                FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_GRAY)));
        valueContent.setAlignment(Element.ALIGN_CENTER);
        valueCell.addElement(valueContent);
        innerTable.addCell(valueCell);

        containerCell.addElement(innerTable);
        table.addCell(containerCell);
    }

    /**
     * Crear barra de progreso
     */
    private PdfPCell createProgressBar(int value, float percentage, BaseColor color) {
        PdfPTable barContainer = new PdfPTable(2);
        float barWidth = Math.max(percentage * 100, 3);
        float emptyWidth = 100 - barWidth;

        try {
            barContainer.setWidths(new float[]{barWidth, emptyWidth});
        } catch (DocumentException e) {
            try {
                barContainer.setWidths(new float[]{50, 50});
            } catch (DocumentException ex) {
                // Ignorar
            }
        }
        barContainer.setWidthPercentage(100);

        // Parte coloreada
        PdfPCell filledCell = new PdfPCell(new Phrase(String.valueOf(value),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, WHITE)));
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(5);
        filledCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        barContainer.addCell(filledCell);

        // Parte vacía
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(LIGHT_GOLD);
        emptyCell.setBorder(Rectangle.NO_BORDER);
        barContainer.addCell(emptyCell);

        PdfPCell containerCell = new PdfPCell();
        containerCell.addElement(barContainer);
        containerCell.setBorder(Rectangle.BOX);
        containerCell.setBorderColor(color);
        containerCell.setBorderWidth(0.5f);
        containerCell.setPadding(0);

        return containerCell;
    }

    /**
     * Gráfico de distribución de tiempos de completitud
     */
    private void addTimeDistributionChart(Document document,
                                         CompletedModalitiesReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        addSubsectionTitle(document, "Distribución de Tiempos");

        Integer fastest = stats.getFastestCompletionDays();
        Double average = stats.getAverageCompletionDays();
        Integer slowest = stats.getSlowestCompletionDays();

        if (fastest == null || slowest == null) return;

        int maxValue = slowest;

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(15);

        // Más rápida
        addTimeBar(chartTable, "Más Rápida", fastest, maxValue, INSTITUTIONAL_GOLD);

        // Promedio
        addTimeBar(chartTable, "Promedio", average.intValue(), maxValue, INSTITUTIONAL_GOLD);

        // Más lenta
        addTimeBar(chartTable, "Más Lenta", slowest, maxValue, INSTITUTIONAL_RED);

        document.add(chartTable);
    }

    /**
     * Agregar barra de tiempo
     */
    private void addTimeBar(PdfPTable table, String label, int days, int maxDays, BaseColor color) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(3);
        containerCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{1.5f, 4f, 1f});
        } catch (DocumentException e) {
            // Ignorar
        }

        // Etiqueta
        PdfPCell labelCell = new PdfPCell(new Phrase(label, SMALL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPadding(3);
        innerTable.addCell(labelCell);

        // Barra
        float percentage = maxDays > 0 ? (float) days / maxDays : 0;
        PdfPCell barCell = createProgressBar(days, percentage, color);
        innerTable.addCell(barCell);

        // Valor
        PdfPCell valueCell = new PdfPCell(new Phrase(days + " días",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, color)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setPadding(3);
        innerTable.addCell(valueCell);

        containerCell.addElement(innerTable);
        table.addCell(containerCell);
    }

    /**
     * Gráfico de distribución de calificaciones
     */
    private void addGradeDistributionChart(Document document,
                                          CompletedModalitiesReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        addSubsectionTitle(document, "Distribución de Calificaciones");

        Double lowest = stats.getLowestGrade();
        Double average = stats.getAverageGrade();
        Double highest = stats.getHighestGrade();

        if (lowest == null || highest == null) return;

        double maxValue = 5.0; // Escala máxima

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(15);

        // Más baja
        addGradeBar(chartTable, "Más Baja", lowest, maxValue, INSTITUTIONAL_RED);

        // Promedio
        addGradeBar(chartTable, "Promedio", average, maxValue, INSTITUTIONAL_GOLD);

        // Más alta
        addGradeBar(chartTable, "Más Alta", highest, maxValue, INSTITUTIONAL_GOLD);

        document.add(chartTable);
    }

    /**
     * Agregar barra de calificación
     */
    private void addGradeBar(PdfPTable table, String label, double grade, double maxGrade, BaseColor color) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(3);
        containerCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{1.5f, 4f, 1f});
        } catch (DocumentException e) {
            // Ignorar
        }

        // Etiqueta
        PdfPCell labelCell = new PdfPCell(new Phrase(label, SMALL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPadding(3);
        innerTable.addCell(labelCell);

        // Barra
        float percentage = (float) (grade / maxGrade);
        PdfPCell barCell = createGradeProgressBar(grade, percentage, color);
        innerTable.addCell(barCell);

        // Valor
        PdfPCell valueCell = new PdfPCell(new Phrase(String.format("%.2f", grade),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, color)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setPadding(3);
        innerTable.addCell(valueCell);

        containerCell.addElement(innerTable);
        table.addCell(containerCell);
    }

    /**
     * Crear barra de progreso de calificación
     */
    private PdfPCell createGradeProgressBar(double grade, float percentage, BaseColor color) {
        PdfPTable barContainer = new PdfPTable(2);
        float barWidth = Math.max(percentage * 100, 3);
        float emptyWidth = 100 - barWidth;

        try {
            barContainer.setWidths(new float[]{barWidth, emptyWidth});
        } catch (DocumentException e) {
            try {
                barContainer.setWidths(new float[]{50, 50});
            } catch (DocumentException ex) {
                // Ignorar
            }
        }
        barContainer.setWidthPercentage(100);

        // Parte coloreada
        PdfPCell filledCell = new PdfPCell(new Phrase(String.format("%.2f", grade),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, WHITE)));
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(5);
        filledCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        barContainer.addCell(filledCell);

        // Parte vacía
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(LIGHT_GOLD);
        emptyCell.setBorder(Rectangle.NO_BORDER);
        barContainer.addCell(emptyCell);

        PdfPCell containerCell = new PdfPCell();
        containerCell.addElement(barContainer);
        containerCell.setBorder(Rectangle.BOX);
        containerCell.setBorderColor(color);
        containerCell.setBorderWidth(0.5f);
        containerCell.setPadding(0);

        return containerCell;
    }

    /**
     * Gráfico de distribución de distinciones
     */
    private void addDistinctionDistributionChart(Document document,
                                                CompletedModalitiesReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        addSubsectionTitle(document, "Gráfico de Distinciones");

        int meritorious = stats.getWithMeritorious();
        int laureate = stats.getWithLaudeate();
        int without = stats.getWithoutDistinction();
        int total = meritorious + laureate + without;

        if (total == 0) return;

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(15);

        // Meritoria
        addDistinctionBar(chartTable, "Meritoria", meritorious, total,
                new BaseColor(255, 152, 0)); // Naranja

        // Laureada
        addDistinctionBar(chartTable, "Laureada", laureate, total, INSTITUTIONAL_GOLD);

        // Sin distinción
        addDistinctionBar(chartTable, "Sin Distinción", without, total, LIGHT_GOLD);

        document.add(chartTable);
    }

    /**
     * Agregar barra de distinción
     */
    private void addDistinctionBar(PdfPTable table, String label, int count, int total, BaseColor color) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(3);
        containerCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{1.5f, 4f, 1.5f});
        } catch (DocumentException e) {
            // Ignorar
        }

        // Etiqueta
        PdfPCell labelCell = new PdfPCell(new Phrase(label, SMALL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPadding(3);
        innerTable.addCell(labelCell);

        // Barra
        float percentage = total > 0 ? (float) count / total : 0;
        PdfPCell barCell = createProgressBar(count, percentage, color);
        innerTable.addCell(barCell);

        // Valor y porcentaje
        PdfPCell valueCell = new PdfPCell();
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setPadding(3);

        Paragraph valueContent = new Paragraph();
        valueContent.add(new Chunk(count + " ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, color)));
        valueContent.add(new Chunk("(" + String.format("%.1f%%", percentage * 100) + ")",
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_GRAY)));
        valueContent.setAlignment(Element.ALIGN_CENTER);
        valueCell.addElement(valueContent);
        innerTable.addCell(valueCell);

        containerCell.addElement(innerTable);
        table.addCell(containerCell);
    }

    /**
     * Agregar tarjetas de resumen de tipos de modalidad
     */
    private void addModalityTypesSummaryCards(Document document,
                                             List<CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO> analysis)
            throws DocumentException {

        PdfPTable cardsTable = new PdfPTable(3);
        cardsTable.setWidthPercentage(100);
        cardsTable.setSpacingBefore(10);
        cardsTable.setSpacingAfter(20);

        // Total de tipos
        addSummaryCardWithIcon(cardsTable, "Tipos Diferentes",
                String.valueOf(analysis.size()), INSTITUTIONAL_GOLD);

        // Mejor tasa de éxito
        double bestRate = analysis.stream()
                .mapToDouble(CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO::getSuccessRate)
                .max().orElse(0);
        addSummaryCardWithIcon(cardsTable, "Mejor Tasa Éxito",
                String.format("%.1f%%", bestRate), INSTITUTIONAL_GOLD);

        // Total completadas
        int totalCompleted = analysis.stream()
                .mapToInt(CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO::getTotalCompleted)
                .sum();
        addSummaryCardWithIcon(cardsTable, "Total Completadas",
                String.valueOf(totalCompleted), INSTITUTIONAL_RED);

        document.add(cardsTable);
    }

    /**
     * Gráfico de Top 5 modalidades con mejor desempeño
     */
    private void addTopModalitiesChart(Document document,
                                      List<CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO> analysis)
            throws DocumentException {

        addSubsectionTitle(document, "Top 5 Modalidades por Tasa de Éxito");

        // Ordenar por tasa de éxito
        List<CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO> topModalities = analysis.stream()
                .sorted(Comparator.comparingDouble(
                        CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO::getSuccessRate).reversed())
                .limit(5)
                .collect(java.util.stream.Collectors.toList());

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(20);

        int position = 1;
        for (CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO modality : topModalities) {
            addModalityRankingBar(chartTable, position++, modality);
        }

        document.add(chartTable);
    }

    /**
     * Agregar barra de ranking de modalidad
     */
    private void addModalityRankingBar(PdfPTable table, int position,
                                      CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO modality) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(5);
        containerCell.setBorder(Rectangle.NO_BORDER);

        // Encabezado con posición y nombre
        PdfPCell headerCell = new PdfPCell();
        BaseColor rankColor = position == 1 ? INSTITUTIONAL_GOLD : INSTITUTIONAL_RED;
        headerCell.setBackgroundColor(rankColor);
        headerCell.setPadding(6);
        headerCell.setBorder(Rectangle.NO_BORDER);

        String rankIcon = position + "º";
        Paragraph headerText = new Paragraph(rankIcon + " " + truncate(modality.getModalityType(), 40),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, WHITE));
        headerCell.addElement(headerText);

        // Información en tabla interna
        PdfPTable infoTable = new PdfPTable(4);
        try {
            infoTable.setWidths(new float[]{25, 25, 25, 25});
        } catch (DocumentException e) {
            // Ignorar
        }
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(3);

        addModalityInfoCell(infoTable, "Total: " + modality.getTotalCompleted());
        addModalityInfoCell(infoTable, "Exitosas: " + modality.getSuccessful());
        addModalityInfoCell(infoTable, "Fallidas: " + modality.getFailed());
        addModalityInfoCell(infoTable, "Tasa: " + String.format("%.1f%%", modality.getSuccessRate()));

        PdfPCell mainCell = new PdfPCell();
        mainCell.setBorder(Rectangle.BOX);
        mainCell.setBorderColor(rankColor);
        mainCell.setBorderWidth(1f);
        mainCell.setPadding(0);

        mainCell.addElement(headerCell);
        mainCell.addElement(infoTable);

        table.addCell(mainCell);
    }

    /**
     * Agregar celda de información de modalidad
     */
    private void addModalityInfoCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_BLACK)));
        cell.setBackgroundColor(LIGHT_GOLD);
        cell.setPadding(4);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    /**
     * Agregar tarjetas de resumen temporal
     */
    private void addTemporalSummaryCards(Document document,
                                        CompletedModalitiesReportDTO.TemporalAnalysisDTO temporal)
            throws DocumentException {

        PdfPTable cardsTable = new PdfPTable(4);
        cardsTable.setWidthPercentage(100);
        cardsTable.setSpacingBefore(10);
        cardsTable.setSpacingAfter(20);

        // Tendencia
        String trendIcon = getTrendIcon(temporal.getTrend());
        addSummaryCardWithIcon(cardsTable, "Tendencia",
                trendIcon + " " + translateTrend(temporal.getTrend()),
                getTrendColor(temporal.getTrend()));

        // Tasa de crecimiento
        addSummaryCardWithIcon(cardsTable, "Crecimiento",
                String.format("%+.1f%%", temporal.getGrowthRate()),
                INSTITUTIONAL_GOLD);

        // Total periodos
        int totalPeriods = temporal.getPeriodData() != null ? temporal.getPeriodData().size() : 0;
        addSummaryCardWithIcon(cardsTable, "Periodos Analizados",
                String.valueOf(totalPeriods), INSTITUTIONAL_RED);

        // Total completadas
        int totalCompleted = temporal.getPeriodData() != null ?
                temporal.getPeriodData().stream()
                        .mapToInt(CompletedModalitiesReportDTO.PeriodDataDTO::getCompleted)
                        .sum() : 0;
        addSummaryCardWithIcon(cardsTable, "Total Completadas",
                String.valueOf(totalCompleted), INSTITUTIONAL_GOLD);

        document.add(cardsTable);
    }

    /**
     * Obtener icono según tendencia
     */
    private String getTrendIcon(String trend) {
        switch (trend) {
            case "IMPROVING": return "↗";
            case "STABLE": return "→";
            case "DECLINING": return "↘";
            default: return "→";
        }
    }

    /**
     * Gráfico de evolución temporal
     */
    private void addTemporalEvolutionChart(Document document,
                                          CompletedModalitiesReportDTO.TemporalAnalysisDTO temporal)
            throws DocumentException {

        if (temporal.getPeriodData() == null || temporal.getPeriodData().isEmpty()) return;

        addSubsectionTitle(document, "Evolución de Modalidades Completadas por Periodo");

        List<CompletedModalitiesReportDTO.PeriodDataDTO> periods = temporal.getPeriodData();
        int maxCompleted = periods.stream()
                .mapToInt(CompletedModalitiesReportDTO.PeriodDataDTO::getCompleted)
                .max()
                .orElse(1);

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(15);

        for (CompletedModalitiesReportDTO.PeriodDataDTO period : periods) {
            addPeriodEvolutionBar(chartTable, period, maxCompleted);
        }

        document.add(chartTable);
    }

    /**
     * Agregar barra de evolución de periodo
     */
    private void addPeriodEvolutionBar(PdfPTable table,
                                      CompletedModalitiesReportDTO.PeriodDataDTO period,
                                      int maxValue) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(3);
        containerCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{1.2f, 4f, 1.5f});
        } catch (DocumentException e) {
            // Ignorar
        }

        // Periodo
        PdfPCell periodCell = new PdfPCell(new Phrase(period.getPeriod(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_BLACK)));
        periodCell.setBorder(Rectangle.NO_BORDER);
        periodCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        periodCell.setPadding(3);
        innerTable.addCell(periodCell);

        // Barra
        float percentage = maxValue > 0 ? (float) period.getCompleted() / maxValue : 0;
        BaseColor barColor = period.getSuccessRate() >= 70 ? INSTITUTIONAL_GOLD : INSTITUTIONAL_RED;
        PdfPCell barCell = createProgressBar(period.getCompleted(), percentage, barColor);
        innerTable.addCell(barCell);

        // Info
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        infoCell.setPadding(3);

        Paragraph infoContent = new Paragraph();
        infoContent.add(new Chunk(period.getCompleted() + " total",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, barColor)));
        infoContent.add(new Chunk(" | " + String.format("%.1f%%", period.getSuccessRate()),
                FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_GRAY)));
        infoCell.addElement(infoContent);
        innerTable.addCell(infoCell);

        containerCell.addElement(innerTable);
        table.addCell(containerCell);
    }

    // ==================== FIN DE NUEVOS MÉTODOS ====================

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Tarjeta de métrica
     */
    private void addMetricCard(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(color);
        cell.setPadding(10);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(60);

        Paragraph content = new Paragraph();
        content.add(new Chunk(value + "\n",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, WHITE)));
        content.add(new Chunk(label,
            FontFactory.getFont(FontFactory.HELVETICA, 8, new BaseColor(240, 240, 240))));
        content.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(content);
        table.addCell(cell);
    }

    /**
     * Tarjeta de estadística
     */
    private void addStatsCard(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(color);
        cell.setPadding(12);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(55);

        Paragraph content = new Paragraph();
        content.add(new Chunk(value + "\n",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, WHITE)));
        content.add(new Chunk(label,
            FontFactory.getFont(FontFactory.HELVETICA, 8, new BaseColor(240, 240, 240))));
        content.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(content);
        table.addCell(cell);
    }

    /**
     * Fila de información
     */
    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setPadding(8);
        labelCell.setBackgroundColor(LIGHT_GOLD);
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setPadding(8);
        valueCell.setBackgroundColor(WHITE);
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }

    /**
     * Fila de estadística
     */
    private void addStatRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setPadding(8);
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, BOLD_FONT));
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setBackgroundColor(LIGHT_GOLD);
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }

    /**
     * Encabezado de tabla
     */
    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setPadding(6);
        cell.setBackgroundColor(INSTITUTIONAL_RED);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    /**
     * Celda de tabla
     */
    private void addTableCell(PdfPTable table, String text, boolean alternate) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", TABLE_FONT));
        cell.setPadding(5);
        cell.setBackgroundColor(alternate ? LIGHT_GOLD : WHITE);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(LIGHT_GOLD);
        cell.setBorderWidth(0.3f);
        table.addCell(cell);
    }

    /**
     * Título de sección
     */
    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph section = new Paragraph(title, HEADER_FONT);
        section.setSpacingBefore(15);
        section.setSpacingAfter(8);
        document.add(section);
        InstitutionalPdfHeader.addGoldLine(document);
        Paragraph gap = new Paragraph(" ");
        gap.setSpacingAfter(6f);
        document.add(gap);
    }

    /**
     * Título de subsección
     */
    private void addSubsectionTitle(Document document, String title) throws DocumentException {
        Paragraph subsection = new Paragraph(title, SUBHEADER_FONT);
        subsection.setSpacingBefore(10);
        subsection.setSpacingAfter(8);
        document.add(subsection);
    }

    /**
     * Trunca texto
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

    // ==================== MÉTODOS INSTITUCIONALES ====================

    /**
     * Encabezado compacto institucional para páginas internas.
     */
    private void addInternalHeader(Document document, CompletedModalitiesReportDTO report) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setSpacingAfter(8f);
        try { header.setWidths(new float[]{65f, 35f}); } catch (DocumentException ignored) {}

        PdfPCell leftCell = new PdfPCell(new Phrase(
                "UNIVERSIDAD SURCOLOMBIANA — SIGMA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INSTITUTIONAL_RED)));
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        leftCell.setPaddingBottom(4f);
        header.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell(new Phrase(
                report.getAcademicProgramName(),
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY)));
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        rightCell.setPaddingBottom(4f);
        header.addCell(rightCell);

        document.add(header);
        InstitutionalPdfHeader.addGoldLine(document);

        Paragraph gap = new Paragraph(" ");
        gap.setSpacingAfter(6f);
        document.add(gap);
    }

    /**
     * Cierre institucional al final del reporte.
     */
    private void addInstitutionalClosing(Document document, CompletedModalitiesReportDTO report) throws DocumentException {
        Paragraph gap = new Paragraph(" ");
        gap.setSpacingBefore(20f);
        document.add(gap);

        InstitutionalPdfHeader.addRedLine(document);
        InstitutionalPdfHeader.addGoldLine(document);

        PdfPTable closingTable = new PdfPTable(1);
        closingTable.setWidthPercentage(100);
        closingTable.setSpacingBefore(8f);

        PdfPCell closingCell = new PdfPCell();
        closingCell.setBackgroundColor(LIGHT_GOLD);
        closingCell.setPadding(12f);
        closingCell.setBorder(Rectangle.NO_BORDER);

        Paragraph closingText = new Paragraph(
                "Documento generado automáticamente por el Sistema SIGMA.\n" +
                "Universidad Surcolombiana | Facultad de Ingeniería | Neiva – Huila\n" +
                "www.usco.edu.co  •  NIT: 891180084-2",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY));
        closingText.setAlignment(Element.ALIGN_CENTER);
        closingCell.addElement(closingText);
        closingTable.addCell(closingCell);
        document.add(closingTable);
    }

    // ==================== CLASE INTERNA: PAGE EVENT HELPER ====================

    /**
     * Helper para eventos de página — pie institucional
     */
    private static class CompletedModalitiesPageEventHelper extends PdfPageEventHelper {

        private final CompletedModalitiesReportDTO report;
        private static final BaseColor GOLD = new BaseColor(213, 203, 160);
        private static final BaseColor RED  = new BaseColor(143, 30, 30);
        private static final BaseColor GRAY = new BaseColor(80, 80, 80);

        public CompletedModalitiesPageEventHelper(CompletedModalitiesReportDTO report) {
            this.report = report;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float left   = document.leftMargin();
            float right  = document.right();
            float bottom = document.bottom() - 15f;

            // Línea dorada sobre el pie
            cb.setLineWidth(1f);
            cb.setColorStroke(GOLD);
            cb.moveTo(left, bottom + 10f);
            cb.lineTo(right, bottom + 10f);
            cb.stroke();

            // Izquierda: sistema
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("SIGMA — Universidad Surcolombiana",
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, RED)),
                    left, bottom, 0);

            // Centro: programa
            String progName = (report != null && report.getAcademicProgramName() != null)
                    ? report.getAcademicProgramName() : "";
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(progName,
                            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, GRAY)),
                    (left + right) / 2f, bottom, 0);

            // Derecha: número de página
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Pág. " + writer.getPageNumber(),
                            FontFactory.getFont(FontFactory.HELVETICA, 8, GRAY)),
                    right, bottom, 0);
        }
    }
}

