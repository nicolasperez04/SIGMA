package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.report.dto.StudentListingReportDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio para generar PDF del reporte de listado de estudiantes con filtros
 * Diseño profesional y universitario con máxima información relevante
 */
@Service
public class StudentListingPdfGenerator {

    // COLORES INSTITUCIONALES - USO EXCLUSIVO
    private static final BaseColor INSTITUTIONAL_RED = new BaseColor(143, 30, 30); // #8F1E1E - Color primario
    private static final BaseColor INSTITUTIONAL_GOLD = new BaseColor(213, 203, 160); // #D5CBA0 - Color secundario
    private static final BaseColor WHITE = BaseColor.WHITE; // Color primario
    private static final BaseColor LIGHT_GOLD = new BaseColor(245, 242, 235); // Tono muy claro de dorado para fondos sutiles
    private static final BaseColor TEXT_BLACK = BaseColor.BLACK; // Texto principal
    private static final BaseColor TEXT_GRAY = new BaseColor(80, 80, 80); // Texto secundario

    // Fuentes con colores institucionales
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, INSTITUTIONAL_RED);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, INSTITUTIONAL_RED);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, INSTITUTIONAL_RED);
    private static final Font SUBHEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, INSTITUTIONAL_RED);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_BLACK);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_BLACK);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_GRAY);
    private static final Font TINY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_GRAY);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, WHITE);
    private static final Font TABLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_BLACK);

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ByteArrayOutputStream generatePDF(StudentListingReportDTO report)
            throws DocumentException, IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 40, 40, 50, 50); // Landscape para más espacio
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);

        // Agregar eventos de página
        StudentListingPageEventHelper pageEvent = new StudentListingPageEventHelper(report);
        writer.setPageEvent(pageEvent);

        document.open();

        // 1. Portada
        addCoverPage(document, report);

        // 2. Filtros Aplicados y Resumen Ejecutivo
        document.newPage();
        addInternalHeader(document, report);
        addFiltersAndExecutiveSummary(document, report);

        // 3. Estadísticas Generales
        document.newPage();
        addInternalHeader(document, report);
        addGeneralStatistics(document, report);

        // 4. Análisis de Distribución
        document.newPage();
        addInternalHeader(document, report);
        addDistributionAnalysis(document, report);

        // 5. Listado Detallado de Estudiantes
        document.newPage();
        addInternalHeader(document, report);
        addStudentListing(document, report);

        // 6. Estadísticas por Modalidad
        document.newPage();
        addInternalHeader(document, report);
        addModalityStatistics(document, report);

        // 7. Estadísticas por Estado
        document.newPage();
        addInternalHeader(document, report);
        addStatusStatistics(document, report);

        // 8. Estadísticas por Semestre
        document.newPage();
        addInternalHeader(document, report);
        addSemesterStatistics(document, report);

        // 9. Pie institucional de cierre
        addFooterSection(document, report);

        document.close();
        return outputStream;
    }

    /**
     * Portada del reporte
     */
    private void addCoverPage(Document document, StudentListingReportDTO report)
            throws DocumentException, IOException {

        // 1. Encabezado con logo institucional
        InstitutionalPdfHeader.addHeader(
                document,
                "Facultad de Ingeniería",
                report.getAcademicProgramName() + (report.getAcademicProgramCode() != null
                        ? " — Cód. " + report.getAcademicProgramCode() : ""),
                "Reporte de Listado de Estudiantes"
        );

        // 2. Caja de título principal roja
        PdfPTable titleBox = new PdfPTable(1);
        titleBox.setWidthPercentage(100);
        titleBox.setSpacingAfter(18);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(INSTITUTIONAL_RED);
        titleCell.setPadding(18);
        titleCell.setBorder(Rectangle.NO_BORDER);

        Paragraph titlePara = new Paragraph("REPORTE DE LISTADO DE ESTUDIANTES\nModalidades de Grado",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, WHITE));
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(titlePara);
        titleBox.addCell(titleCell);
        document.add(titleBox);

        // 3. Línea dorada decorativa
        InstitutionalPdfHeader.addGoldLine(document);

        // 4. Tabla de información con bordes dorados
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(85);
        infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        infoTable.setSpacingBefore(18);
        infoTable.setSpacingAfter(22);
        try { infoTable.setWidths(new float[]{42f, 58f}); } catch (DocumentException ignored) {}

        addCoverInfoRow(infoTable, "Programa académico:", report.getAcademicProgramName());
        if (report.getAcademicProgramCode() != null) {
            addCoverInfoRow(infoTable, "Código del programa:", report.getAcademicProgramCode());
        }
        addCoverInfoRow(infoTable, "Fecha de generación:",
                report.getGeneratedAt().format(DATETIME_FORMATTER));
        addCoverInfoRow(infoTable, "Generado por:", report.getGeneratedBy());
        int totalStudents = report.getStudents() != null ? report.getStudents().size() : 0;
        addCoverInfoRow(infoTable, "Total de estudiantes:", String.valueOf(totalStudents));
        addCoverInfoRow(infoTable, "Filtros aplicados:",
                (report.getAppliedFilters() != null && report.getAppliedFilters().getHasFilters())
                        ? "Sí" : "No — Listado completo");
        document.add(infoTable);

        // 5. Líneas de cierre institucionales
        InstitutionalPdfHeader.addRedLine(document);
        InstitutionalPdfHeader.addGoldLine(document);

        // 6. Nota informativa
        addSpacingParagraph(document, 10f);
        Paragraph disclaimer = new Paragraph(
                "Este reporte presenta un listado detallado de estudiantes con sus modalidades de grado, " +
                "incluyendo información académica, estado de avance, directores asignados y estadísticas " +
                "generales. La información es generada automáticamente por el Sistema SIGMA.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, TEXT_GRAY));
        disclaimer.setAlignment(Element.ALIGN_JUSTIFIED);
        disclaimer.setIndentationLeft(40);
        disclaimer.setIndentationRight(40);
        document.add(disclaimer);

        addSpacingParagraph(document, 14f);
        Paragraph closing = new Paragraph(
                "Sistema Integral de Gestión de Modalidades de Grado — SIGMA\n" +
                "Universidad Surcolombiana | Facultad de Ingeniería | Neiva – Huila",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY));
        closing.setAlignment(Element.ALIGN_CENTER);
        document.add(closing);
    }

    /**
     * Encabezado compacto institucional para páginas internas.
     */
    private void addInternalHeader(Document document, StudentListingReportDTO report)
            throws DocumentException {
        PdfPTable strip = new PdfPTable(2);
        strip.setWidthPercentage(100);
        strip.setSpacingAfter(8f);
        try { strip.setWidths(new float[]{65f, 35f}); } catch (DocumentException ignored) {}

        PdfPCell leftCell = new PdfPCell(new Phrase(
                "UNIVERSIDAD SURCOLOMBIANA — SIGMA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INSTITUTIONAL_RED)));
        leftCell.setBorder(Rectangle.BOTTOM);
        leftCell.setBorderColorBottom(INSTITUTIONAL_RED);
        leftCell.setBorderWidthBottom(1.5f);
        leftCell.setPadding(4f);
        strip.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell(new Phrase(
                "Listado de Estudiantes — " + report.getAcademicProgramName(),
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY)));
        rightCell.setBorder(Rectangle.BOTTOM);
        rightCell.setBorderColorBottom(INSTITUTIONAL_GOLD);
        rightCell.setBorderWidthBottom(1.5f);
        rightCell.setPadding(4f);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        strip.addCell(rightCell);

        document.add(strip);
    }

    /**
     * Pie institucional de cierre del reporte.
     */
    private void addFooterSection(Document document, StudentListingReportDTO report)
            throws DocumentException {
        addSpacingParagraph(document, 20f);
        InstitutionalPdfHeader.addRedLine(document);
        InstitutionalPdfHeader.addGoldLine(document);
        addSpacingParagraph(document, 8f);

        PdfPTable noteTable = new PdfPTable(1);
        noteTable.setWidthPercentage(100);

        PdfPCell noteCell = new PdfPCell();
        noteCell.setBackgroundColor(LIGHT_GOLD);
        noteCell.setPadding(12f);
        noteCell.setBorder(Rectangle.NO_BORDER);

        Paragraph note = new Paragraph(
                "Este reporte fue generado automáticamente por el sistema SIGMA a partir de los datos académicos " +
                "registrados para el programa: " + report.getAcademicProgramName() + ". " +
                "Para consultas o modificaciones del listado, contacte con la coordinación del programa académico.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY));
        note.setAlignment(Element.ALIGN_JUSTIFIED);
        noteCell.addElement(note);
        noteTable.addCell(noteCell);
        document.add(noteTable);

        addSpacingParagraph(document, 10f);
        Paragraph closing = new Paragraph(
                "Sistema Integral de Gestión de Modalidades de Grado — SIGMA\n" +
                "Universidad Surcolombiana | Facultad de Ingeniería | Neiva – Huila",
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_GRAY));
        closing.setAlignment(Element.ALIGN_CENTER);
        document.add(closing);
    }

    /**
     * Fila de la tabla de portada con estilo institucional.
     */
    private void addCoverInfoRow(PdfPTable table, String label, String value) {
        if (value == null) value = "—";

        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_GRAY)));
        labelCell.setBackgroundColor(LIGHT_GOLD);
        labelCell.setPadding(8f);
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderColor(INSTITUTIONAL_GOLD);
        labelCell.setBorderWidth(0.8f);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value,
                FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_BLACK)));
        valueCell.setPadding(8f);
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(INSTITUTIONAL_GOLD);
        valueCell.setBorderWidth(0.8f);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(valueCell);
    }

    /** Párrafo de espaciado auxiliar. */
    private void addSpacingParagraph(Document document, float height) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(height);
        document.add(spacer);
    }

    /**
     * Filtros aplicados y resumen ejecutivo
     */
    private void addFiltersAndExecutiveSummary(Document document, StudentListingReportDTO report)
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

            StudentListingReportDTO.ExecutiveSummaryDTO summary = report.getExecutiveSummary();

            // Métricas principales
            PdfPTable metricsTable = new PdfPTable(5);
            metricsTable.setWidthPercentage(100);
            metricsTable.setSpacingBefore(10);
            metricsTable.setSpacingAfter(20);

            addMetricCard(metricsTable, "Total Estudiantes",
                String.valueOf(summary.getTotalStudents()), INSTITUTIONAL_RED);
            addMetricCard(metricsTable, "Modalidades Activas",
                String.valueOf(summary.getActiveModalities()), INSTITUTIONAL_GOLD);
            addMetricCard(metricsTable, "Completadas",
                String.valueOf(summary.getCompletedModalities()), INSTITUTIONAL_GOLD);
            addMetricCard(metricsTable, "Progreso Promedio",
                String.format("%.1f%%", summary.getAverageProgress()), INSTITUTIONAL_GOLD);
            addMetricCard(metricsTable, "Tipos de Modalidad",
                String.valueOf(summary.getDifferentModalityTypes()), INSTITUTIONAL_GOLD);

            document.add(metricsTable);

            // Información adicional
            PdfPTable detailTable = new PdfPTable(2);
            detailTable.setWidthPercentage(90);
            detailTable.setWidths(new float[]{1.5f, 2f});
            detailTable.setSpacingBefore(10);
            detailTable.setSpacingAfter(15);
            detailTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            addDetailRow(detailTable, "Modalidad Más Común:", summary.getMostCommonModalityType());
            addDetailRow(detailTable, "Estado Más Común:", summary.getMostCommonStatus());
            addDetailRow(detailTable, "Estados Diferentes:", String.valueOf(summary.getDifferentStatuses()));

            document.add(detailTable);
        }
    }

    /**
     * Estadísticas generales
     */
    private void addGeneralStatistics(Document document, StudentListingReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "2. ESTADÍSTICAS GENERALES");

        if (report.getGeneralStatistics() == null) {
            document.add(new Paragraph("No hay estadísticas disponibles.", NORMAL_FONT));
            return;
        }

        StudentListingReportDTO.GeneralStatisticsDTO stats = report.getGeneralStatistics();

        // Tarjetas de resumen principal
        addGeneralStatsSummaryCards(document, stats);

        // Estadísticas de modalidades mejoradas
        addSubsectionTitle(document, "📊 Distribución por Tipo de Modalidad");

        PdfPTable modalityTypeTable = new PdfPTable(4);
        modalityTypeTable.setWidthPercentage(100);
        modalityTypeTable.setSpacingBefore(10);
        modalityTypeTable.setSpacingAfter(20);

        // Modalidades individuales
        addStatsCard(modalityTypeTable, "Individuales",
            String.valueOf(stats.getIndividualModalities() != null ? stats.getIndividualModalities() : 0) + " modalidades",
            INSTITUTIONAL_GOLD);

        // Modalidades grupales
        addStatsCard(modalityTypeTable, "Grupales",
            String.valueOf(stats.getGroupModalities() != null ? stats.getGroupModalities() : 0) + " modalidades",
            INSTITUTIONAL_GOLD);

        // Con director
        addStatsCard(modalityTypeTable, "Con Director",
            String.valueOf(stats.getStudentsWithDirector() != null ? stats.getStudentsWithDirector() : 0) + " estudiantes",
            INSTITUTIONAL_GOLD);

        // Sin director
        addStatsCard(modalityTypeTable, "Sin Director",
            String.valueOf(stats.getStudentsWithoutDirector() != null ? stats.getStudentsWithoutDirector() : 0) + " estudiantes",
            INSTITUTIONAL_RED);

        document.add(modalityTypeTable);

        // NUEVO: Gráfico comparativo de modalidades
        addModalityComparisonChart(document, stats);

        // Estado de avance
        addSubsectionTitle(document, "⏱ Estado de Avance Temporal");

        PdfPTable timelineTable = new PdfPTable(3);
        timelineTable.setWidthPercentage(90);
        timelineTable.setSpacingBefore(10);
        timelineTable.setSpacingAfter(20);
        timelineTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addStatsCard(timelineTable, "A Tiempo",
            String.valueOf(stats.getStudentsOnTime() != null ? stats.getStudentsOnTime() : 0) + " estudiantes",
            INSTITUTIONAL_GOLD);
        addStatsCard(timelineTable, "En Riesgo",
            String.valueOf(stats.getStudentsAtRisk() != null ? stats.getStudentsAtRisk() : 0) + " estudiantes",
            INSTITUTIONAL_RED);
        addStatsCard(timelineTable, "Retrasados",
            String.valueOf(stats.getStudentsDelayed() != null ? stats.getStudentsDelayed() : 0) + " estudiantes",
            INSTITUTIONAL_RED);

        document.add(timelineTable);

        // NUEVO: Gráfico visual de estado temporal
        addTimelineStatusChart(document, stats);

        // Promedios académicos
        addSubsectionTitle(document, "📚 Indicadores Académicos Promedio");

        PdfPTable avgTable = new PdfPTable(2);
        avgTable.setWidthPercentage(80);
        avgTable.setWidths(new float[]{2f, 1f});
        avgTable.setSpacingBefore(10);
        avgTable.setSpacingAfter(15);
        avgTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addStatRow(avgTable, "Promedio Acumulado:",
            String.format("%.2f", stats.getAverageCumulativeGPA()));
        addStatRow(avgTable, "Créditos Completados (Promedio):",
            String.format("%.0f", stats.getAverageCompletedCredits()));
        addStatRow(avgTable, "Días en Modalidad (Promedio):",
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

        addSectionTitle(document, "3. ANÁLISIS DE DISTRIBUCIÓN");

        if (report.getDistributionAnalysis() == null) {
            document.add(new Paragraph("No hay análisis de distribución disponible.", NORMAL_FONT));
            return;
        }

        StudentListingReportDTO.DistributionAnalysisDTO distribution = report.getDistributionAnalysis();


        // Distribución por modalidad mejorada
        if (distribution.getByModalityType() != null && !distribution.getByModalityType().isEmpty()) {
            addSubsectionTitle(document, "📊 Distribución por Tipo de Modalidad");

            addEnhancedDistributionChart(document, distribution.getByModalityType(),
                distribution.getByModalityTypePercentage(), INSTITUTIONAL_RED);  // Rojo institucional
        }

        // Distribución por estado mejorada
        if (distribution.getByStatus() != null && !distribution.getByStatus().isEmpty()) {
            document.add(new Paragraph("\n"));
            addSubsectionTitle(document, "📌 Distribución por Estado");

            addEnhancedDistributionChart(document, distribution.getByStatus(),
                distribution.getByStatusPercentage(), INSTITUTIONAL_GOLD);  // Dorado institucional
        }

        // Distribución por estado temporal mejorada
        if (distribution.getByTimelineStatus() != null && !distribution.getByTimelineStatus().isEmpty()) {
            document.add(new Paragraph("\n"));
            addSubsectionTitle(document, "⏱ Distribución por Estado Temporal");

            addEnhancedDistributionChart(document, distribution.getByTimelineStatus(),
                distribution.getByTimelineStatusPercentage(), INSTITUTIONAL_GOLD);  // Dorado institucional
        }
    }

    /**
     * Listado detallado de estudiantes
     */
    private void addStudentListing(Document document, StudentListingReportDTO report)
            throws DocumentException {

        addSectionTitle(document, "4. LISTADO DETALLADO DE ESTUDIANTES");

        if (report.getStudents() == null || report.getStudents().isEmpty()) {
            document.add(new Paragraph("No hay estudiantes para mostrar.", NORMAL_FONT));
            return;
        }

        Paragraph totalInfo = new Paragraph(
                String.format("Total de estudiantes en el listado: %d", report.getStudents().size()),
                BOLD_FONT
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
        addTableHeader(table, "Estudiante");
        addTableHeader(table, "Código");
        addTableHeader(table, "Modalidad");
        addTableHeader(table, "Estado");
        addTableHeader(table, "Prog.");
        addTableHeader(table, "GPA");
        addTableHeader(table, "Director");
        addTableHeader(table, "Días");
        addTableHeader(table, "Estado Temp.");
        addTableHeader(table, "Grupo");
        addTableHeader(table, "Créditos");

        // Datos
        boolean alternate = false;
        for (StudentListingReportDTO.StudentDetailDTO student : report.getStudents()) {
            // Nombre completo
            addTableCell(table, truncate(student.getFullName(), 25), alternate);

            // Código
            addTableCell(table, student.getStudentCode() != null ? student.getStudentCode() : "N/D", alternate);

            // Modalidad
            addTableCell(table, truncate(student.getModalityType(), 25), alternate);

            // Estado
            addTableCell(table, truncate(student.getModalityStatusDescription() != null ?
                    student.getModalityStatusDescription() : "N/D", 18), alternate);

            // Progreso
            Double progress = student.getProgressPercentage() != null ? student.getProgressPercentage() : 0.0;
            addTableCell(table, String.format("%.0f%%", progress), alternate);

            // GPA
            Double gpa = student.getCumulativeAverage() != null ? student.getCumulativeAverage() : 0.0;
            addTableCell(table, String.format("%.2f", gpa), alternate);

            // Director
            addTableCell(table, student.getDirectorName() != null ?
                truncate(student.getDirectorName(), 18) : "Sin asignar", alternate);

            // Días en modalidad
            Integer days = student.getDaysInModality() != null ? student.getDaysInModality() : 0;
            addTableCell(table, String.valueOf(days), alternate);

            // Estado temporal
            addTableCell(table, translateTimelineStatus(student.getTimelineStatus()), alternate);

            // Tamaño del grupo
            Integer groupSize = student.getGroupSize() != null ? student.getGroupSize() : 1;
            String groupInfo = groupSize > 1 ? String.valueOf(groupSize) : "Ind.";
            addTableCell(table, groupInfo, alternate);

            // Créditos completados
            Integer credits = student.getCompletedCredits() != null ? student.getCompletedCredits() : 0;
            addTableCell(table, String.valueOf(credits), alternate);

            alternate = !alternate;
        }

        document.add(table);

        // Notas explicativas mejoradas
        document.add(new Paragraph("\n"));
        PdfPTable legendTable = new PdfPTable(1);
        legendTable.setWidthPercentage(100);

        PdfPCell legendCell = new PdfPCell();
        legendCell.setBackgroundColor(LIGHT_GOLD);
        legendCell.setPadding(8);
        legendCell.setBorder(Rectangle.BOX);
        legendCell.setBorderColor(INSTITUTIONAL_GOLD);

        Paragraph legendText = new Paragraph();
        legendText.add(new Chunk("LEYENDA: ", BOLD_FONT));
        legendText.add(new Chunk(
                "Prog. = Progreso | GPA = Promedio acumulado | " +
                "Estado Temp. = A Tiempo/En Riesgo/Retrasado | " +
                "Grupo = Tamaño del grupo (Ind. = Individual) | " +
                "Créditos = Créditos académicos completados",
                TINY_FONT
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

        addSectionTitle(document, "5. ESTADÍSTICAS POR TIPO DE MODALIDAD");

        if (report.getModalityStatistics() == null || report.getModalityStatistics().isEmpty()) {
            document.add(new Paragraph("No hay estadísticas por modalidad disponibles.", NORMAL_FONT));
            return;
        }

        // Agregar resumen introductorio
        Paragraph intro = new Paragraph(
                String.format("Se han identificado %d tipos de modalidades diferentes. " +
                        "A continuación se presenta el análisis detallado de cada una:",
                        report.getModalityStatistics().size()),
                NORMAL_FONT
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
        addTableHeader(table, "Tipo de Modalidad");
        addTableHeader(table, "Total Est.");
        addTableHeader(table, "Activos");
        addTableHeader(table, "Completados");
        addTableHeader(table, "Tasa Complet.");
        addTableHeader(table, "GPA Prom.");

        // Datos
        boolean alternate = false;
        for (StudentListingReportDTO.ModalityStatisticsDTO stat : report.getModalityStatistics()) {
            // Tipo de modalidad
            addTableCell(table, truncate(stat.getModalityType() != null ?
                    stat.getModalityType() : "Sin especificar", 35), alternate);

            // Total estudiantes
            Integer total = stat.getTotalStudents() != null ? stat.getTotalStudents() : 0;
            addTableCell(table, String.valueOf(total), alternate);

            // Activos
            Integer active = stat.getActiveStudents() != null ? stat.getActiveStudents() : 0;
            addTableCell(table, String.valueOf(active), alternate);

            // Completados
            Integer completed = stat.getCompletedStudents() != null ? stat.getCompletedStudents() : 0;
            addTableCell(table, String.valueOf(completed), alternate);

            // Tasa de completación
            Double completionRate = stat.getCompletionRate() != null ? stat.getCompletionRate() : 0.0;
            addTableCell(table, String.format("%.1f%%", completionRate), alternate);

            // GPA promedio
            Double avgGPA = stat.getAverageGPA() != null ? stat.getAverageGPA() : 0.0;
            addTableCell(table, String.format("%.2f", avgGPA), alternate);

            alternate = !alternate;
        }

        document.add(table);

        // Agregar análisis de top directores si está disponible
        for (StudentListingReportDTO.ModalityStatisticsDTO stat : report.getModalityStatistics()) {
            if (stat.getTopDirectors() != null && !stat.getTopDirectors().isEmpty()) {
                addSubsectionTitle(document, "Top Directores en " + stat.getModalityType());

                PdfPTable directorTable = new PdfPTable(1);
                directorTable.setWidthPercentage(90);
                directorTable.setSpacingBefore(5);
                directorTable.setSpacingAfter(10);
                directorTable.setHorizontalAlignment(Element.ALIGN_CENTER);

                for (String director : stat.getTopDirectors()) {
                    PdfPCell cell = new PdfPCell(new Phrase("• " + director, SMALL_FONT));
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

        addSectionTitle(document, "6. ESTADÍSTICAS POR ESTADO");

        if (report.getStatusStatistics() == null || report.getStatusStatistics().isEmpty()) {
            document.add(new Paragraph("No hay estadísticas por estado disponibles.", NORMAL_FONT));
            return;
        }

        // Agregar resumen introductorio
        int totalStudentsInStates = report.getStatusStatistics().stream()
                .mapToInt(s -> s.getStudentCount() != null ? s.getStudentCount() : 0)
                .sum();

        Paragraph intro = new Paragraph(
                String.format("Distribución de %d estudiantes en %d estados diferentes:",
                        totalStudentsInStates, report.getStatusStatistics().size()),
                NORMAL_FONT
        );
        intro.setSpacingAfter(15);
        document.add(intro);

        // Gráfico de barras mejorado
        addSubsectionTitle(document, "Distribución Visual de Estudiantes por Estado");

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
                truncate(statusLabel, 30),
                SMALL_FONT
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
                INSTITUTIONAL_GOLD
            );
            barTable.addCell(barCell);

            // Información adicional
            PdfPCell infoCell = new PdfPCell();
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            infoCell.setPadding(3);

            Paragraph infoPara = new Paragraph();
            infoPara.add(new Chunk(String.format("%.1f%%\n", pct),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INSTITUTIONAL_RED)));

            // Agregar días promedio si está disponible
            if (stat.getAverageDaysInStatus() != null && stat.getAverageDaysInStatus() > 0) {
                infoPara.add(new Chunk(String.format("(%.0f días)", stat.getAverageDaysInStatus()),
                        FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_GRAY)));
            }

            infoCell.addElement(infoPara);
            barTable.addCell(infoCell);

            cell.addElement(barTable);
            chartTable.addCell(cell);
        }

        document.add(chartTable);

        // Tabla adicional con modalidades top por estado
        addSubsectionTitle(document, "Modalidades Más Comunes por Estado");

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
                        BOLD_FONT));
                stateCell.setBackgroundColor(LIGHT_GOLD);
                stateCell.setPadding(6);
                stateCell.setBorder(Rectangle.BOX);
                stateCell.setBorderColor(INSTITUTIONAL_GOLD);
                topModalitiesTable.addCell(stateCell);

                StringBuilder modalities = new StringBuilder();
                for (int i = 0; i < Math.min(3, stat.getTopModalities().size()); i++) {
                    if (i > 0) modalities.append("\n");
                    modalities.append("• ").append(stat.getTopModalities().get(i));
                }

                PdfPCell modalitiesCell = new PdfPCell(new Phrase(modalities.toString(), SMALL_FONT));
                modalitiesCell.setPadding(6);
                modalitiesCell.setBorder(Rectangle.BOX);
                modalitiesCell.setBorderColor(INSTITUTIONAL_GOLD);
                topModalitiesTable.addCell(modalitiesCell);
            }
        }

        if (hasTopModalities) {
            document.add(topModalitiesTable);
        } else {
            Paragraph noDataPara = new Paragraph(
                    "No hay información detallada de modalidades por estado disponible.",
                    SMALL_FONT);
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

        addSectionTitle(document, "7. ESTADÍSTICAS POR SEMESTRE ACADÉMICO");

        if (report.getSemesterStatistics() == null || report.getSemesterStatistics().isEmpty()) {
            document.add(new Paragraph("No hay estadísticas por semestre disponibles.", NORMAL_FONT));
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
                NORMAL_FONT
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
        addTableHeader(table, "Semestre");
        addTableHeader(table, "Estudiantes");
        addTableHeader(table, "Iniciadas");
        addTableHeader(table, "Completadas");
        addTableHeader(table, "Tasa Complet.");
        addTableHeader(table, "GPA Prom.");
        addTableHeader(table, "Modalidades Top");

        // Datos
        boolean alternate = false;
        for (StudentListingReportDTO.SemesterStatisticsDTO stat : report.getSemesterStatistics()) {
            // Semestre
            String semesterLabel = stat.getSemester() != null ? stat.getSemester() : "N/D";
            if (stat.getYear() != null) {
                semesterLabel += "-" + stat.getYear();
            }
            addTableCell(table, semesterLabel, alternate);

            // Estudiantes
            Integer students = stat.getStudentCount() != null ? stat.getStudentCount() : 0;
            addTableCell(table, String.valueOf(students), alternate);

            // Iniciadas
            Integer started = stat.getModalitiesStarted() != null ? stat.getModalitiesStarted() : 0;
            addTableCell(table, String.valueOf(started), alternate);

            // Completadas
            Integer completed = stat.getModalitiesCompleted() != null ? stat.getModalitiesCompleted() : 0;
            addTableCell(table, String.valueOf(completed), alternate);

            // Tasa de completación
            Double completionRate = stat.getCompletionRate() != null ? stat.getCompletionRate() : 0.0;
            addTableCell(table, String.format("%.1f%%", completionRate), alternate);

            // GPA promedio
            Double avgGPA = stat.getAverageGPA() != null ? stat.getAverageGPA() : 0.0;
            addTableCell(table, String.format("%.2f", avgGPA), alternate);

            // Top modalidades
            String topModalities = "N/D";
            if (stat.getTopModalityTypes() != null && !stat.getTopModalityTypes().isEmpty()) {
                topModalities = stat.getTopModalityTypes().stream()
                        .limit(2)
                        .map(m -> truncate(m, 15))
                        .collect(java.util.stream.Collectors.joining(", "));
            }
            addTableCell(table, topModalities, alternate);

            alternate = !alternate;
        }

        document.add(table);

        // Gráfico de tendencia de completación
        addSubsectionTitle(document, "Tendencia de Tasa de Completación por Semestre");

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
            PdfPCell labelCell = new PdfPCell(new Phrase(semLabel, SMALL_FONT));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            labelCell.setPadding(3);
            innerTable.addCell(labelCell);

            // Barra de completación
            Double rate = stat.getCompletionRate() != null ? stat.getCompletionRate() : 0.0;
            float barPercentage = maxRate > 0 ? (float) (rate / maxRate) : 0;

            // Color basado en la tasa: verde para alto, amarillo para medio, rojo para bajo
            BaseColor barColor = rate >= 70 ? INSTITUTIONAL_GOLD :
                                 rate >= 40 ? new BaseColor(255, 193, 7) :
                                 INSTITUTIONAL_RED;

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
                TINY_FONT
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
                String.valueOf(totalStudents), INSTITUTIONAL_GOLD);

        // Tasa de asignación de directores
        int studentsWithDir = stats.getStudentsWithDirector() != null ? stats.getStudentsWithDirector() : 0;
        int studentsWithoutDir = stats.getStudentsWithoutDirector() != null ? stats.getStudentsWithoutDirector() : 0;
        int total = studentsWithDir + studentsWithoutDir;
        double directorRate = total > 0 ? (double) studentsWithDir / total * 100 : 0;
        addSummaryCardWithIcon(cardsTable, "Con Director",
                String.format("%.0f%%", directorRate), INSTITUTIONAL_GOLD);

        // Estudiantes a tiempo
        int onTime = stats.getStudentsOnTime() != null ? stats.getStudentsOnTime() : 0;
        int atRisk = stats.getStudentsAtRisk() != null ? stats.getStudentsAtRisk() : 0;
        int delayed = stats.getStudentsDelayed() != null ? stats.getStudentsDelayed() : 0;
        int totalTimeline = onTime + atRisk + delayed;
        double onTimeRate = totalTimeline > 0 ? (double) onTime / totalTimeline * 100 : 0;
        addSummaryCardWithIcon(cardsTable, "A Tiempo",
                String.format("%.0f%%", onTimeRate), INSTITUTIONAL_GOLD);

        // Promedio académico
        Double avgGPA = stats.getAverageCumulativeGPA() != null ? stats.getAverageCumulativeGPA() : 0.0;
        addSummaryCardWithIcon(cardsTable, "Promedio GPA",
                String.format("%.2f", avgGPA), INSTITUTIONAL_RED);

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
        card.setBackgroundColor(WHITE);
        card.setMinimumHeight(75);

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
                    SMALL_FONT);
            noDataPara.setSpacingBefore(5);
            noDataPara.setSpacingAfter(15);
            document.add(noDataPara);
            return;
        }

        addSubsectionTitle(document, "📊 Comparativa Individual vs Grupal");

        PdfPTable compTable = new PdfPTable(2);
        compTable.setWidthPercentage(100);
        compTable.setSpacingBefore(10);
        compTable.setSpacingAfter(20);

        // Modalidades individuales
        PdfPCell individualCell = new PdfPCell();
        individualCell.setPadding(15);
        individualCell.setBackgroundColor(INSTITUTIONAL_GOLD);
        individualCell.setBorder(Rectangle.NO_BORDER);

        Paragraph individualContent = new Paragraph();
        individualContent.add(new Chunk("INDIVIDUALES\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, WHITE)));
        individualContent.add(new Chunk(individualCount + " estudiantes\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, WHITE)));
        individualContent.add(new Chunk(String.format("%.1f%% del total", (double) individualCount / total * 100),
                FontFactory.getFont(FontFactory.HELVETICA, 10, WHITE)));
        individualContent.setAlignment(Element.ALIGN_CENTER);
        individualCell.addElement(individualContent);
        compTable.addCell(individualCell);

        // Modalidades grupales
        PdfPCell groupCell = new PdfPCell();
        groupCell.setPadding(15);
        groupCell.setBackgroundColor(INSTITUTIONAL_RED);
        groupCell.setBorder(Rectangle.NO_BORDER);

        Paragraph groupContent = new Paragraph();
        groupContent.add(new Chunk("GRUPALES\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, WHITE)));
        groupContent.add(new Chunk(groupCount + " estudiantes\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, WHITE)));
        groupContent.add(new Chunk(String.format("%.1f%% del total", (double) groupCount / total * 100),
                FontFactory.getFont(FontFactory.HELVETICA, 10, WHITE)));
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
                    SMALL_FONT);
            noDataPara.setSpacingBefore(5);
            noDataPara.setSpacingAfter(15);
            document.add(noDataPara);
            return;
        }

        addSubsectionTitle(document, "📈 Gráfico de Estado de Avance");

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(15);

        // A tiempo
        addTimelineBar(chartTable, "A Tiempo", onTimeCount, total, INSTITUTIONAL_GOLD);

        // En riesgo
        addTimelineBar(chartTable, "En Riesgo", atRiskCount, total, new BaseColor(255, 152, 0));

        // Retrasados
        addTimelineBar(chartTable, "Retrasados", delayedCount, total, INSTITUTIONAL_RED);

        document.add(chartTable);
    }

    /**
     * Agregar barra de estado temporal
     */
    private void addTimelineBar(PdfPTable table, String label, int count, int total, BaseColor color) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(4);
        containerCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{1.5f, 4f, 1f});
        } catch (DocumentException e) {
            // Ignorar
        }

        // Etiqueta
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPadding(3);
        innerTable.addCell(labelCell);

        // Barra de progreso
        float percentage = total > 0 ? (float) count / total : 0;
        PdfPCell barCell = createEnhancedTimelineBar(count, percentage, color);
        innerTable.addCell(barCell);

        // Valor y porcentaje
        PdfPCell valueCell = new PdfPCell();
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setPadding(3);

        Paragraph valueContent = new Paragraph();
        valueContent.add(new Chunk(count + " ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, color)));
        valueContent.add(new Chunk("(" + String.format("%.1f%%", percentage * 100) + ")",
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_GRAY)));
        valueCell.addElement(valueContent);
        innerTable.addCell(valueCell);

        containerCell.addElement(innerTable);
        table.addCell(containerCell);
    }

    /**
     * Crear barra mejorada de estado temporal
     */
    private PdfPCell createEnhancedTimelineBar(int value, float percentage, BaseColor color) {
        PdfPTable barContainer = new PdfPTable(2);
        float barWidth = Math.max(percentage * 100, 3); // Mínimo 3% para visibilidad
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
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, WHITE)));
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(4);
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
     * Agregar indicadores de rendimiento visual
     */
    private void addPerformanceIndicators(Document document,
                                         StudentListingReportDTO.GeneralStatisticsDTO stats)
            throws DocumentException {

        addSubsectionTitle(document, "🎯 Indicadores de Rendimiento");

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
        cell.setBorderColor(INSTITUTIONAL_GOLD);
        cell.setBackgroundColor(WHITE);

        // Etiqueta
        Paragraph labelPara = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_BLACK));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);

        // Barra de progreso
        float percentage = (float) (value / maxValue);
        BaseColor barColor = percentage >= 0.7 ? INSTITUTIONAL_GOLD : INSTITUTIONAL_RED;

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
        empty.setBackgroundColor(LIGHT_GOLD);
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
    private void addDistributionSummaryCards(Document document,
                                            StudentListingReportDTO.DistributionAnalysisDTO distribution)
            throws DocumentException {

        PdfPTable cardsTable = new PdfPTable(3);
        cardsTable.setWidthPercentage(100);
        cardsTable.setSpacingBefore(10);
        cardsTable.setSpacingAfter(20);

        // Tipos de modalidad diferentes
        int modalityTypes = distribution.getByModalityType() != null ?
                distribution.getByModalityType().size() : 0;
        addSummaryCardWithIcon(cardsTable, "Tipos Modalidad",
                String.valueOf(modalityTypes), INSTITUTIONAL_GOLD);

        // Estados diferentes
        int states = distribution.getByStatus() != null ?
                distribution.getByStatus().size() : 0;
        addSummaryCardWithIcon(cardsTable, "Estados Diferentes",
                String.valueOf(states), INSTITUTIONAL_RED);

        // Estados temporales
        int timelineStates = distribution.getByTimelineStatus() != null ?
                distribution.getByTimelineStatus().size() : 0;
        addSummaryCardWithIcon(cardsTable, "Estados Temporales",
                String.valueOf(timelineStates), INSTITUTIONAL_GOLD);

        document.add(cardsTable);
    }

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
                try {
                    addEnhancedDistributionBar(chartTable, entry.getKey(), entry.getValue(),
                            maxValue, percentages, color);
                } catch (DocumentException e) {
                    // Ignorar errores individuales
                }
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
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, WHITE));
        totalText.setAlignment(Element.ALIGN_CENTER);
        totalCell.addElement(totalText);
        totalTable.addCell(totalCell);

        document.add(totalTable);
    }

    /**
     * Agregar barra de distribución mejorada
     */
    private void addEnhancedDistributionBar(PdfPTable table, String label, int value,
                                           int maxValue, Map<String, Double> percentages,
                                           BaseColor color) throws DocumentException {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(3);
        containerCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        innerTable.setWidths(new float[]{2.5f, 4f, 1.5f});

        // Etiqueta
        PdfPCell labelCell = new PdfPCell(new Phrase(truncate(label, 40), SMALL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPadding(3);
        innerTable.addCell(labelCell);

        // Barra
        float percentage = maxValue > 0 ? (float) value / maxValue : 0;
        PdfPCell barCell = createEnhancedDistributionBarCell(value, percentage, color);
        innerTable.addCell(barCell);

        // Valor y porcentaje
        Double pct = percentages != null ? percentages.get(label) : null;
        String valueText = value + " (" + (pct != null ? String.format("%.1f%%", pct) : "N/D") + ")";

        PdfPCell valueCell = new PdfPCell(new Phrase(valueText,
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
     * Crear celda de barra de distribución
     */
    private PdfPCell createEnhancedDistributionBarCell(int value, float percentage, BaseColor color) {
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

        // Parte llena
        PdfPCell filledCell = new PdfPCell(new Phrase(String.valueOf(value),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, WHITE)));
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(3);
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

    // ==================== FIN DE NUEVOS MÉTODOS ====================

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Gráfico de distribución
     */
    private void addDistributionChart(Document document, Map<String, Integer> data,
                                      Map<String, Double> percentages, BaseColor color)
            throws DocumentException {

        if (data == null || data.isEmpty()) return;

        int maxValue = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(15);

        // Ordenar por valor descendente
        data.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10) // Top 10
            .forEach(entry -> {
                try {
                    PdfPCell cell = new PdfPCell();
                    cell.setPadding(3);
                    cell.setBorder(Rectangle.NO_BORDER);

                    PdfPTable barTable = new PdfPTable(2);
                    barTable.setWidthPercentage(100);
                    barTable.setWidths(new float[]{2f, 5f});

                    // Etiqueta
                    PdfPCell labelCell = new PdfPCell(new Phrase(
                        truncate(entry.getKey(), 35),
                        SMALL_FONT
                    ));
                    labelCell.setBorder(Rectangle.NO_BORDER);
                    labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    labelCell.setPadding(3);
                    barTable.addCell(labelCell);

                    // Barra
                    float percentage = maxValue > 0 ? (float) entry.getValue() / maxValue : 0;
                    Double pct = percentages != null ? percentages.get(entry.getKey()) : null;
                    String label = entry.getValue() + (pct != null ?
                        " (" + String.format("%.1f%%", pct) + ")" : "");

                    PdfPCell barCell = createBarCell(label, percentage, color);
                    barTable.addCell(barCell);

                    cell.addElement(barTable);
                    chartTable.addCell(cell);
                } catch (DocumentException e) {
                    // Ignorar errores individuales
                }
            });

        document.add(chartTable);
    }

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
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, WHITE)));
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(3);
        filledCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        barContainer.addCell(filledCell);

        // Parte vacía
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(LIGHT_GOLD);
        emptyCell.setBorder(Rectangle.NO_BORDER);
        barContainer.addCell(emptyCell);

        PdfPCell containerCell = new PdfPCell();
        containerCell.addElement(barContainer);
        containerCell.setBorder(Rectangle.NO_BORDER);
        containerCell.setPadding(0);

        return containerCell;
    }

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
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.WHITE)));
        content.add(new Chunk(label,
            FontFactory.getFont(FontFactory.HELVETICA, 8, new BaseColor(240, 240, 240))));
        content.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(content);
        table.addCell(cell);
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
            FontFactory.getFont(FontFactory.HELVETICA, 7, new BaseColor(240, 240, 240)));
        unitPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(unitPara);

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
        valueCell.setBackgroundColor(BaseColor.WHITE);
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }

    /**
     * Fila de detalle
     */
    private void addDetailRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setPadding(8);
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(LIGHT_GOLD);
        labelCell.setBorderWidth(0.5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setPadding(8);
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(LIGHT_GOLD);
        valueCell.setBorderWidth(0.5f);
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
        cell.setBackgroundColor(alternate ? LIGHT_GOLD : BaseColor.WHITE);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(LIGHT_GOLD);
        cell.setBorderWidth(0.3f);
        table.addCell(cell);
    }


    /**
     * Título de sección con línea dorada institucional.
     */
    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph section = new Paragraph(title, HEADER_FONT);
        section.setSpacingBefore(10);
        section.setSpacingAfter(8);
        document.add(section);
        InstitutionalPdfHeader.addGoldLine(document);
        addSpacingParagraph(document, 6f);
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

    /**
     * Trunca texto
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

    // ==================== CLASE INTERNA: PAGE EVENT HELPER ====================

    /**
     * Helper para eventos de página con pie institucional.
     */
    private static class StudentListingPageEventHelper extends PdfPageEventHelper {

        private final StudentListingReportDTO report;
        private static final BaseColor GOLD  = new BaseColor(213, 203, 160);
        private static final BaseColor RED   = new BaseColor(143, 30, 30);
        private static final BaseColor GRAY  = new BaseColor(80, 80, 80);

        public StudentListingPageEventHelper(StudentListingReportDTO report) {
            this.report = report;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            float left   = document.leftMargin();
            float right  = document.right();
            float bottom = document.bottom() - 15f;

            // Línea dorada encima del pie
            cb.setLineWidth(1f);
            cb.setColorStroke(GOLD);
            cb.moveTo(left, bottom + 10f);
            cb.lineTo(right, bottom + 10f);
            cb.stroke();

            // SIGMA — izquierda
            Phrase systemPhrase = new Phrase("SIGMA — Universidad Surcolombiana",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, RED));
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, systemPhrase, left, bottom, 0);

            // Programa — centro
            Phrase centerPhrase = new Phrase(report.getAcademicProgramName(),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, GRAY));
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, centerPhrase,
                    (left + right) / 2f, bottom, 0);

            // Número de página — derecha
            Phrase pagePhrase = new Phrase("Pág. " + writer.getPageNumber(),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, GRAY));
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, pagePhrase, right, bottom, 0);
        }
    }
}

