package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.report.dto.CompletedModalitiesReportDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Servicio para generar PDF del reporte de modalidades completadas
 * Diseño profesional e institucional con análisis completo de resultados
 */
@Service
public class CompletedModalitiesPdfGenerator extends BaseReportPdfGenerator {

    public ByteArrayOutputStream generatePDF(CompletedModalitiesReportDTO report) throws DocumentException, IOException {
        PdfSession session = openDocument(PageSize.A4, 50, 50, 50, 50, report.getAcademicProgramName(), null);
        // IOException propagada desde addCoverPage (carga del logo institucional)

        // Validación de datos
        if (report == null) {
            session.document().add(new Paragraph("No hay datos para generar el reporte.", InstitutionalPdfHeader.NORMAL_FONT));
            close(session);
            return session.out();
        }
        if (report.getCompletedModalities() == null || report.getCompletedModalities().isEmpty()) {
            session.document().add(new Paragraph("No hay modalidades completadas para mostrar.", InstitutionalPdfHeader.NORMAL_FONT));
            close(session);
            return session.out();
        }

        // 1. Portada
        addCoverPage(session.document(), report);

        // 2. Filtros y Resumen Ejecutivo
        newPageWithHeader(session, "Modalidades Completadas \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addFiltersAndExecutiveSummary(session.document(), report);

        // 3. Estadísticas Generales
        newPageWithHeader(session, "Modalidades Completadas \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addGeneralStatistics(session.document(), report);

        // 4. Análisis por Resultado
        newPageWithHeader(session, "Modalidades Completadas \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addResultAnalysis(session.document(), report);

        // 5. Análisis por Tipo de Modalidad
        newPageWithHeader(session, "Modalidades Completadas \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addModalityTypeAnalysis(session.document(), report);

        // 6. Listado Detallado de Modalidades Completadas
        newPageWithHeader(session, "Modalidades Completadas \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addCompletedModalitiesListing(session.document(), report);

        // 7. Análisis Temporal
        newPageWithHeader(session, "Modalidades Completadas \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addTemporalAnalysis(session.document(), report);

        // 8. Desempeño de Directores
        newPageWithHeader(session, "Modalidades Completadas \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addDirectorPerformance(session.document(), report);

        // 9. Análisis de Distinciones Académicas
        newPageWithHeader(session, "Modalidades Completadas \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addDistinctionAnalysis(session.document(), report);

        // 10. Cierre institucional
        InstitutionalPdfHeader.addFooterSection(session.document(),
            "Documento generado autom\u00e1ticamente por el Sistema SIGMA.\n" +
            "Universidad Surcolombiana | Facultad de Ingenier\u00eda | Neiva \u2013 Huila\n" +
            "www.usco.edu.co  \u2022  NIT: 891180084-2",
            "Sistema Integral de Gesti\u00f3n de Modalidades de Grado \u2014 SIGMA | Universidad Surcolombiana");

        close(session);
        return session.out();
    }

    /**
     * Portada del reporte
     */
    private void addCoverPage(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException, IOException {

        List<String[]> infoRows = new ArrayList<>();
        infoRows.add(new String[]{"Fecha de generación:",
                report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_FULL)});
        infoRows.add(new String[]{"Generado por:", report.getGeneratedBy()});
        if (report.getExecutiveSummary() != null) {
            infoRows.add(new String[]{"Total completadas:",
                    String.valueOf(report.getExecutiveSummary().getTotalCompleted())});
            infoRows.add(new String[]{"Exitosas:",
                    String.valueOf(report.getExecutiveSummary().getTotalSuccessful())});
            infoRows.add(new String[]{"Fallidas:",
                    String.valueOf(report.getExecutiveSummary().getTotalFailed())});
            infoRows.add(new String[]{"Tasa de éxito:",
                    String.format("%.1f%%", report.getExecutiveSummary().getSuccessRate())});
        }

        InstitutionalPdfHeader.addCoverPage(
                document,
                report.getAcademicProgramName() + (report.getAcademicProgramCode() != null
                        ? " — Cód. " + report.getAcademicProgramCode() : ""),
                "Reporte de Modalidades Completadas",
                "REPORTE DE MODALIDADES COMPLETADAS\nAnálisis de Resultados Académicos",
                List.of(),
                infoRows,
                "Este reporte presenta un análisis completo de las modalidades de grado finalizadas, " +
                "incluyendo tanto las exitosas como las fallidas. Se incluyen estadísticas de calificaciones, " +
                "tiempos de completitud, distinciones académicas, desempeño de directores y tendencias temporales. " +
                "La información es generada automáticamente por el sistema SIGMA.");
    }



    /**
     * Filtros y resumen ejecutivo
     */
    private void addFiltersAndExecutiveSummary(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "1. FILTROS APLICADOS Y RESUMEN EJECUTIVO");

        // Filtros aplicados
        if (report.getAppliedFilters() != null) {
            InstitutionalPdfHeader.addSubsectionTitle(document, "Filtros Aplicados");

            PdfPTable filterTable = new PdfPTable(1);
            filterTable.setWidthPercentage(100);
            filterTable.setSpacingBefore(10);
            filterTable.setSpacingAfter(20);

            PdfPCell filterCell = new PdfPCell();
            filterCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
            filterCell.setPadding(15);
            filterCell.setBorder(Rectangle.NO_BORDER);

            Paragraph filterText = new Paragraph(
                report.getAppliedFilters().getFilterDescription(),
                InstitutionalPdfHeader.NORMAL_FONT
            );
            filterCell.addElement(filterText);
            filterTable.addCell(filterCell);
            document.add(filterTable);
        }

        // Resumen ejecutivo
        if (report.getExecutiveSummary() != null) {
            InstitutionalPdfHeader.addSubsectionTitle(document, "Resumen Ejecutivo");

            CompletedModalitiesReportDTO.ExecutiveSummaryDTO summary = report.getExecutiveSummary();

            // Métricas principales en tarjetas
            PdfPTable metricsTable = new PdfPTable(5);
            metricsTable.setWidthPercentage(100);
            metricsTable.setSpacingBefore(10);
            metricsTable.setSpacingAfter(20);

            InstitutionalPdfHeader.addMetricCard(metricsTable, "Total Completadas",
                String.valueOf(summary.getTotalCompleted()), InstitutionalPdfHeader.INST_GOLD);
            InstitutionalPdfHeader.addMetricCard(metricsTable, "Exitosas",
                String.valueOf(summary.getTotalSuccessful()), InstitutionalPdfHeader.INST_GOLD);
            InstitutionalPdfHeader.addMetricCard(metricsTable, "Fallidas",
                String.valueOf(summary.getTotalFailed()), InstitutionalPdfHeader.INST_RED);
            InstitutionalPdfHeader.addMetricCard(metricsTable, "Tasa de Éxito",
                String.format("%.1f%%", summary.getSuccessRate()), InstitutionalPdfHeader.INST_GOLD);
            InstitutionalPdfHeader.addMetricCard(metricsTable, "Con Distinción",
                String.valueOf(summary.getWithDistinction()), InstitutionalPdfHeader.INST_GOLD);

            document.add(metricsTable);

            // Segunda fila de métricas
            PdfPTable metrics2Table = new PdfPTable(4);
            metrics2Table.setWidthPercentage(100);
            metrics2Table.setSpacingBefore(5);
            metrics2Table.setSpacingAfter(15);

            InstitutionalPdfHeader.addMetricCard(metrics2Table, "Calificación Promedio",
                String.format("%.2f", summary.getAverageGrade()), InstitutionalPdfHeader.INST_GOLD);
            InstitutionalPdfHeader.addMetricCard(metrics2Table, "Días Promedio",
                String.format("%.0f", summary.getAverageCompletionDays()), InstitutionalPdfHeader.INST_RED);
            InstitutionalPdfHeader.addMetricCard(metrics2Table, "Total Estudiantes",
                String.valueOf(summary.getTotalStudents()), InstitutionalPdfHeader.INST_GOLD);
            InstitutionalPdfHeader.addMetricCard(metrics2Table, "Directores Únicos",
                String.valueOf(summary.getUniqueDirectors()), InstitutionalPdfHeader.TEXT_GRAY);

            document.add(metrics2Table);
        }
    }

    /**
     * Estadísticas generales
     */
    private void addGeneralStatistics(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "2. ESTADÍSTICAS GENERALES");

        if (report.getGeneralStatistics() == null) {
            document.add(new Paragraph("No hay estadísticas disponibles.", InstitutionalPdfHeader.NORMAL_FONT));
            return;
        }

        CompletedModalitiesReportDTO.GeneralStatisticsDTO stats = report.getGeneralStatistics();

        // NUEVO: Tarjetas de resumen con iconos
        addGeneralStatsSummaryCards(document, stats);

        // Resultados con gráfico visual mejorado
        InstitutionalPdfHeader.addSubsectionTitle(document, "Resultados Generales");

        PdfPTable resultsTable = new PdfPTable(4);
        resultsTable.setWidthPercentage(90);
        resultsTable.setSpacingBefore(10);
        resultsTable.setSpacingAfter(20);
        resultsTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        InstitutionalPdfHeader.addMetricCard(resultsTable, "Aprobadas",
            String.valueOf(stats.getApproved()), InstitutionalPdfHeader.INST_GOLD);
        InstitutionalPdfHeader.addMetricCard(resultsTable, "Reprobadas",
            String.valueOf(stats.getFailed()), InstitutionalPdfHeader.INST_RED);
        InstitutionalPdfHeader.addMetricCard(resultsTable, "Tasa Aprobación",
            String.format("%.1f%%", stats.getApprovalRate()), InstitutionalPdfHeader.INST_GOLD);
        InstitutionalPdfHeader.addMetricCard(resultsTable, "Total",
            String.valueOf(stats.getTotalCompleted()), InstitutionalPdfHeader.INST_RED);

        document.add(resultsTable);

        // NUEVO: Gráfico visual de tasa de aprobación
        addApprovalRateChart(document, stats);

        // Tiempos de completitud con visualización mejorada
        InstitutionalPdfHeader.addSubsectionTitle(document, "Tiempos de Completitud (días)");

        PdfPTable timeTable = new PdfPTable(2);
        timeTable.setWidthPercentage(80);
        timeTable.setWidths(new float[]{2f, 1f});
        timeTable.setSpacingBefore(10);
        timeTable.setSpacingAfter(15);
        timeTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        InstitutionalPdfHeader.addStatRow(timeTable, "Promedio:",
            String.format("%.0f días", stats.getAverageCompletionDays()));
        InstitutionalPdfHeader.addStatRow(timeTable, "Más Rápida:",
            stats.getFastestCompletionDays() != null ?
            stats.getFastestCompletionDays() + " días" : "N/D");
        InstitutionalPdfHeader.addStatRow(timeTable, "Más Lenta:",
            stats.getSlowestCompletionDays() != null ?
            stats.getSlowestCompletionDays() + " días" : "N/D");
        InstitutionalPdfHeader.addStatRow(timeTable, "Mediana:",
            stats.getMedianCompletionDays() != null ?
            String.format("%.0f días", stats.getMedianCompletionDays()) : "N/D");

        document.add(timeTable);

        // NUEVO: Gráfico de distribución de tiempos
        addTimeDistributionChart(document, stats);

        // Calificaciones con gráfico visual
        InstitutionalPdfHeader.addSubsectionTitle(document, "Calificaciones");

        PdfPTable gradeTable = new PdfPTable(2);
        gradeTable.setWidthPercentage(80);
        gradeTable.setWidths(new float[]{2f, 1f});
        gradeTable.setSpacingBefore(10);
        gradeTable.setSpacingAfter(15);
        gradeTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        InstitutionalPdfHeader.addStatRow(gradeTable, "Promedio General:",
            String.format("%.2f", stats.getAverageGrade()));
        InstitutionalPdfHeader.addStatRow(gradeTable, "Calificación Más Alta:",
            stats.getHighestGrade() != null ?
            String.format("%.2f", stats.getHighestGrade()) : "N/D");
        InstitutionalPdfHeader.addStatRow(gradeTable, "Calificación Más Baja:",
            stats.getLowestGrade() != null ?
            String.format("%.2f", stats.getLowestGrade()) : "N/D");
        InstitutionalPdfHeader.addStatRow(gradeTable, "Mediana:",
            stats.getMedianGrade() != null ?
            String.format("%.2f", stats.getMedianGrade()) : "N/D");

        document.add(gradeTable);

        // NUEVO: Gráfico de distribución de calificaciones
        addGradeDistributionChart(document, stats);

        // Distinciones académicas con visualización mejorada
        InstitutionalPdfHeader.addSubsectionTitle(document, "Distinciones Académicas");

        PdfPTable distinctionTable = new PdfPTable(3);
        distinctionTable.setWidthPercentage(90);
        distinctionTable.setSpacingBefore(10);
        distinctionTable.setSpacingAfter(15);
        distinctionTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        InstitutionalPdfHeader.addMetricCard(distinctionTable, "Meritoria",
            String.valueOf(stats.getWithMeritorious()), InstitutionalPdfHeader.INST_GOLD);
        InstitutionalPdfHeader.addMetricCard(distinctionTable, "Laureada",
            String.valueOf(stats.getWithLaudeate()), InstitutionalPdfHeader.INST_GOLD);
        InstitutionalPdfHeader.addMetricCard(distinctionTable, "Sin Distinción",
            String.valueOf(stats.getWithoutDistinction()), InstitutionalPdfHeader.LIGHT_GOLD);

        document.add(distinctionTable);

        // NUEVO: Gráfico de distribución de distinciones
        addDistinctionDistributionChart(document, stats);
    }

    /**
     * Análisis por resultado
     */
    private void addResultAnalysis(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "3. ANÁLISIS POR RESULTADO");

        if (report.getResultAnalysis() == null) {
            document.add(new Paragraph("No hay análisis de resultado disponible.", InstitutionalPdfHeader.NORMAL_FONT));
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
        successCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        successCell.setBorderWidth(2);

        Paragraph successTitle = new Paragraph("✓ EXITOSAS",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, InstitutionalPdfHeader.INST_GOLD));
        successTitle.setAlignment(Element.ALIGN_CENTER);
        successCell.addElement(successTitle);
        successCell.addElement(new Paragraph("\n"));

        successCell.addElement(new Paragraph("Cantidad: " + analysis.getSuccessfulCount(), InstitutionalPdfHeader.BOLD_FONT));
        successCell.addElement(new Paragraph("Tasa: " + String.format("%.1f%%", analysis.getSuccessRate()), InstitutionalPdfHeader.NORMAL_FONT));
        successCell.addElement(new Paragraph("Calificación Promedio: " +
            String.format("%.2f", analysis.getAverageSuccessGrade()), InstitutionalPdfHeader.NORMAL_FONT));
        successCell.addElement(new Paragraph("Días Promedio: " +
            String.format("%.0f", analysis.getAverageSuccessCompletionDays()), InstitutionalPdfHeader.NORMAL_FONT));

        if (analysis.getSuccessFactors() != null && !analysis.getSuccessFactors().isEmpty()) {
            successCell.addElement(new Paragraph("\nFactores de Éxito:",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, InstitutionalPdfHeader.INST_GOLD)));
            for (String factor : analysis.getSuccessFactors()) {
                successCell.addElement(new Paragraph("• " + factor, InstitutionalPdfHeader.SMALL_FONT));
            }
        }

        comparisonTable.addCell(successCell);

        // Columna fallidas
        PdfPCell failedCell = new PdfPCell();
        failedCell.setBackgroundColor(new BaseColor(255, 235, 238));
        failedCell.setPadding(15);
        failedCell.setBorder(Rectangle.BOX);
        failedCell.setBorderColor(InstitutionalPdfHeader.INST_RED);
        failedCell.setBorderWidth(2);

        Paragraph failedTitle = new Paragraph("✗ FALLIDAS",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, InstitutionalPdfHeader.INST_RED));
        failedTitle.setAlignment(Element.ALIGN_CENTER);
        failedCell.addElement(failedTitle);
        failedCell.addElement(new Paragraph("\n"));

        failedCell.addElement(new Paragraph("Cantidad: " + analysis.getFailedCount(), InstitutionalPdfHeader.BOLD_FONT));
        failedCell.addElement(new Paragraph("Tasa: " + String.format("%.1f%%", analysis.getFailureRate()), InstitutionalPdfHeader.NORMAL_FONT));
        failedCell.addElement(new Paragraph("Calificación Promedio: " +
            String.format("%.2f", analysis.getAverageFailureGrade()), InstitutionalPdfHeader.NORMAL_FONT));
        failedCell.addElement(new Paragraph("Días Promedio: " +
            String.format("%.0f", analysis.getAverageFailureCompletionDays()), InstitutionalPdfHeader.NORMAL_FONT));

        if (analysis.getFailureReasons() != null && !analysis.getFailureReasons().isEmpty()) {
            failedCell.addElement(new Paragraph("\nRazones de Fallo:",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, InstitutionalPdfHeader.INST_RED)));
            for (String reason : analysis.getFailureReasons()) {
                failedCell.addElement(new Paragraph("• " + reason, InstitutionalPdfHeader.SMALL_FONT));
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
            BaseColor verdictColor = ReportUtils.getPerformanceColor(analysis.getPerformanceVerdict());
            verdictCell.setBackgroundColor(verdictColor);
            verdictCell.setPadding(12);
            verdictCell.setBorder(Rectangle.NO_BORDER);

            Paragraph verdictText = new Paragraph(
                "DESEMPEÑO: " + ReportUtils.translatePerformanceVerdict(analysis.getPerformanceVerdict()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, InstitutionalPdfHeader.WHITE)
            );
            verdictText.setAlignment(Element.ALIGN_CENTER);
            verdictCell.addElement(verdictText);
            verdictTable.addCell(verdictCell);
            document.add(verdictTable);
        }

        // Recomendaciones
        if (analysis.getRecommendations() != null && !analysis.getRecommendations().isEmpty()) {
            InstitutionalPdfHeader.addSubsectionTitle(document, "Recomendaciones");

            PdfPTable recTable = new PdfPTable(1);
            recTable.setWidthPercentage(95);
            recTable.setSpacingBefore(10);

            for (String rec : analysis.getRecommendations()) {
                PdfPCell recCell = new PdfPCell();
                recCell.setBackgroundColor(new BaseColor(255, 248, 225));
                recCell.setPadding(10);
                recCell.setBorder(Rectangle.NO_BORDER);
                recCell.setPhrase(new Phrase("→ " + rec, InstitutionalPdfHeader.NORMAL_FONT));
                recTable.addCell(recCell);
            }

            document.add(recTable);
        }
    }

    // Continúa en el siguiente mensaje debido a límites de longitud...

    // Continuará en siguiente archivo...

    /**
     * Análisis por tipo de modalidad
     */
    private void addModalityTypeAnalysis(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "4. ANÁLISIS POR TIPO DE MODALIDAD");

        if (report.getModalityTypeAnalysis() == null || report.getModalityTypeAnalysis().isEmpty()) {
            document.add(new Paragraph("No hay análisis por tipo disponible.", InstitutionalPdfHeader.NORMAL_FONT));
            return;
        }

        // NUEVO: Resumen con tarjetas
        addModalityTypesSummaryCards(document, report.getModalityTypeAnalysis());

        // NUEVO: Top 5 modalidades con mejor desempeño
        addTopModalitiesChart(document, report.getModalityTypeAnalysis());

        // Tabla de análisis detallada
        InstitutionalPdfHeader.addSubsectionTitle(document, "Detalle por Tipo de Modalidad");

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 1f, 1f, 1f, 1f, 1.2f});
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);

        // Encabezados
        InstitutionalPdfHeader.addTableHeader(table, "Tipo de Modalidad");
        InstitutionalPdfHeader.addTableHeader(table, "Total");
        InstitutionalPdfHeader.addTableHeader(table, "Exitosas");
        InstitutionalPdfHeader.addTableHeader(table, "Fallidas");
        InstitutionalPdfHeader.addTableHeader(table, "Tasa Éxito");
        InstitutionalPdfHeader.addTableHeader(table, "Desempeño");

        // Datos
        boolean alternate = false;
        for (CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO analysis : report.getModalityTypeAnalysis()) {
            InstitutionalPdfHeader.addTableCell(table, analysis.getModalityType(), alternate);
            InstitutionalPdfHeader.addTableCell(table, String.valueOf(analysis.getTotalCompleted()), alternate);
            InstitutionalPdfHeader.addTableCell(table, String.valueOf(analysis.getSuccessful()), alternate);
            InstitutionalPdfHeader.addTableCell(table, String.valueOf(analysis.getFailed()), alternate);
            InstitutionalPdfHeader.addTableCell(table, String.format("%.1f%%", analysis.getSuccessRate()), alternate);
            InstitutionalPdfHeader.addTableCell(table, translatePerformance(analysis.getPerformance()), alternate);

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

        InstitutionalPdfHeader.addSectionTitle(document, "5. LISTADO DETALLADO DE MODALIDADES COMPLETADAS");

        if (report.getCompletedModalities() == null || report.getCompletedModalities().isEmpty()) {
            document.add(new Paragraph("No hay modalidades para mostrar.", InstitutionalPdfHeader.NORMAL_FONT));
            return;
        }

        // Tabla detallada
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.8f, 0.8f, 1.2f, 0.8f, 0.8f, 1.5f, 1f});
        table.setSpacingBefore(10);
        table.setHeaderRows(1);

        // Encabezados
        InstitutionalPdfHeader.addTableHeader(table, "Modalidad");
        InstitutionalPdfHeader.addTableHeader(table, "Resultado");
        InstitutionalPdfHeader.addTableHeader(table, "Estudiantes");
        InstitutionalPdfHeader.addTableHeader(table, "Calif.");
        InstitutionalPdfHeader.addTableHeader(table, "Días");
        InstitutionalPdfHeader.addTableHeader(table, "Director");
        InstitutionalPdfHeader.addTableHeader(table, "Distinción");

        // Datos (limitar a primeros 50 para no sobrecargar)
        boolean alternate = false;
        int count = 0;
        for (CompletedModalitiesReportDTO.CompletedModalityDetailDTO detail : report.getCompletedModalities()) {
            if (count++ >= 50) break;

            InstitutionalPdfHeader.addTableCell(table, InstitutionalPdfHeader.truncate(detail.getModalityTypeName(), 25), alternate);

            PdfPCell resultCell = new PdfPCell(new Phrase(
                "SUCCESS".equals(detail.getResult()) ? "✓" : "✗",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9,
                    "SUCCESS".equals(detail.getResult()) ? InstitutionalPdfHeader.INST_GOLD : InstitutionalPdfHeader.INST_RED)
            ));
            resultCell.setBackgroundColor(alternate ? InstitutionalPdfHeader.LIGHT_GOLD : InstitutionalPdfHeader.WHITE);
            resultCell.setBorder(Rectangle.BOTTOM);
            resultCell.setBorderColor(InstitutionalPdfHeader.LIGHT_GOLD);
            resultCell.setBorderWidth(0.3f);
            resultCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            resultCell.setPadding(5);
            table.addCell(resultCell);

            String studentNames = detail.getStudents() != null ?
                detail.getStudents().stream()
                    .map(s -> s.getFullName())
                    .collect(java.util.stream.Collectors.joining(", ")) : "N/D";
            InstitutionalPdfHeader.addTableCell(table, InstitutionalPdfHeader.truncate(studentNames, 25), alternate);

            InstitutionalPdfHeader.addTableCell(table, detail.getFinalGrade() != null ?
                String.format("%.2f", detail.getFinalGrade()) : "N/D", alternate);
            InstitutionalPdfHeader.addTableCell(table, detail.getCompletionDays() != null ?
                String.valueOf(detail.getCompletionDays()) : "N/D", alternate);
            InstitutionalPdfHeader.addTableCell(table, detail.getDirectorName() != null ?
                InstitutionalPdfHeader.truncate(detail.getDirectorName(), 20) : "Sin asignar", alternate);
            InstitutionalPdfHeader.addTableCell(table, detail.getAcademicDistinction() != null ?
                translateDistinction(detail.getAcademicDistinction()) : "-", alternate);

            alternate = !alternate;
        }

        document.add(table);

        if (report.getCompletedModalities().size() > 50) {
            Paragraph note = new Paragraph(
                "* Se muestran las primeras 50 modalidades. Total: " + report.getCompletedModalities().size(),
                InstitutionalPdfHeader.TINY_FONT
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

        InstitutionalPdfHeader.addSectionTitle(document, "6. ANÁLISIS TEMPORAL");

        if (report.getTemporalAnalysis() == null) {
            document.add(new Paragraph("No hay análisis temporal disponible.", InstitutionalPdfHeader.NORMAL_FONT));
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

        InstitutionalPdfHeader.addMetricCard(trendTable, "Tendencia",
            translateTrend(temporal.getTrend()), getTrendColor(temporal.getTrend()));
        InstitutionalPdfHeader.addMetricCard(trendTable, "Tasa de Crecimiento",
            String.format("%.1f%%", temporal.getGrowthRate()), InstitutionalPdfHeader.INST_GOLD);
        InstitutionalPdfHeader.addMetricCard(trendTable, "Mejor Periodo",
            temporal.getBestPeriod() != null ? temporal.getBestPeriod() : "N/D", InstitutionalPdfHeader.INST_GOLD);
        InstitutionalPdfHeader.addMetricCard(trendTable, "Peor Periodo",
            temporal.getWorstPeriod() != null ? temporal.getWorstPeriod() : "N/D", InstitutionalPdfHeader.INST_RED);

        document.add(trendTable);

        // Tabla de datos por periodo
        if (temporal.getPeriodData() != null && !temporal.getPeriodData().isEmpty()) {
            InstitutionalPdfHeader.addSubsectionTitle(document, "Datos por Periodo Académico");

            PdfPTable periodTable = new PdfPTable(6);
            periodTable.setWidthPercentage(100);
            periodTable.setWidths(new float[]{1.2f, 1f, 1f, 1f, 1.2f, 1f});
            periodTable.setSpacingBefore(10);

            InstitutionalPdfHeader.addTableHeader(periodTable, "Periodo");
            InstitutionalPdfHeader.addTableHeader(periodTable, "Completadas");
            InstitutionalPdfHeader.addTableHeader(periodTable, "Exitosas");
            InstitutionalPdfHeader.addTableHeader(periodTable, "Fallidas");
            InstitutionalPdfHeader.addTableHeader(periodTable, "Tasa Éxito");
            InstitutionalPdfHeader.addTableHeader(periodTable, "Calif. Prom.");

            boolean alternate = false;
            for (CompletedModalitiesReportDTO.PeriodDataDTO period : temporal.getPeriodData()) {
                InstitutionalPdfHeader.addTableCell(periodTable, period.getPeriod(), alternate);
                InstitutionalPdfHeader.addTableCell(periodTable, String.valueOf(period.getCompleted()), alternate);
                InstitutionalPdfHeader.addTableCell(periodTable, String.valueOf(period.getSuccessful()), alternate);
                InstitutionalPdfHeader.addTableCell(periodTable, String.valueOf(period.getFailed()), alternate);
                InstitutionalPdfHeader.addTableCell(periodTable, String.format("%.1f%%", period.getSuccessRate()), alternate);
                InstitutionalPdfHeader.addTableCell(periodTable, String.format("%.2f", period.getAverageGrade()), alternate);

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
            case "IMPROVING": return InstitutionalPdfHeader.INST_GOLD;
            case "STABLE": return InstitutionalPdfHeader.INST_GOLD;
            case "DECLINING": return InstitutionalPdfHeader.INST_RED;
            default: return InstitutionalPdfHeader.LIGHT_GOLD;
        }
    }

    /**
     * Desempeño de directores
     */
    private void addDirectorPerformance(Document document, CompletedModalitiesReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "7. DESEMPEÑO DE DIRECTORES");

        if (report.getDirectorPerformance() == null) {
            document.add(new Paragraph("No hay datos de desempeño de directores.", InstitutionalPdfHeader.NORMAL_FONT));
            return;
        }

        CompletedModalitiesReportDTO.DirectorPerformanceDTO performance = report.getDirectorPerformance();

        // Indicadores generales
        PdfPTable indicatorsTable = new PdfPTable(3);
        indicatorsTable.setWidthPercentage(90);
        indicatorsTable.setSpacingBefore(10);
        indicatorsTable.setSpacingAfter(20);
        indicatorsTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        InstitutionalPdfHeader.addMetricCard(indicatorsTable, "Total Directores",
            String.valueOf(performance.getTotalDirectors()), InstitutionalPdfHeader.INST_RED);
        InstitutionalPdfHeader.addMetricCard(indicatorsTable, "Tasa Éxito Prom.",
            String.format("%.1f%%", performance.getAverageSuccessRateByDirector()), InstitutionalPdfHeader.INST_GOLD);
        InstitutionalPdfHeader.addMetricCard(indicatorsTable, "Mejor Director",
            performance.getBestDirector() != null ?
            InstitutionalPdfHeader.truncate(performance.getBestDirector(), 15) : "N/D", InstitutionalPdfHeader.INST_GOLD);

        document.add(indicatorsTable);

        // Top directores
        if (performance.getTopDirectors() != null && !performance.getTopDirectors().isEmpty()) {
            InstitutionalPdfHeader.addSubsectionTitle(document, "Top 10 Directores por Desempeño");

            PdfPTable directorTable = new PdfPTable(6);
            directorTable.setWidthPercentage(100);
            directorTable.setWidths(new float[]{2.5f, 1f, 1f, 1f, 1.2f, 1f});
            directorTable.setSpacingBefore(10);

            InstitutionalPdfHeader.addTableHeader(directorTable, "Director");
            InstitutionalPdfHeader.addTableHeader(directorTable, "Total");
            InstitutionalPdfHeader.addTableHeader(directorTable, "Exitosas");
            InstitutionalPdfHeader.addTableHeader(directorTable, "Fallidas");
            InstitutionalPdfHeader.addTableHeader(directorTable, "Tasa Éxito");
            InstitutionalPdfHeader.addTableHeader(directorTable, "Distinciones");

            boolean alternate = false;
            for (CompletedModalitiesReportDTO.TopDirectorDTO director : performance.getTopDirectors()) {
                InstitutionalPdfHeader.addTableCell(directorTable, InstitutionalPdfHeader.truncate(director.getDirectorName(), 30), alternate);
                InstitutionalPdfHeader.addTableCell(directorTable, String.valueOf(director.getTotalSupervised()), alternate);
                InstitutionalPdfHeader.addTableCell(directorTable, String.valueOf(director.getSuccessful()), alternate);
                InstitutionalPdfHeader.addTableCell(directorTable, String.valueOf(director.getFailed()), alternate);
                InstitutionalPdfHeader.addTableCell(directorTable, String.format("%.1f%%", director.getSuccessRate()), alternate);
                InstitutionalPdfHeader.addTableCell(directorTable, String.valueOf(director.getWithDistinction()), alternate);

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

        InstitutionalPdfHeader.addSectionTitle(document, "8. ANÁLISIS DE DISTINCIONES ACADÉMICAS");

        if (report.getDistinctionAnalysis() == null) {
            document.add(new Paragraph("No hay análisis de distinciones disponible.", InstitutionalPdfHeader.NORMAL_FONT));
            return;
        }

        CompletedModalitiesReportDTO.DistinctionAnalysisDTO distinction = report.getDistinctionAnalysis();

        // Indicadores principales
        PdfPTable indicatorsTable = new PdfPTable(4);
        indicatorsTable.setWidthPercentage(100);
        indicatorsTable.setSpacingBefore(10);
        indicatorsTable.setSpacingAfter(20);

        InstitutionalPdfHeader.addMetricCard(indicatorsTable, "Total con Distinción",
            String.valueOf(distinction.getTotalWithDistinction()), InstitutionalPdfHeader.INST_GOLD);
        InstitutionalPdfHeader.addMetricCard(indicatorsTable, "Meritorias",
            String.valueOf(distinction.getMeritorious()), new BaseColor(255, 152, 0));
        InstitutionalPdfHeader.addMetricCard(indicatorsTable, "Laureadas",
            String.valueOf(distinction.getLaureate()), InstitutionalPdfHeader.INST_GOLD);
        InstitutionalPdfHeader.addMetricCard(indicatorsTable, "Tasa Distinción",
            String.format("%.1f%%", distinction.getDistinctionRate()), InstitutionalPdfHeader.INST_GOLD);

        document.add(indicatorsTable);

        // Modalidades con más distinciones
        if (distinction.getModalitiesWithMostDistinctions() != null &&
            !distinction.getModalitiesWithMostDistinctions().isEmpty()) {

            InstitutionalPdfHeader.addSubsectionTitle(document, "Modalidades con Más Distinciones");

            PdfPTable modalityTable = new PdfPTable(1);
            modalityTable.setWidthPercentage(90);
            modalityTable.setSpacingBefore(10);
            modalityTable.setSpacingAfter(15);
            modalityTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            for (String modality : distinction.getModalitiesWithMostDistinctions()) {
                PdfPCell cell = new PdfPCell(new Phrase("★ " + modality, InstitutionalPdfHeader.NORMAL_FONT));
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

            InstitutionalPdfHeader.addSubsectionTitle(document, "Directores con Más Distinciones");

            PdfPTable directorTable = new PdfPTable(1);
            directorTable.setWidthPercentage(90);
            directorTable.setSpacingBefore(10);
            directorTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            for (String director : distinction.getDirectorsWithMostDistinctions()) {
                PdfPCell cell = new PdfPCell(new Phrase("★ " + director, InstitutionalPdfHeader.NORMAL_FONT));
                cell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
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
                String.valueOf(stats.getTotalCompleted()), InstitutionalPdfHeader.INST_GOLD);

        // Tasa de aprobación
        addSummaryCardWithIcon(cardsTable, "Tasa Aprobación",
                String.format("%.1f%%", stats.getApprovalRate()), InstitutionalPdfHeader.INST_GOLD);

        // Calificación promedio
        addSummaryCardWithIcon(cardsTable, "Calificación Promedio",
                String.format("%.2f", stats.getAverageGrade()), InstitutionalPdfHeader.INST_RED);

        // Días promedio
        addSummaryCardWithIcon(cardsTable, "Días Promedio",
                String.format("%.0f", stats.getAverageCompletionDays()), InstitutionalPdfHeader.INST_RED);

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
        card.setBackgroundColor(InstitutionalPdfHeader.WHITE);
        card.setFixedHeight(70);

        // Valor grande (número principal)
        Paragraph valuePara = new Paragraph(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, color));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        valuePara.setSpacingAfter(5);
        card.addElement(valuePara);

        // Etiqueta descriptiva
        Paragraph labelPara = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, InstitutionalPdfHeader.TEXT_BLACK));
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

        InstitutionalPdfHeader.addSubsectionTitle(document, "Visualización de Tasa de Aprobación");

        int approved = stats.getApproved();
        int failed = stats.getFailed();
        int total = approved + failed;

        if (total == 0) return;

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(20);

        // Barra de aprobados
        float approvedPct = total > 0 ? (float) approved / total : 0;
        InstitutionalPdfHeader.addBarRow(chartTable, "Aprobadas", String.valueOf(approved),
            approved + " (" + String.format("%.1f%%", approvedPct * 100) + ")",
            approvedPct, InstitutionalPdfHeader.INST_GOLD);

        // Barra de reprobados
        float failedPct = total > 0 ? (float) failed / total : 0;
        InstitutionalPdfHeader.addBarRow(chartTable, "Reprobadas", String.valueOf(failed),
            failed + " (" + String.format("%.1f%%", failedPct * 100) + ")",
            failedPct, InstitutionalPdfHeader.INST_RED);

        document.add(chartTable);
    }

    /**
     * Gráfico de distribución de tiempos de completitud
     */
    private void addTimeDistributionChart(Document document,
                                         CompletedModalitiesReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        InstitutionalPdfHeader.addSubsectionTitle(document, "Distribución de Tiempos");

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
        float fastestPct = maxValue > 0 ? (float) fastest / maxValue : 0;
        InstitutionalPdfHeader.addBarRow(chartTable, "Más Rápida", fastest + " días", fastestPct, InstitutionalPdfHeader.INST_GOLD);

        // Promedio
        float averagePct = maxValue > 0 ? (float) average.intValue() / maxValue : 0;
        InstitutionalPdfHeader.addBarRow(chartTable, "Promedio", average.intValue() + " días", averagePct, InstitutionalPdfHeader.INST_GOLD);

        // Más lenta
        float slowestPct = maxValue > 0 ? (float) slowest / maxValue : 0;
        InstitutionalPdfHeader.addBarRow(chartTable, "Más Lenta", slowest + " días", slowestPct, InstitutionalPdfHeader.INST_RED);

        document.add(chartTable);
    }

    /**
     * Gráfico de distribución de calificaciones
     */
    private void addGradeDistributionChart(Document document,
                                          CompletedModalitiesReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        InstitutionalPdfHeader.addSubsectionTitle(document, "Distribución de Calificaciones");

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
        InstitutionalPdfHeader.addBarRow(chartTable, "Más Baja", String.format("%.2f", lowest), (float) (lowest / maxValue), InstitutionalPdfHeader.INST_RED);

        // Promedio
        InstitutionalPdfHeader.addBarRow(chartTable, "Promedio", String.format("%.2f", average), (float) (average / maxValue), InstitutionalPdfHeader.INST_GOLD);

        // Más alta
        InstitutionalPdfHeader.addBarRow(chartTable, "Más Alta", String.format("%.2f", highest), (float) (highest / maxValue), InstitutionalPdfHeader.INST_GOLD);

        document.add(chartTable);
    }

    /**
     * Gráfico de distribución de distinciones
     */
    private void addDistinctionDistributionChart(Document document,
                                                CompletedModalitiesReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        InstitutionalPdfHeader.addSubsectionTitle(document, "Gráfico de Distinciones");

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
        float meritoriousPct = total > 0 ? (float) meritorious / total : 0;
        InstitutionalPdfHeader.addBarRow(chartTable, "Meritoria", String.valueOf(meritorious),
                meritorious + " (" + String.format("%.1f%%", meritoriousPct * 100) + ")",
                meritoriousPct, new BaseColor(255, 152, 0)); // Naranja

        // Laureada
        float laureatePct = total > 0 ? (float) laureate / total : 0;
        InstitutionalPdfHeader.addBarRow(chartTable, "Laureada", String.valueOf(laureate),
                laureate + " (" + String.format("%.1f%%", laureatePct * 100) + ")",
                laureatePct, InstitutionalPdfHeader.INST_GOLD);

        // Sin distinción
        float withoutPct = total > 0 ? (float) without / total : 0;
        InstitutionalPdfHeader.addBarRow(chartTable, "Sin Distinción", String.valueOf(without),
                without + " (" + String.format("%.1f%%", withoutPct * 100) + ")",
                withoutPct, InstitutionalPdfHeader.LIGHT_GOLD);

        document.add(chartTable);
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
                String.valueOf(analysis.size()), InstitutionalPdfHeader.INST_GOLD);

        // Mejor tasa de éxito
        double bestRate = analysis.stream()
                .mapToDouble(CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO::getSuccessRate)
                .max().orElse(0);
        addSummaryCardWithIcon(cardsTable, "Mejor Tasa Éxito",
                String.format("%.1f%%", bestRate), InstitutionalPdfHeader.INST_GOLD);

        // Total completadas
        int totalCompleted = analysis.stream()
                .mapToInt(CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO::getTotalCompleted)
                .sum();
        addSummaryCardWithIcon(cardsTable, "Total Completadas",
                String.valueOf(totalCompleted), InstitutionalPdfHeader.INST_RED);

        document.add(cardsTable);
    }

    /**
     * Gráfico de Top 5 modalidades con mejor desempeño
     */
    private void addTopModalitiesChart(Document document,
                                      List<CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO> analysis)
            throws DocumentException {

        InstitutionalPdfHeader.addSubsectionTitle(document, "Top 5 Modalidades por Tasa de Éxito");

        // Ordenar por tasa de éxito
        List<CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO> topModalities = analysis.stream()
                .sorted(Comparator.comparingDouble(
                        CompletedModalitiesReportDTO.ModalityTypeAnalysisDTO::getSuccessRate).reversed())
                .limit(5)
                .toList();

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
        BaseColor rankColor = position == 1 ? InstitutionalPdfHeader.INST_GOLD : InstitutionalPdfHeader.INST_RED;
        headerCell.setBackgroundColor(rankColor);
        headerCell.setPadding(6);
        headerCell.setBorder(Rectangle.NO_BORDER);

        String rankIcon = position + "º";
        Paragraph headerText = new Paragraph(rankIcon + " " + InstitutionalPdfHeader.truncate(modality.getModalityType(), 40),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, InstitutionalPdfHeader.WHITE));
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
                FontFactory.getFont(FontFactory.HELVETICA, 7, InstitutionalPdfHeader.TEXT_BLACK)));
        cell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
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
                InstitutionalPdfHeader.INST_GOLD);

        // Total periodos
        int totalPeriods = temporal.getPeriodData() != null ? temporal.getPeriodData().size() : 0;
        addSummaryCardWithIcon(cardsTable, "Periodos Analizados",
                String.valueOf(totalPeriods), InstitutionalPdfHeader.INST_RED);

        // Total completadas
        int totalCompleted = temporal.getPeriodData() != null ?
                temporal.getPeriodData().stream()
                        .mapToInt(CompletedModalitiesReportDTO.PeriodDataDTO::getCompleted)
                        .sum() : 0;
        addSummaryCardWithIcon(cardsTable, "Total Completadas",
                String.valueOf(totalCompleted), InstitutionalPdfHeader.INST_GOLD);

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

        InstitutionalPdfHeader.addSubsectionTitle(document, "Evolución de Modalidades Completadas por Periodo");

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
            float periodPct = maxCompleted > 0 ? (float) period.getCompleted() / maxCompleted : 0;
            InstitutionalPdfHeader.addBarRow(chartTable, period.getPeriod(), String.valueOf(period.getCompleted()),
                period.getCompleted() + " total | " + String.format("%.1f%%", period.getSuccessRate()),
                periodPct, period.getSuccessRate() >= 70 ? InstitutionalPdfHeader.INST_GOLD : InstitutionalPdfHeader.INST_RED);
        }

        document.add(chartTable);
    }

    // ==================== FIN DE NUEVOS MÉTODOS ====================

    // ==================== MÉTODOS AUXILIARES ====================

    // ==================== MÉTODOS INSTITUCIONALES ====================

}

