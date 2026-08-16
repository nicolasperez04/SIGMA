package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.report.dto.StudentListingReportDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Servicio para generar PDF del reporte de listado de estudiantes con filtros
 * Diseño profesional y universitario con máxima información relevante
 */
@Service
public class StudentListingPdfGenerator extends BaseReportPdfGenerator {

    public ByteArrayOutputStream generatePDF(StudentListingReportDTO report)
            throws DocumentException, IOException {

        PdfSession session = openDocument(PageSize.A4.rotate(), 40, 40, 50, 50, report.getAcademicProgramName(), null);

        // 1. Portada
        addCoverPage(session.document(), report);

        // 2. Filtros Aplicados y Resumen Ejecutivo
        newPageWithHeader(session, "Listado de Estudiantes \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addFiltersAndExecutiveSummary(session.document(), report);

        // 3. Estadísticas Generales
        newPageWithHeader(session, "Listado de Estudiantes \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addGeneralStatistics(session.document(), report);

        // 4. Análisis de Distribución
        newPageWithHeader(session, "Listado de Estudiantes \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addDistributionAnalysis(session.document(), report);

        // 5. Listado Detallado de Estudiantes
        newPageWithHeader(session, "Listado de Estudiantes \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addStudentListing(session.document(), report);

        // 6. Estadísticas por Modalidad
        newPageWithHeader(session, "Listado de Estudiantes \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addModalityStatistics(session.document(), report);

        // 7. Estadísticas por Estado
        newPageWithHeader(session, "Listado de Estudiantes \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addStatusStatistics(session.document(), report);

        // 8. Estadísticas por Semestre
        newPageWithHeader(session, "Listado de Estudiantes \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addSemesterStatistics(session.document(), report);

        // 9. Pie institucional de cierre
        InstitutionalPdfHeader.addFooterSection(session.document(),
        "Este reporte fue generado autom\u00e1ticamente por el sistema SIGMA a partir de los datos acad\u00e9micos registrados para el programa: " + report.getAcademicProgramName() + ". Para consultas o modificaciones del listado, contacte con la coordinaci\u00f3n del programa acad\u00e9mico.",
        "Sistema Integral de Gesti\u00f3n de Modalidades de Grado \u2014 SIGMA\nUniversidad Surcolombiana | Facultad de Ingenier\u00eda | Neiva \u2013 Huila");

        close(session);
        return session.out();
    }

    /**
     * Portada del reporte
     */
    private void addCoverPage(Document document, StudentListingReportDTO report)
            throws DocumentException, IOException {

        java.util.List<String[]> infoRows = new ArrayList<>();
        infoRows.add(new String[]{"Programa académico:", report.getAcademicProgramName()});
        if (report.getAcademicProgramCode() != null) {
            infoRows.add(new String[]{"Código del programa:", report.getAcademicProgramCode()});
        }
        infoRows.add(new String[]{"Fecha de generación:",
                report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_FULL)});
        infoRows.add(new String[]{"Generado por:", report.getGeneratedBy()});
        int totalStudents = report.getStudents() != null ? report.getStudents().size() : 0;
        infoRows.add(new String[]{"Total de estudiantes:", String.valueOf(totalStudents)});
        infoRows.add(new String[]{"Filtros aplicados:",
                (report.getAppliedFilters() != null && report.getAppliedFilters().getHasFilters())
                        ? "Sí" : "No — Listado completo"});

        InstitutionalPdfHeader.addCoverPage(
                document,
                report.getAcademicProgramName() + (report.getAcademicProgramCode() != null
                        ? " — Cód. " + report.getAcademicProgramCode() : ""),
                "Reporte de Listado de Estudiantes",
                "REPORTE DE LISTADO DE ESTUDIANTES\nModalidades de Grado",
                java.util.List.of(),
                infoRows,
                "Este reporte presenta un listado detallado de estudiantes con sus modalidades de grado, " +
                "incluyendo información académica, estado de avance, directores asignados y estadísticas " +
                "generales. La información es generada automáticamente por el Sistema SIGMA.");
    }









    /**
     * Filtros aplicados y resumen ejecutivo
     */
    private void addFiltersAndExecutiveSummary(Document document, StudentListingReportDTO report)
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

            StudentListingReportDTO.ExecutiveSummaryDTO summary = report.getExecutiveSummary();

            // Métricas principales
            PdfPTable metricsTable = new PdfPTable(5);
            metricsTable.setWidthPercentage(100);
            metricsTable.setSpacingBefore(10);
            metricsTable.setSpacingAfter(20);

            InstitutionalPdfHeader.addMetricCard(metricsTable, "Total Estudiantes",
                String.valueOf(summary.getTotalStudents()), InstitutionalPdfHeader.INST_RED);
            InstitutionalPdfHeader.addMetricCard(metricsTable, "Modalidades Activas",
                String.valueOf(summary.getActiveModalities()), InstitutionalPdfHeader.INST_GOLD);
            InstitutionalPdfHeader.addMetricCard(metricsTable, "Completadas",
                String.valueOf(summary.getCompletedModalities()), InstitutionalPdfHeader.INST_GOLD);
            InstitutionalPdfHeader.addMetricCard(metricsTable, "Progreso Promedio",
                String.format("%.1f%%", summary.getAverageProgress()), InstitutionalPdfHeader.INST_GOLD);
            InstitutionalPdfHeader.addMetricCard(metricsTable, "Tipos de Modalidad",
                String.valueOf(summary.getDifferentModalityTypes()), InstitutionalPdfHeader.INST_GOLD);

            document.add(metricsTable);

            // Información adicional
            PdfPTable detailTable = new PdfPTable(2);
            detailTable.setWidthPercentage(90);
            detailTable.setWidths(new float[]{1.5f, 2f});
            detailTable.setSpacingBefore(10);
            detailTable.setSpacingAfter(15);
            detailTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            InstitutionalPdfHeader.addDetailRow(detailTable, "Modalidad Más Común:", summary.getMostCommonModalityType());
            InstitutionalPdfHeader.addDetailRow(detailTable, "Estado Más Común:", summary.getMostCommonStatus());
            InstitutionalPdfHeader.addDetailRow(detailTable, "Estados Diferentes:", String.valueOf(summary.getDifferentStatuses()));

            document.add(detailTable);
        }
    }

    /**
     * Estadísticas generales
     */
    private void addGeneralStatistics(Document document, StudentListingReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "2. ESTADÍSTICAS GENERALES");

        if (report.getGeneralStatistics() == null) {
            document.add(new Paragraph("No hay estadísticas disponibles.", InstitutionalPdfHeader.NORMAL_FONT));
            return;
        }

        StudentListingReportDTO.GeneralStatisticsDTO stats = report.getGeneralStatistics();

        // Tarjetas de resumen principal
        addGeneralStatsSummaryCards(document, stats);

        // Estadísticas de modalidades mejoradas
        InstitutionalPdfHeader.addSubsectionTitle(document, "📊 Distribución por Tipo de Modalidad");

        PdfPTable modalityTypeTable = new PdfPTable(4);
        modalityTypeTable.setWidthPercentage(100);
        modalityTypeTable.setSpacingBefore(10);
        modalityTypeTable.setSpacingAfter(20);

        // Modalidades individuales
        addStatsCard(modalityTypeTable, "Individuales",
            String.valueOf(stats.getIndividualModalities() != null ? stats.getIndividualModalities() : 0) + " modalidades",
            InstitutionalPdfHeader.INST_GOLD);

        // Modalidades grupales
        addStatsCard(modalityTypeTable, "Grupales",
            String.valueOf(stats.getGroupModalities() != null ? stats.getGroupModalities() : 0) + " modalidades",
            InstitutionalPdfHeader.INST_GOLD);

        // Con director
        addStatsCard(modalityTypeTable, "Con Director",
            String.valueOf(stats.getStudentsWithDirector() != null ? stats.getStudentsWithDirector() : 0) + " estudiantes",
            InstitutionalPdfHeader.INST_GOLD);

        // Sin director
        addStatsCard(modalityTypeTable, "Sin Director",
            String.valueOf(stats.getStudentsWithoutDirector() != null ? stats.getStudentsWithoutDirector() : 0) + " estudiantes",
            InstitutionalPdfHeader.INST_RED);

        document.add(modalityTypeTable);

        // NUEVO: Gráfico comparativo de modalidades
        addModalityComparisonChart(document, stats);

        // Estado de avance
        InstitutionalPdfHeader.addSubsectionTitle(document, "⏱ Estado de Avance Temporal");

        PdfPTable timelineTable = new PdfPTable(3);
        timelineTable.setWidthPercentage(90);
        timelineTable.setSpacingBefore(10);
        timelineTable.setSpacingAfter(20);
        timelineTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addStatsCard(timelineTable, "A Tiempo",
            String.valueOf(stats.getStudentsOnTime() != null ? stats.getStudentsOnTime() : 0) + " estudiantes",
            InstitutionalPdfHeader.INST_GOLD);
        addStatsCard(timelineTable, "En Riesgo",
            String.valueOf(stats.getStudentsAtRisk() != null ? stats.getStudentsAtRisk() : 0) + " estudiantes",
            InstitutionalPdfHeader.INST_RED);
        addStatsCard(timelineTable, "Retrasados",
            String.valueOf(stats.getStudentsDelayed() != null ? stats.getStudentsDelayed() : 0) + " estudiantes",
            InstitutionalPdfHeader.INST_RED);

        document.add(timelineTable);

        // NUEVO: Gráfico visual de estado temporal
        addTimelineStatusChart(document, stats);

        // Promedios académicos
        InstitutionalPdfHeader.addSubsectionTitle(document, "📚 Indicadores Académicos Promedio");

        PdfPTable avgTable = new PdfPTable(2);
        avgTable.setWidthPercentage(80);
        avgTable.setWidths(new float[]{2f, 1f});
        avgTable.setSpacingBefore(10);
        avgTable.setSpacingAfter(15);
        avgTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        InstitutionalPdfHeader.addStatRow(avgTable, "Promedio Acumulado:",
            String.format("%.2f", stats.getAverageCumulativeGPA()));
        InstitutionalPdfHeader.addStatRow(avgTable, "Créditos Completados (Promedio):",
            String.format("%.0f", stats.getAverageCompletedCredits()));
        InstitutionalPdfHeader.addStatRow(avgTable, "Días en Modalidad (Promedio):",
            String.format("%.0f días", stats.getAverageDaysInModality()));

        document.add(avgTable);

        // NUEVO: Indicadores de rendimiento visual
        addPerformanceIndicators(document, stats);
    }

    /**
     * Análisis de distribución
     */
    private void addDistributionAnalysis(Document document, StudentListingReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "3. ANÁLISIS DE DISTRIBUCIÓN");

        if (report.getDistributionAnalysis() == null) {
            document.add(new Paragraph("No hay análisis de distribución disponible.", InstitutionalPdfHeader.NORMAL_FONT));
            return;
        }

        StudentListingReportDTO.DistributionAnalysisDTO distribution = report.getDistributionAnalysis();


        // Distribución por modalidad mejorada
        if (distribution.getByModalityType() != null && !distribution.getByModalityType().isEmpty()) {
            InstitutionalPdfHeader.addSubsectionTitle(document, "📊 Distribución por Tipo de Modalidad");

            addEnhancedDistributionChart(document, distribution.getByModalityType(),
                distribution.getByModalityTypePercentage(), InstitutionalPdfHeader.INST_RED);  // Rojo institucional
        }

        // Distribución por estado mejorada
        if (distribution.getByStatus() != null && !distribution.getByStatus().isEmpty()) {
            document.add(new Paragraph("\n"));
            InstitutionalPdfHeader.addSubsectionTitle(document, "📌 Distribución por Estado");

            addEnhancedDistributionChart(document, distribution.getByStatus(),
                distribution.getByStatusPercentage(), InstitutionalPdfHeader.INST_GOLD);  // Dorado institucional
        }

        // Distribución por estado temporal mejorada
        if (distribution.getByTimelineStatus() != null && !distribution.getByTimelineStatus().isEmpty()) {
            document.add(new Paragraph("\n"));
            InstitutionalPdfHeader.addSubsectionTitle(document, "⏱ Distribución por Estado Temporal");

            addEnhancedDistributionChart(document, distribution.getByTimelineStatus(),
                distribution.getByTimelineStatusPercentage(), InstitutionalPdfHeader.INST_GOLD);  // Dorado institucional
        }
    }

    /**
     * Listado detallado de estudiantes
     */
    private void addStudentListing(Document document, StudentListingReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "4. LISTADO DETALLADO DE ESTUDIANTES");

        if (report.getStudents() == null || report.getStudents().isEmpty()) {
            document.add(new Paragraph("No hay estudiantes para mostrar.", InstitutionalPdfHeader.NORMAL_FONT));
            return;
        }

        Paragraph totalInfo = new Paragraph(
                String.format("Total de estudiantes en el listado: %d", report.getStudents().size()),
                InstitutionalPdfHeader.BOLD_FONT
        );
        totalInfo.setSpacingAfter(15);
        document.add(totalInfo);

        // Crear tabla detallada con más columnas
        PdfPTable table = new PdfPTable(11);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 0.8f, 1.8f, 1f, 0.6f, 0.6f, 1.2f, 0.7f, 1f, 0.7f, 0.8f});
        table.setSpacingBefore(10);
        table.setHeaderRows(1);

        // Encabezados
        InstitutionalPdfHeader.addTableHeader(table, "Estudiante");
        InstitutionalPdfHeader.addTableHeader(table, "Código");
        InstitutionalPdfHeader.addTableHeader(table, "Modalidad");
        InstitutionalPdfHeader.addTableHeader(table, "Estado");
        InstitutionalPdfHeader.addTableHeader(table, "Prog.");
        InstitutionalPdfHeader.addTableHeader(table, "GPA");
        InstitutionalPdfHeader.addTableHeader(table, "Director");
        InstitutionalPdfHeader.addTableHeader(table, "Días");
        InstitutionalPdfHeader.addTableHeader(table, "Estado Temp.");
        InstitutionalPdfHeader.addTableHeader(table, "Grupo");
        InstitutionalPdfHeader.addTableHeader(table, "Créditos");

        // Datos
        boolean alternate = false;
        for (StudentListingReportDTO.StudentDetailDTO student : report.getStudents()) {
            // Nombre completo
            InstitutionalPdfHeader.addTableCell(table, InstitutionalPdfHeader.truncate(student.getFullName(), 25), alternate);

            // Código
            InstitutionalPdfHeader.addTableCell(table, student.getStudentCode() != null ? student.getStudentCode() : "N/D", alternate);

            // Modalidad
            InstitutionalPdfHeader.addTableCell(table, InstitutionalPdfHeader.truncate(student.getModalityType(), 25), alternate);

            // Estado
            InstitutionalPdfHeader.addTableCell(table, InstitutionalPdfHeader.truncate(student.getModalityStatusDescription() != null ?
                    student.getModalityStatusDescription() : "N/D", 18), alternate);

            // Progreso
            Double progress = student.getProgressPercentage() != null ? student.getProgressPercentage() : 0.0;
            InstitutionalPdfHeader.addTableCell(table, String.format("%.0f%%", progress), alternate);

            // GPA
            Double gpa = student.getCumulativeAverage() != null ? student.getCumulativeAverage() : 0.0;
            InstitutionalPdfHeader.addTableCell(table, String.format("%.2f", gpa), alternate);

            // Director
            InstitutionalPdfHeader.addTableCell(table, student.getDirectorName() != null ?
                InstitutionalPdfHeader.truncate(student.getDirectorName(), 18) : "Sin asignar", alternate);

            // Días en modalidad
            Integer days = student.getDaysInModality() != null ? student.getDaysInModality() : 0;
            InstitutionalPdfHeader.addTableCell(table, String.valueOf(days), alternate);

            // Estado temporal
            InstitutionalPdfHeader.addTableCell(table, translateTimelineStatus(student.getTimelineStatus()), alternate);

            // Tamaño del grupo
            Integer groupSize = student.getGroupSize() != null ? student.getGroupSize() : 1;
            String groupInfo = groupSize > 1 ? String.valueOf(groupSize) : "Ind.";
            InstitutionalPdfHeader.addTableCell(table, groupInfo, alternate);

            // Créditos completados
            Integer credits = student.getCompletedCredits() != null ? student.getCompletedCredits() : 0;
            InstitutionalPdfHeader.addTableCell(table, String.valueOf(credits), alternate);

            alternate = !alternate;
        }

        document.add(table);

        // Notas explicativas mejoradas
        document.add(new Paragraph("\n"));
        PdfPTable legendTable = new PdfPTable(1);
        legendTable.setWidthPercentage(100);

        PdfPCell legendCell = new PdfPCell();
        legendCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        legendCell.setPadding(8);
        legendCell.setBorder(Rectangle.BOX);
        legendCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);

        Paragraph legendText = new Paragraph();
        legendText.add(new Chunk("LEYENDA: ", InstitutionalPdfHeader.BOLD_FONT));
        legendText.add(new Chunk(
                "Prog. = Progreso | GPA = Promedio acumulado | " +
                "Estado Temp. = A Tiempo/En Riesgo/Retrasado | " +
                "Grupo = Tamaño del grupo (Ind. = Individual) | " +
                "Créditos = Créditos académicos completados",
                InstitutionalPdfHeader.TINY_FONT
        ));
        legendCell.addElement(legendText);
        legendTable.addCell(legendCell);
        document.add(legendTable);
    }

    /**
     * Estadísticas por modalidad
     */
    private void addModalityStatistics(Document document, StudentListingReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "5. ESTADÍSTICAS POR TIPO DE MODALIDAD");

        if (report.getModalityStatistics() == null || report.getModalityStatistics().isEmpty()) {
            document.add(new Paragraph("No hay estadísticas por modalidad disponibles.", InstitutionalPdfHeader.NORMAL_FONT));
            return;
        }

        // Agregar resumen introductorio
        Paragraph intro = new Paragraph(
                String.format("Se han identificado %d tipos de modalidades diferentes. " +
                        "A continuación se presenta el análisis detallado de cada una:",
                        report.getModalityStatistics().size()),
                InstitutionalPdfHeader.NORMAL_FONT
        );
        intro.setSpacingAfter(15);
        document.add(intro);

        // Tabla de estadísticas mejorada
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 1f, 1f, 1f, 1.2f, 1.2f});
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);
        table.setHeaderRows(1);

        // Encabezados
        InstitutionalPdfHeader.addTableHeader(table, "Tipo de Modalidad");
        InstitutionalPdfHeader.addTableHeader(table, "Total Est.");
        InstitutionalPdfHeader.addTableHeader(table, "Activos");
        InstitutionalPdfHeader.addTableHeader(table, "Completados");
        InstitutionalPdfHeader.addTableHeader(table, "Tasa Complet.");
        InstitutionalPdfHeader.addTableHeader(table, "GPA Prom.");

        // Datos
        boolean alternate = false;
        for (StudentListingReportDTO.ModalityStatisticsDTO stat : report.getModalityStatistics()) {
            // Tipo de modalidad
            InstitutionalPdfHeader.addTableCell(table, InstitutionalPdfHeader.truncate(stat.getModalityType() != null ?
                    stat.getModalityType() : "Sin especificar", 35), alternate);

            // Total estudiantes
            Integer total = stat.getTotalStudents() != null ? stat.getTotalStudents() : 0;
            InstitutionalPdfHeader.addTableCell(table, String.valueOf(total), alternate);

            // Activos
            Integer active = stat.getActiveStudents() != null ? stat.getActiveStudents() : 0;
            InstitutionalPdfHeader.addTableCell(table, String.valueOf(active), alternate);

            // Completados
            Integer completed = stat.getCompletedStudents() != null ? stat.getCompletedStudents() : 0;
            InstitutionalPdfHeader.addTableCell(table, String.valueOf(completed), alternate);

            // Tasa de completación
            Double completionRate = stat.getCompletionRate() != null ? stat.getCompletionRate() : 0.0;
            InstitutionalPdfHeader.addTableCell(table, String.format("%.1f%%", completionRate), alternate);

            // GPA promedio
            Double avgGPA = stat.getAverageGPA() != null ? stat.getAverageGPA() : 0.0;
            InstitutionalPdfHeader.addTableCell(table, String.format("%.2f", avgGPA), alternate);

            alternate = !alternate;
        }

        document.add(table);

        // Agregar análisis de top directores si está disponible
        for (StudentListingReportDTO.ModalityStatisticsDTO stat : report.getModalityStatistics()) {
            if (stat.getTopDirectors() != null && !stat.getTopDirectors().isEmpty()) {
                InstitutionalPdfHeader.addSubsectionTitle(document, "Top Directores en " + stat.getModalityType());

                PdfPTable directorTable = new PdfPTable(1);
                directorTable.setWidthPercentage(90);
                directorTable.setSpacingBefore(5);
                directorTable.setSpacingAfter(10);
                directorTable.setHorizontalAlignment(Element.ALIGN_CENTER);

                for (String director : stat.getTopDirectors()) {
                    PdfPCell cell = new PdfPCell(new Phrase("• " + director, InstitutionalPdfHeader.SMALL_FONT));
                    cell.setBorder(Rectangle.NO_BORDER);
                    cell.setPadding(3);
                    directorTable.addCell(cell);
                }

                document.add(directorTable);
                break; // Solo mostrar para la primera modalidad con datos
            }
        }
    }

    /**
     * Estadísticas por estado
     */
    private void addStatusStatistics(Document document, StudentListingReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "6. ESTADÍSTICAS POR ESTADO");

        if (report.getStatusStatistics() == null || report.getStatusStatistics().isEmpty()) {
            document.add(new Paragraph("No hay estadísticas por estado disponibles.", InstitutionalPdfHeader.NORMAL_FONT));
            return;
        }

        // Agregar resumen introductorio
        int totalStudentsInStates = report.getStatusStatistics().stream()
                .mapToInt(s -> s.getStudentCount() != null ? s.getStudentCount() : 0)
                .sum();

        Paragraph intro = new Paragraph(
                String.format("Distribución de %d estudiantes en %d estados diferentes:",
                        totalStudentsInStates, report.getStatusStatistics().size()),
                InstitutionalPdfHeader.NORMAL_FONT
        );
        intro.setSpacingAfter(15);
        document.add(intro);

        // Gráfico de barras mejorado
        InstitutionalPdfHeader.addSubsectionTitle(document, "Distribución Visual de Estudiantes por Estado");

        // Encontrar máximo para escalar
        int maxValue = report.getStatusStatistics().stream()
            .mapToInt(s -> s.getStudentCount() != null ? s.getStudentCount() : 0)
            .max()
            .orElse(1);

        // Crear gráfico
        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(15);

        for (StudentListingReportDTO.StatusStatisticsDTO stat : report.getStatusStatistics()) {
            PdfPCell cell = new PdfPCell();
            cell.setPadding(3);
            cell.setBorder(Rectangle.NO_BORDER);

            // Tabla interna para la barra
            PdfPTable barTable = new PdfPTable(3);
            barTable.setWidthPercentage(100);

            try {
                barTable.setWidths(new float[]{2f, 4.5f, 1.5f});
            } catch (DocumentException e) {
                // Ignorar
            }

            // Etiqueta
            String statusLabel = stat.getStatusDescription() != null ?
                    stat.getStatusDescription() : "Estado desconocido";
            PdfPCell labelCell = new PdfPCell(new Phrase(
                InstitutionalPdfHeader.truncate(statusLabel, 30),
                InstitutionalPdfHeader.SMALL_FONT
            ));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            labelCell.setPadding(3);
            barTable.addCell(labelCell);

            // Barra
            Integer count = stat.getStudentCount() != null ? stat.getStudentCount() : 0;
            Double pct = stat.getPercentage() != null ? stat.getPercentage() : 0.0;
            float percentage = maxValue > 0 ? (float) count / maxValue : 0;
            PdfPCell barCell = createBarCell(
                count + " estudiantes",
                percentage,
                InstitutionalPdfHeader.INST_GOLD
            );
            barTable.addCell(barCell);

            // Información adicional
            PdfPCell infoCell = new PdfPCell();
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            infoCell.setPadding(3);

            Paragraph infoPara = new Paragraph();
            infoPara.add(new Chunk(String.format("%.1f%%\n", pct),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, InstitutionalPdfHeader.INST_RED)));

            // Agregar días promedio si está disponible
            if (stat.getAverageDaysInStatus() != null && stat.getAverageDaysInStatus() > 0) {
                infoPara.add(new Chunk(String.format("(%.0f días)", stat.getAverageDaysInStatus()),
                        FontFactory.getFont(FontFactory.HELVETICA, 7, InstitutionalPdfHeader.TEXT_GRAY)));
            }

            infoCell.addElement(infoPara);
            barTable.addCell(infoCell);

            cell.addElement(barTable);
            chartTable.addCell(cell);
        }

        document.add(chartTable);

        // Tabla adicional con modalidades top por estado
        InstitutionalPdfHeader.addSubsectionTitle(document, "Modalidades Más Comunes por Estado");

        PdfPTable topModalitiesTable = new PdfPTable(2);
        topModalitiesTable.setWidthPercentage(90);
        topModalitiesTable.setSpacingBefore(10);
        topModalitiesTable.setSpacingAfter(15);
        topModalitiesTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        boolean hasTopModalities = false;
        for (StudentListingReportDTO.StatusStatisticsDTO stat : report.getStatusStatistics()) {
            if (stat.getTopModalities() != null && !stat.getTopModalities().isEmpty()) {
                hasTopModalities = true;

                PdfPCell stateCell = new PdfPCell(new Phrase(
                        stat.getStatusDescription() != null ? stat.getStatusDescription() : "N/D",
                        InstitutionalPdfHeader.BOLD_FONT));
                stateCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
                stateCell.setPadding(6);
                stateCell.setBorder(Rectangle.BOX);
                stateCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
                topModalitiesTable.addCell(stateCell);

                StringBuilder modalities = new StringBuilder();
                for (int i = 0; i < Math.min(3, stat.getTopModalities().size()); i++) {
                    if (i > 0) modalities.append("\n");
                    modalities.append("• ").append(stat.getTopModalities().get(i));
                }

                PdfPCell modalitiesCell = new PdfPCell(new Phrase(modalities.toString(), InstitutionalPdfHeader.SMALL_FONT));
                modalitiesCell.setPadding(6);
                modalitiesCell.setBorder(Rectangle.BOX);
                modalitiesCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
                topModalitiesTable.addCell(modalitiesCell);
            }
        }

        if (hasTopModalities) {
            document.add(topModalitiesTable);
        } else {
            Paragraph noDataPara = new Paragraph(
                    "No hay información detallada de modalidades por estado disponible.",
                    InstitutionalPdfHeader.SMALL_FONT);
            noDataPara.setSpacingBefore(5);
            noDataPara.setSpacingAfter(15);
            document.add(noDataPara);
        }
    }

    /**
     * Estadísticas por semestre
     */
    private void addSemesterStatistics(Document document, StudentListingReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "7. ESTADÍSTICAS POR SEMESTRE ACADÉMICO");

        if (report.getSemesterStatistics() == null || report.getSemesterStatistics().isEmpty()) {
            document.add(new Paragraph("No hay estadísticas por semestre disponibles.", InstitutionalPdfHeader.NORMAL_FONT));
            return;
        }

        // Agregar resumen introductorio
        int totalSemesters = report.getSemesterStatistics().size();
        int totalStarted = report.getSemesterStatistics().stream()
                .mapToInt(s -> s.getModalitiesStarted() != null ? s.getModalitiesStarted() : 0)
                .sum();
        int totalCompleted = report.getSemesterStatistics().stream()
                .mapToInt(s -> s.getModalitiesCompleted() != null ? s.getModalitiesCompleted() : 0)
                .sum();

        Paragraph intro = new Paragraph(
                String.format("Análisis de %d semestres académicos: %d modalidades iniciadas, %d completadas",
                        totalSemesters, totalStarted, totalCompleted),
                InstitutionalPdfHeader.NORMAL_FONT
        );
        intro.setSpacingAfter(15);
        document.add(intro);

        // Tabla de estadísticas mejorada
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1f, 1.2f, 1.2f, 1.2f, 1f, 2f});
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);
        table.setHeaderRows(1);

        // Encabezados
        InstitutionalPdfHeader.addTableHeader(table, "Semestre");
        InstitutionalPdfHeader.addTableHeader(table, "Estudiantes");
        InstitutionalPdfHeader.addTableHeader(table, "Iniciadas");
        InstitutionalPdfHeader.addTableHeader(table, "Completadas");
        InstitutionalPdfHeader.addTableHeader(table, "Tasa Complet.");
        InstitutionalPdfHeader.addTableHeader(table, "GPA Prom.");
        InstitutionalPdfHeader.addTableHeader(table, "Modalidades Top");

        // Datos
        boolean alternate = false;
        for (StudentListingReportDTO.SemesterStatisticsDTO stat : report.getSemesterStatistics()) {
            // Semestre
            String semesterLabel = stat.getSemester() != null ? stat.getSemester() : "N/D";
            if (stat.getYear() != null) {
                semesterLabel += "-" + stat.getYear();
            }
            InstitutionalPdfHeader.addTableCell(table, semesterLabel, alternate);

            // Estudiantes
            Integer students = stat.getStudentCount() != null ? stat.getStudentCount() : 0;
            InstitutionalPdfHeader.addTableCell(table, String.valueOf(students), alternate);

            // Iniciadas
            Integer started = stat.getModalitiesStarted() != null ? stat.getModalitiesStarted() : 0;
            InstitutionalPdfHeader.addTableCell(table, String.valueOf(started), alternate);

            // Completadas
            Integer completed = stat.getModalitiesCompleted() != null ? stat.getModalitiesCompleted() : 0;
            InstitutionalPdfHeader.addTableCell(table, String.valueOf(completed), alternate);

            // Tasa de completación
            Double completionRate = stat.getCompletionRate() != null ? stat.getCompletionRate() : 0.0;
            InstitutionalPdfHeader.addTableCell(table, String.format("%.1f%%", completionRate), alternate);

            // GPA promedio
            Double avgGPA = stat.getAverageGPA() != null ? stat.getAverageGPA() : 0.0;
            InstitutionalPdfHeader.addTableCell(table, String.format("%.2f", avgGPA), alternate);

            // Top modalidades
            String topModalities = "N/D";
            if (stat.getTopModalityTypes() != null && !stat.getTopModalityTypes().isEmpty()) {
                topModalities = stat.getTopModalityTypes().stream()
                        .limit(2)
                        .map(m -> InstitutionalPdfHeader.truncate(m, 15))
                        .collect(java.util.stream.Collectors.joining(", "));
            }
            InstitutionalPdfHeader.addTableCell(table, topModalities, alternate);

            alternate = !alternate;
        }

        document.add(table);

        // Gráfico de tendencia de completación
        InstitutionalPdfHeader.addSubsectionTitle(document, "Tendencia de Tasa de Completación por Semestre");

        PdfPTable trendTable = new PdfPTable(1);
        trendTable.setWidthPercentage(100);
        trendTable.setSpacingBefore(10);
        trendTable.setSpacingAfter(15);

        // Encontrar máxima tasa para escalar
        double maxRate = report.getSemesterStatistics().stream()
                .mapToDouble(s -> s.getCompletionRate() != null ? s.getCompletionRate() : 0.0)
                .max()
                .orElse(100.0);

        for (StudentListingReportDTO.SemesterStatisticsDTO stat : report.getSemesterStatistics()) {
            PdfPCell trendCell = new PdfPCell();
            trendCell.setPadding(3);
            trendCell.setBorder(Rectangle.NO_BORDER);

            PdfPTable innerTable = new PdfPTable(2);
            try {
                innerTable.setWidths(new float[]{1.5f, 5.5f});
            } catch (DocumentException e) {
                // Ignorar
            }

            // Etiqueta
            String semLabel = stat.getSemester() != null ? stat.getSemester() : "N/D";
            if (stat.getYear() != null) {
                semLabel += "-" + stat.getYear();
            }
            PdfPCell labelCell = new PdfPCell(new Phrase(semLabel, InstitutionalPdfHeader.SMALL_FONT));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            labelCell.setPadding(3);
            innerTable.addCell(labelCell);

            // Barra de completación
            Double rate = stat.getCompletionRate() != null ? stat.getCompletionRate() : 0.0;
            float barPercentage = maxRate > 0 ? (float) (rate / maxRate) : 0;

            // Color basado en la tasa: verde para alto, amarillo para medio, rojo para bajo
            BaseColor barColor = rate >= 70 ? InstitutionalPdfHeader.INST_GOLD :
                                 rate >= 40 ? new BaseColor(255, 193, 7) :
                                 InstitutionalPdfHeader.INST_RED;

            PdfPCell barCell = createBarCell(
                    String.format("%.1f%% (%d/%d)", rate,
                            stat.getModalitiesCompleted() != null ? stat.getModalitiesCompleted() : 0,
                            stat.getModalitiesStarted() != null ? stat.getModalitiesStarted() : 0),
                    barPercentage,
                    barColor
            );
            innerTable.addCell(barCell);

            trendCell.addElement(innerTable);
            trendTable.addCell(trendCell);
        }

        document.add(trendTable);

        // Nota explicativa
        Paragraph note = new Paragraph(
                "La tasa de completación indica el porcentaje de modalidades completadas respecto " +
                "a las iniciadas en cada semestre. Los colores indican: Dorado = Excelente (≥70%), " +
                "Amarillo = Medio (40-69%), Rojo = Bajo (<40%).",
                InstitutionalPdfHeader.TINY_FONT
        );
        note.setSpacingBefore(10);
        note.setIndentationLeft(15);
        note.setIndentationRight(15);
        document.add(note);
    }

    // ==================== NUEVOS MÉTODOS PARA VISUALIZACIONES MEJORADAS ====================

    /**
     * Agregar tarjetas de resumen de estadísticas generales
     */
    private void addGeneralStatsSummaryCards(Document document,
                                            StudentListingReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        PdfPTable cardsTable = new PdfPTable(4);
        cardsTable.setWidthPercentage(100);
        cardsTable.setSpacingBefore(10);
        cardsTable.setSpacingAfter(20);

        // Total estudiantes - CORREGIDO: usar totalStudents del DTO
        Integer totalStudents = stats.getTotalStudents() != null ? stats.getTotalStudents() : 0;
        addSummaryCardWithIcon(cardsTable, "Total Estudiantes",
                String.valueOf(totalStudents), InstitutionalPdfHeader.INST_GOLD);

        // Tasa de asignación de directores
        int studentsWithDir = stats.getStudentsWithDirector() != null ? stats.getStudentsWithDirector() : 0;
        int studentsWithoutDir = stats.getStudentsWithoutDirector() != null ? stats.getStudentsWithoutDirector() : 0;
        int total = studentsWithDir + studentsWithoutDir;
        double directorRate = total > 0 ? (double) studentsWithDir / total * 100 : 0;
        addSummaryCardWithIcon(cardsTable, "Con Director",
                String.format("%.0f%%", directorRate), InstitutionalPdfHeader.INST_GOLD);

        // Estudiantes a tiempo
        int onTime = stats.getStudentsOnTime() != null ? stats.getStudentsOnTime() : 0;
        int atRisk = stats.getStudentsAtRisk() != null ? stats.getStudentsAtRisk() : 0;
        int delayed = stats.getStudentsDelayed() != null ? stats.getStudentsDelayed() : 0;
        int totalTimeline = onTime + atRisk + delayed;
        double onTimeRate = totalTimeline > 0 ? (double) onTime / totalTimeline * 100 : 0;
        addSummaryCardWithIcon(cardsTable, "A Tiempo",
                String.format("%.0f%%", onTimeRate), InstitutionalPdfHeader.INST_GOLD);

        // Promedio académico
        Double avgGPA = stats.getAverageCumulativeGPA() != null ? stats.getAverageCumulativeGPA() : 0.0;
        addSummaryCardWithIcon(cardsTable, "Promedio GPA",
                String.format("%.2f", avgGPA), InstitutionalPdfHeader.INST_RED);

        document.add(cardsTable);
    }

    /**
     * Agregar tarjeta resumen (sin emoji - solo texto)
     */
    private void addSummaryCardWithIcon(PdfPTable table, String label, String value,
                                        BaseColor color) {
        PdfPCell card = new PdfPCell();
        card.setPadding(15);
        card.setBorderColor(color);
        card.setBorderWidth(2f);
        card.setBackgroundColor(InstitutionalPdfHeader.WHITE);
        card.setMinimumHeight(75);

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
     * Gráfico comparativo de modalidades individuales vs grupales
     */
    private void addModalityComparisonChart(Document document,
                                           StudentListingReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        Integer individualCount = stats.getIndividualModalities() != null ? stats.getIndividualModalities() : 0;
        Integer groupCount = stats.getGroupModalities() != null ? stats.getGroupModalities() : 0;
        int total = individualCount + groupCount;

        if (total == 0) {
            Paragraph noDataPara = new Paragraph("No hay datos de modalidades individuales/grupales disponibles.",
                    InstitutionalPdfHeader.SMALL_FONT);
            noDataPara.setSpacingBefore(5);
            noDataPara.setSpacingAfter(15);
            document.add(noDataPara);
            return;
        }

        InstitutionalPdfHeader.addSubsectionTitle(document, "📊 Comparativa Individual vs Grupal");

        PdfPTable compTable = new PdfPTable(2);
        compTable.setWidthPercentage(100);
        compTable.setSpacingBefore(10);
        compTable.setSpacingAfter(20);

        // Modalidades individuales
        PdfPCell individualCell = new PdfPCell();
        individualCell.setPadding(15);
        individualCell.setBackgroundColor(InstitutionalPdfHeader.INST_GOLD);
        individualCell.setBorder(Rectangle.NO_BORDER);

        Paragraph individualContent = new Paragraph();
        individualContent.add(new Chunk("INDIVIDUALES\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, InstitutionalPdfHeader.WHITE)));
        individualContent.add(new Chunk(individualCount + " estudiantes\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, InstitutionalPdfHeader.WHITE)));
        individualContent.add(new Chunk(String.format("%.1f%% del total", (double) individualCount / total * 100),
                FontFactory.getFont(FontFactory.HELVETICA, 10, InstitutionalPdfHeader.WHITE)));
        individualContent.setAlignment(Element.ALIGN_CENTER);
        individualCell.addElement(individualContent);
        compTable.addCell(individualCell);

        // Modalidades grupales
        PdfPCell groupCell = new PdfPCell();
        groupCell.setPadding(15);
        groupCell.setBackgroundColor(InstitutionalPdfHeader.INST_RED);
        groupCell.setBorder(Rectangle.NO_BORDER);

        Paragraph groupContent = new Paragraph();
        groupContent.add(new Chunk("GRUPALES\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, InstitutionalPdfHeader.WHITE)));
        groupContent.add(new Chunk(groupCount + " estudiantes\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, InstitutionalPdfHeader.WHITE)));
        groupContent.add(new Chunk(String.format("%.1f%% del total", (double) groupCount / total * 100),
                FontFactory.getFont(FontFactory.HELVETICA, 10, InstitutionalPdfHeader.WHITE)));
        groupContent.setAlignment(Element.ALIGN_CENTER);
        groupCell.addElement(groupContent);
        compTable.addCell(groupCell);

        document.add(compTable);
    }

    /**
     * Gráfico de estado temporal con barras proporcionales
     */
    private void addTimelineStatusChart(Document document,
                                       StudentListingReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        Integer onTimeCount = stats.getStudentsOnTime() != null ? stats.getStudentsOnTime() : 0;
        Integer atRiskCount = stats.getStudentsAtRisk() != null ? stats.getStudentsAtRisk() : 0;
        Integer delayedCount = stats.getStudentsDelayed() != null ? stats.getStudentsDelayed() : 0;
        int total = onTimeCount + atRiskCount + delayedCount;

        if (total == 0) {
            Paragraph noDataPara = new Paragraph("No hay datos de estado temporal disponibles.",
                    InstitutionalPdfHeader.SMALL_FONT);
            noDataPara.setSpacingBefore(5);
            noDataPara.setSpacingAfter(15);
            document.add(noDataPara);
            return;
        }

        InstitutionalPdfHeader.addSubsectionTitle(document, "📈 Gráfico de Estado de Avance");

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(15);

        // A tiempo
        float onTimePct = total > 0 ? (float) onTimeCount / total : 0;
        InstitutionalPdfHeader.addBarRow(chartTable, "A Tiempo", String.valueOf(onTimeCount), onTimeCount + " (" + String.format("%.1f%%", onTimePct * 100) + ")", onTimePct, InstitutionalPdfHeader.INST_GOLD);

        // En riesgo
        float atRiskPct = total > 0 ? (float) atRiskCount / total : 0;
        InstitutionalPdfHeader.addBarRow(chartTable, "En Riesgo", String.valueOf(atRiskCount), atRiskCount + " (" + String.format("%.1f%%", atRiskPct * 100) + ")", atRiskPct, new BaseColor(255, 152, 0));

        // Retrasados
        float delayedPct = total > 0 ? (float) delayedCount / total : 0;
        InstitutionalPdfHeader.addBarRow(chartTable, "Retrasados", String.valueOf(delayedCount), delayedCount + " (" + String.format("%.1f%%", delayedPct * 100) + ")", delayedPct, InstitutionalPdfHeader.INST_RED);

        document.add(chartTable);
    }

    /**
     * Agregar indicadores de rendimiento visual
     */
    private void addPerformanceIndicators(Document document,
                                         StudentListingReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        InstitutionalPdfHeader.addSubsectionTitle(document, "🎯 Indicadores de Rendimiento");

        PdfPTable indicatorsTable = new PdfPTable(3);
        indicatorsTable.setWidthPercentage(100);
        indicatorsTable.setSpacingBefore(10);
        indicatorsTable.setSpacingAfter(15);

        // GPA Promedio con indicador visual
        Double avgGPA = stats.getAverageCumulativeGPA() != null ? stats.getAverageCumulativeGPA() : 0.0;
        addPerformanceIndicator(indicatorsTable, "Promedio GPA",
                avgGPA, 5.0, "GPA");

        // Créditos completados - Calculamos el porcentaje real si hay datos
        Double avgCredits = stats.getAverageCompletedCredits() != null ? stats.getAverageCompletedCredits() : 0.0;
        // Asumimos que el programa tiene aproximadamente 160-180 créditos
        // Limitamos el porcentaje a 100% máximo
        double creditPercentage = Math.min((avgCredits / 170.0) * 100, 100);
        addPerformanceIndicator(indicatorsTable, "Avance Créditos",
                creditPercentage, 100, "%");

        // Eficiencia temporal basada en días promedio
        // Menos días = mejor eficiencia
        Double avgDays = stats.getAverageDaysInModality() != null ? stats.getAverageDaysInModality() : 0.0;
        // Calculamos eficiencia: óptimo = 180 días (6 meses), máximo razonable = 730 días (2 años)
        // Invertimos la escala: menos días = mayor eficiencia
        double efficiencyPercentage = 0;
        if (avgDays > 0) {
            if (avgDays <= 180) {
                efficiencyPercentage = 100; // Excelente
            } else if (avgDays <= 365) {
                efficiencyPercentage = 100 - ((avgDays - 180) / 185 * 30); // 70-100%
            } else if (avgDays <= 730) {
                efficiencyPercentage = 70 - ((avgDays - 365) / 365 * 70); // 0-70%
            } else {
                efficiencyPercentage = 0; // Muy retrasado
            }
        }
        addPerformanceIndicator(indicatorsTable, "Eficiencia Temporal",
                Math.max(0, efficiencyPercentage), 100, "%");

        document.add(indicatorsTable);
    }

    /**
     * Agregar indicador de rendimiento individual
     */
    private void addPerformanceIndicator(PdfPTable table, String label,
                                        double value, double maxValue, String unit) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        cell.setBackgroundColor(InstitutionalPdfHeader.WHITE);

        // Etiqueta
        Paragraph labelPara = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, InstitutionalPdfHeader.TEXT_BLACK));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);

        // Barra de progreso
        float percentage = (float) (value / maxValue);
        BaseColor barColor = percentage >= 0.7 ? InstitutionalPdfHeader.INST_GOLD : InstitutionalPdfHeader.INST_RED;

        PdfPTable progressBar = new PdfPTable(2);
        float barWidth = Math.max(percentage * 100, 2);
        float emptyWidth = 100 - barWidth;

        try {
            progressBar.setWidths(new float[]{barWidth, emptyWidth});
        } catch (DocumentException e) {
            // Ignorar
        }
        progressBar.setWidthPercentage(100);
        progressBar.setSpacingBefore(5);
        progressBar.setSpacingAfter(5);

        PdfPCell filled = new PdfPCell();
        filled.setBackgroundColor(barColor);
        filled.setBorder(Rectangle.NO_BORDER);
        filled.setFixedHeight(15);
        progressBar.addCell(filled);

        PdfPCell empty = new PdfPCell();
        empty.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        empty.setBorder(Rectangle.NO_BORDER);
        empty.setFixedHeight(15);
        progressBar.addCell(empty);

        cell.addElement(progressBar);

        // Valor
        String valueText = unit.equals("GPA") ? String.format("%.2f", value) :
                          String.format("%.0f%s", value, unit);
        Paragraph valuePara = new Paragraph(valueText,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, barColor));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);

        table.addCell(cell);
    }

    /**
     * Agregar tarjetas de resumen de distribución
     */
    /**
     * Gráfico de distribución mejorado con diseño profesional
     */
    private void addEnhancedDistributionChart(Document document, Map<String, Integer> data,
                                             Map<String, Double> percentages, BaseColor color)
            throws DocumentException {

        if (data == null || data.isEmpty()) return;

        int maxValue = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int totalItems = data.values().stream().mapToInt(Integer::intValue).sum();

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(15);

        // Ordenar por valor descendente
        data.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10) // Top 10
            .forEach(entry -> {
                String label = InstitutionalPdfHeader.truncate(entry.getKey(), 40);
                int value = entry.getValue();
                float pct = maxValue > 0 ? (float) value / maxValue : 0;
                Double mapPct = percentages != null ? percentages.get(entry.getKey()) : null;
                String valueText = value + " (" + (mapPct != null ? String.format("%.1f%%", mapPct) : "N/D") + ")";
                InstitutionalPdfHeader.addBarRow(chartTable, label, String.valueOf(value), valueText, pct, color);
            });

        document.add(chartTable);

        // Agregar total al final
        PdfPTable totalTable = new PdfPTable(1);
        totalTable.setWidthPercentage(90);
        totalTable.setSpacingBefore(5);
        totalTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell totalCell = new PdfPCell();
        totalCell.setBackgroundColor(color);
        totalCell.setPadding(8);
        totalCell.setBorder(Rectangle.NO_BORDER);

        Paragraph totalText = new Paragraph("TOTAL: " + totalItems + " estudiantes",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, InstitutionalPdfHeader.WHITE));
        totalText.setAlignment(Element.ALIGN_CENTER);
        totalCell.addElement(totalText);
        totalTable.addCell(totalCell);

        document.add(totalTable);
    }

    // ==================== FIN DE NUEVOS MÉTODOS ====================

    // ==================== MÉTODOS AUXILIARES ====================



    /**
     * Crear celda de barra
     */
    private PdfPCell createBarCell(String label, float percentage, BaseColor color) {
        PdfPTable barContainer = new PdfPTable(2);
        try {
            float barWidth = Math.max(percentage * 100, 1); // Mínimo 1%
            float emptyWidth = Math.max((1 - percentage) * 100, 1);
            barContainer.setWidths(new float[]{barWidth, emptyWidth});
        } catch (DocumentException e) {
            // Ignorar
        }
        barContainer.setWidthPercentage(100);

        // Parte coloreada
        PdfPCell filledCell = new PdfPCell(new Phrase(label,
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, InstitutionalPdfHeader.WHITE)));
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(3);
        filledCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        barContainer.addCell(filledCell);

        // Parte vacía
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        emptyCell.setBorder(Rectangle.NO_BORDER);
        barContainer.addCell(emptyCell);

        PdfPCell containerCell = new PdfPCell();
        containerCell.addElement(barContainer);
        containerCell.setBorder(Rectangle.NO_BORDER);
        containerCell.setPadding(0);

        return containerCell;
    }

    /**
     * Tarjeta de estadística mejorada con etiqueta más visible
     */
    private void addStatsCard(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(color);
        cell.setPadding(15);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setMinimumHeight(70);

        // Etiqueta primero (más prominente)
        Paragraph labelPara = new Paragraph(label,
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        labelPara.setSpacingAfter(5);
        cell.addElement(labelPara);

        // Valor grande
        Paragraph valuePara = new Paragraph(value,
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.WHITE));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        valuePara.setSpacingAfter(3);
        cell.addElement(valuePara);

        // Texto "estudiantes" pequeño para dar contexto
        Paragraph unitPara = new Paragraph("estudiantes",
            FontFactory.getFont(FontFactory.HELVETICA, 7, InstitutionalPdfHeader.LIGHT_GRAY));
        unitPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(unitPara);

        table.addCell(cell);
    }
















    /**
     * Traduce estado temporal
     */
    private String translateTimelineStatus(String status) {
        if (status == null) return "N/D";
        switch (status) {
            case "ON_TIME": return "A Tiempo";
            case "AT_RISK": return "En Riesgo";
            case "DELAYED": return "Retrasado";
            case "COMPLETED": return "Completado";
            default: return status;
        }
    }




}

