package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.report.dto.ExecutiveSummaryDTO;
import com.SIGMA.USCO.report.dto.GlobalModalityReportDTO;
import com.SIGMA.USCO.report.dto.ModalityDetailReportDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class PdfReport {

    // ── Paleta institucional ──────────────────────────────────────────────────
    private static final BaseColor INSTITUTIONAL_RED  = new BaseColor(143, 30, 30);   // #8F1E1E
    private static final BaseColor INSTITUTIONAL_GOLD = new BaseColor(213, 203, 160); // #D5CBA0
    private static final BaseColor LIGHT_GOLD         = new BaseColor(245, 242, 235);
    private static final BaseColor TEXT_GRAY          = new BaseColor(80, 80, 80);

    // ── Fuentes ───────────────────────────────────────────────────────────────
    private static final Font TITLE_FONT       = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   INSTITUTIONAL_RED);
    private static final Font SECTION_FONT     = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   INSTITUTIONAL_RED);
    private static final Font NORMAL_FONT      = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
    private static final Font BOLD_FONT        = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.BLACK);
    private static final Font SMALL_FONT       = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font HEADER_TABLE_FONT= new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.WHITE);
    private static final Font INFO_LABEL_FONT  = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   TEXT_GRAY);
    private static final Font INFO_VALUE_FONT  = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, BaseColor.BLACK);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    // =========================================================================
    //  PUNTO DE ENTRADA
    // =========================================================================

    public ByteArrayOutputStream generatePDF(GlobalModalityReportDTO report) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, outputStream);
        document.open();

        // 1. Portada con header institucional
        addCoverPage(document, report);

        // 2. Resumen Ejecutivo
        document.newPage();
        addInternalHeader(document);
        addExecutiveSummary(document, report.getExecutiveSummary());

        // 3. Indicadores de gestión
        document.newPage();
        addInternalHeader(document);
        addManagementIndicators(document, report.getExecutiveSummary(), report.getModalities());

        // 4. Distribuciones visuales
        addInternalHeader(document);
        addVisualDistributions(document, report.getExecutiveSummary());

        // 5. Análisis de directores
        document.newPage();
        addInternalHeader(document);
        addDirectorAnalysis(document, report.getModalities());

        // 6. Detalle de modalidades
        addModalityDetails(document, report.getModalities());

        // 7. Observaciones y pie
        addFooterSection(document, report);

        document.close();
        return outputStream;
    }


    // =========================================================================
    //  PORTADA INSTITUCIONAL
    // =========================================================================

    private void addCoverPage(Document document, GlobalModalityReportDTO report)
            throws DocumentException, IOException {

        // --- Header con logo ---
        InstitutionalPdfHeader.addHeader(
                document,
                "Facultad de Ingeniería",
                report.getAcademicProgramName() != null
                        ? report.getAcademicProgramName() +
                          (report.getAcademicProgramCode() != null ? " — Cód. " + report.getAcademicProgramCode() : "")
                        : "Sistema de Gestión de Modalidades de Grado",
                "Reporte General de Modalidades Activas"
        );

        // --- Caja del título central ---
        addSpacingParagraph(document, 10f);

        PdfPTable titleBox = new PdfPTable(1);
        titleBox.setWidthPercentage(90);
        titleBox.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleBox.setSpacingAfter(18f);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(INSTITUTIONAL_RED);
        titleCell.setPadding(16f);
        titleCell.setBorder(Rectangle.NO_BORDER);

        Paragraph titlePara = new Paragraph("REPORTE GENERAL DE\nMODALIDADES ACTIVAS", TITLE_FONT);
        titlePara.setFont(new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.WHITE));
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(titlePara);

        if (report.getAcademicProgramName() != null) {
            Paragraph progPara = new Paragraph(
                    report.getAcademicProgramName().toUpperCase(),
                    new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, INSTITUTIONAL_GOLD));
            progPara.setAlignment(Element.ALIGN_CENTER);
            progPara.setSpacingBefore(6f);
            titleCell.addElement(progPara);
        }

        titleBox.addCell(titleCell);
        document.add(titleBox);

        // --- Tabla de información de portada ---
        PdfPTable infoBox = new PdfPTable(2);
        infoBox.setWidthPercentage(80);
        infoBox.setHorizontalAlignment(Element.ALIGN_CENTER);
        infoBox.setSpacingAfter(20f);
        try { infoBox.setWidths(new float[]{40f, 60f}); } catch (DocumentException ignored) {}

        addCoverInfoRow(infoBox, "Programa:",
                report.getAcademicProgramName() != null ? report.getAcademicProgramName() : "Todos los programas");
        if (report.getAcademicProgramCode() != null) {
            addCoverInfoRow(infoBox, "Código:", report.getAcademicProgramCode());
        }
        addCoverInfoRow(infoBox, "Fecha de generación:",
                report.getGeneratedAt().format(DATE_FORMATTER));
        addCoverInfoRow(infoBox, "Generado por:", report.getGeneratedBy());
        if (report.getMetadata() != null) {
            addCoverInfoRow(infoBox, "Total de registros:",
                    String.valueOf(report.getMetadata().getTotalRecords()));
        }
        document.add(infoBox);

        // --- Línea de cierre de portada ---
        InstitutionalPdfHeader.addRedLine(document);
        InstitutionalPdfHeader.addGoldLine(document);

        // --- Pie de portada ---
        addSpacingParagraph(document, 16f);
        Paragraph footer = new Paragraph(
                "Sistema Integral de Gestión de Modalidades de Grado — SIGMA\n" +
                "Universidad Surcolombiana",
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, TEXT_GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.newPage();
    }

    /** Fila de portada (label dorado / valor blanco sobre fondo claro). */
    private void addCoverInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, INFO_LABEL_FONT));
        labelCell.setBackgroundColor(LIGHT_GOLD);
        labelCell.setPadding(7f);
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderColor(INSTITUTIONAL_GOLD);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "—", INFO_VALUE_FONT));
        valueCell.setBackgroundColor(BaseColor.WHITE);
        valueCell.setPadding(7f);
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(INSTITUTIONAL_GOLD);
        table.addCell(valueCell);
    }


    // =========================================================================
    //  ENCABEZADO INTERNO (páginas interiores)
    // =========================================================================

    /**
     * Encabezado compacto para páginas internas: línea roja + línea dorada + nombre del sistema.
     */
    private void addInternalHeader(Document document) throws DocumentException {
        PdfPTable strip = new PdfPTable(2);
        strip.setWidthPercentage(100);
        strip.setSpacingAfter(8f);
        try { strip.setWidths(new float[]{70f, 30f}); } catch (DocumentException ignored) {}

        PdfPCell leftCell = new PdfPCell(new Phrase(
                "UNIVERSIDAD SURCOLOMBIANA — SIGMA",
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, INSTITUTIONAL_RED)));
        leftCell.setBorder(Rectangle.BOTTOM);
        leftCell.setBorderColorBottom(INSTITUTIONAL_RED);
        leftCell.setBorderWidthBottom(1.5f);
        leftCell.setPadding(4f);
        strip.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell(new Phrase(
                "Reporte de Modalidades Activas",
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, TEXT_GRAY)));
        rightCell.setBorder(Rectangle.BOTTOM);
        rightCell.setBorderColorBottom(INSTITUTIONAL_GOLD);
        rightCell.setBorderWidthBottom(1.5f);
        rightCell.setPadding(4f);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        strip.addCell(rightCell);

        document.add(strip);
    }


    // =========================================================================
    //  SECCIONES DE CONTENIDO
    // =========================================================================

    private void addExecutiveSummary(Document document, ExecutiveSummaryDTO summary) throws DocumentException {
        addSectionTitle(document, "1. RESUMEN EJECUTIVO");

        PdfPTable metricsTable = new PdfPTable(2);
        metricsTable.setWidthPercentage(100);
        metricsTable.setSpacingAfter(15f);

        addMetricRow(metricsTable, "Total de Modalidades Activas",
                summary.getTotalActiveModalities().toString(), INSTITUTIONAL_RED);
        addMetricRow(metricsTable, "Total de Estudiantes Activos en Modalidades",
                summary.getTotalActiveStudents().toString(), INSTITUTIONAL_GOLD);
        addMetricRow(metricsTable, "Total de Directores Asignados",
                summary.getTotalActiveDirectors().toString(), INSTITUTIONAL_RED);
        addMetricRow(metricsTable, "Modalidades Individuales",
                summary.getIndividualModalities().toString(), INSTITUTIONAL_GOLD);
        addMetricRow(metricsTable, "Modalidades Grupales",
                summary.getGroupModalities().toString(), INSTITUTIONAL_RED);
        addMetricRow(metricsTable, "En Proceso de Revisión",
                summary.getModalitiesInReview().toString(), INSTITUTIONAL_GOLD);

        document.add(metricsTable);

        addSubsectionTitle(document, "1.1 Indicadores de Eficiencia");
        PdfPTable efficiencyTable = new PdfPTable(2);
        efficiencyTable.setWidthPercentage(100);
        efficiencyTable.setSpacingAfter(15f);

        if (summary.getAverageStudentsPerGroup() != null) {
            addMetricRow(efficiencyTable, "Promedio de Estudiantes por Modalidad Grupal",
                    String.format("%.2f", summary.getAverageStudentsPerGroup()), INSTITUTIONAL_GOLD);
        }
        if (summary.getModalitiesWithoutDirector() != null) {
            BaseColor alertColor = summary.getModalitiesWithoutDirector() > 0
                    ? new BaseColor(180, 50, 50) : INSTITUTIONAL_GOLD;
            addMetricRow(efficiencyTable, "⚠ Modalidades sin Director Asignado",
                    summary.getModalitiesWithoutDirector().toString(), alertColor);
        }
        if (summary.getOverallProgressRate() != null) {
            addMetricRow(efficiencyTable, "Tasa de Progreso General",
                    String.format("%.1f%%", summary.getOverallProgressRate()), INSTITUTIONAL_RED);
        }
        document.add(efficiencyTable);

        addSubsectionTitle(document, "1.2 Distribución por Tipo de Modalidad");
        document.add(createEnhancedDistributionTable(
                summary.getModalitiesByType(), summary.getTotalActiveModalities()));

        addSubsectionTitle(document, "1.3 Distribución por Estado");
        document.add(createEnhancedDistributionTable(
                summary.getModalitiesByStatus(), summary.getTotalActiveModalities()));
    }

    private void addManagementIndicators(Document document, ExecutiveSummaryDTO summary,
            java.util.List<ModalityDetailReportDTO> modalities) throws DocumentException {

        addSectionTitle(document, "2. INDICADORES DE GESTIÓN");

        addSubsectionTitle(document, "2.1 Alertas y Observaciones");
        PdfPTable alertsTable = new PdfPTable(2);
        alertsTable.setWidthPercentage(100);
        alertsTable.setSpacingAfter(15f);

        java.util.List<ModalityDetailReportDTO> topLongest = modalities.stream()
                .sorted(java.util.Comparator.comparing(ModalityDetailReportDTO::getDaysSinceStart,
                        java.util.Comparator.reverseOrder()))
                .limit(5)
                .toList();

        if (!topLongest.isEmpty()) {
            addMetricRow(alertsTable, "Modalidad más antigua",
                    topLongest.getFirst().getDaysSinceStart() + " días",
                    new BaseColor(180, 50, 50));
            addMetricRow(alertsTable, "Promedio días modalidades top 5",
                    String.format("%.0f días", topLongest.stream()
                            .mapToLong(ModalityDetailReportDTO::getDaysSinceStart)
                            .average().orElse(0)),
                    INSTITUTIONAL_GOLD);
        }

        long withoutDirector = modalities.stream()
                .filter(m -> m.getDirector() == null && !isDirectorNotRequired(m.getModalityName()))
                .count();
        if (withoutDirector > 0) {
            addMetricRow(alertsTable, "⚠ Modalidades requieren director",
                    String.valueOf(withoutDirector), new BaseColor(200, 100, 50));
        }
        document.add(alertsTable);

        addSubsectionTitle(document, "2.2 Eficiencia Operativa");
        PdfPTable efficiencyTable = new PdfPTable(2);
        efficiencyTable.setWidthPercentage(100);
        efficiencyTable.setSpacingAfter(15f);

        double avgDays = modalities.stream()
                .mapToLong(ModalityDetailReportDTO::getDaysSinceStart).average().orElse(0);
        addMetricRow(efficiencyTable, "Promedio de Días en Proceso",
                String.format("%.0f días", avgDays), INSTITUTIONAL_RED);

        if (summary.getTotalActiveDirectors() > 0) {
            double ratio = (double) summary.getTotalActiveStudents() / summary.getTotalActiveDirectors();
            addMetricRow(efficiencyTable, "Ratio Estudiantes/Director",
                    String.format("%.2f", ratio), INSTITUTIONAL_GOLD);
        }
        document.add(efficiencyTable);

        if (!topLongest.isEmpty()) {
            addSubsectionTitle(document, "2.3 Modalidades con Mayor Tiempo Activo");

            PdfPTable topTable = new PdfPTable(4);
            topTable.setWidthPercentage(100);
            topTable.setSpacingAfter(15f);
            try { topTable.setWidths(new int[]{30, 30, 20, 20}); } catch (DocumentException ignored) {}

            addTableHeader(topTable, "Modalidad");
            addTableHeader(topTable, "Estudiante");
            addTableHeader(topTable, "Estado");
            addTableHeader(topTable, "Días Activo");

            for (ModalityDetailReportDTO modality : topLongest) {
                topTable.addCell(createCell(modality.getModalityName(), Element.ALIGN_LEFT));
                String studentName = modality.getStudents().isEmpty() ? "N/A"
                        : modality.getStudents().getFirst().getFullName();
                topTable.addCell(createCell(studentName, Element.ALIGN_LEFT));
                topTable.addCell(createCell(modality.getStatusDescription(), Element.ALIGN_CENTER));
                topTable.addCell(createCell(modality.getDaysSinceStart() + " días", Element.ALIGN_CENTER));
            }
            document.add(topTable);
        }
    }

    private void addVisualDistributions(Document document, ExecutiveSummaryDTO summary)
            throws DocumentException {

        addSectionTitle(document, "3. ANÁLISIS VISUAL DE DISTRIBUCIÓN");

        addSubsectionTitle(document, "3.1 Comparativa Individual vs Grupal");

        PdfPTable comparisonTable = new PdfPTable(3);
        comparisonTable.setWidthPercentage(90);
        comparisonTable.setSpacingAfter(20f);
        try { comparisonTable.setWidths(new int[]{40, 30, 30}); } catch (DocumentException ignored) {}

        addTableHeader(comparisonTable, "Tipo");
        addTableHeader(comparisonTable, "Cantidad");
        addTableHeader(comparisonTable, "Porcentaje");

        int total = summary.getIndividualModalities() + summary.getGroupModalities();
        double individualPct = total > 0 ? (summary.getIndividualModalities() * 100.0 / total) : 0;
        double groupPct      = total > 0 ? (summary.getGroupModalities()       * 100.0 / total) : 0;

        comparisonTable.addCell(createHighlightedCell("Individual", LIGHT_GOLD));
        comparisonTable.addCell(createHighlightedCell(summary.getIndividualModalities().toString(), BaseColor.WHITE));
        comparisonTable.addCell(createHighlightedCell(String.format("%.1f%%", individualPct), INSTITUTIONAL_GOLD));

        comparisonTable.addCell(createHighlightedCell("Grupal", LIGHT_GOLD));
        comparisonTable.addCell(createHighlightedCell(summary.getGroupModalities().toString(), BaseColor.WHITE));
        comparisonTable.addCell(createHighlightedCell(String.format("%.1f%%", groupPct), INSTITUTIONAL_RED));

        document.add(comparisonTable);
    }

    private void addDirectorAnalysis(Document document, java.util.List<ModalityDetailReportDTO> modalities)
            throws DocumentException {

        addSectionTitle(document, "4. ANÁLISIS DE DIRECTORES");

        java.util.Map<String, Long> directorCount = modalities.stream()
                .filter(m -> m.getDirector() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        m -> m.getDirector().getFullName(),
                        java.util.stream.Collectors.counting()
                ));

        if (directorCount.isEmpty()) {
            Paragraph noDirectors = new Paragraph("No hay directores asignados actualmente.", NORMAL_FONT);
            noDirectors.setSpacingAfter(15f);
            document.add(noDirectors);
            return;
        }

        addSubsectionTitle(document, "4.1 Directores con Mayor Carga de Trabajo");

        java.util.List<java.util.Map.Entry<String, Long>> topDirectors = directorCount.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5).toList();

        PdfPTable directorTable = new PdfPTable(3);
        directorTable.setWidthPercentage(95);
        directorTable.setSpacingAfter(15f);
        try { directorTable.setWidths(new int[]{50, 20, 30}); } catch (DocumentException ignored) {}

        addTableHeader(directorTable, "Director");
        addTableHeader(directorTable, "Modalidades");
        addTableHeader(directorTable, "Carga Relativa");

        long maxLoad = topDirectors.isEmpty() ? 1 : topDirectors.getFirst().getValue();
        for (java.util.Map.Entry<String, Long> entry : topDirectors) {
            double loadPercentage = (entry.getValue() * 100.0) / maxLoad;
            directorTable.addCell(createCell(entry.getKey(), Element.ALIGN_LEFT));
            directorTable.addCell(createCell(entry.getValue().toString(), Element.ALIGN_CENTER));
            directorTable.addCell(createProgressBar(loadPercentage));
        }
        document.add(directorTable);

        addSubsectionTitle(document, "4.2 Estadísticas de Distribución");

        PdfPTable statsTable = new PdfPTable(2);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingAfter(15f);

        addMetricRow(statsTable, "Total de Directores Activos",
                String.valueOf(directorCount.size()), INSTITUTIONAL_RED);

        double avgModalitiesPerDirector = directorCount.values().stream()
                .mapToLong(Long::longValue).average().orElse(0);
        addMetricRow(statsTable, "Promedio Modalidades por Director",
                String.format("%.2f", avgModalitiesPerDirector), INSTITUTIONAL_GOLD);
        addMetricRow(statsTable, "Director con Mayor Carga",
                maxLoad + " modalidades", INSTITUTIONAL_RED);

        document.add(statsTable);
    }

    private void addModalityDetails(Document document, java.util.List<ModalityDetailReportDTO> modalities)
            throws DocumentException {

        document.newPage();
        addInternalHeader(document);
        addSectionTitle(document, "5. DETALLE DE MODALIDADES ACTIVAS");

        Paragraph totalPara = new Paragraph(
            String.format("Total: %d modalidades activas", modalities.size()), BOLD_FONT);
        totalPara.setSpacingAfter(15f);
        document.add(totalPara);

        PdfPTable detailTable = new PdfPTable(7);
        detailTable.setWidthPercentage(100);
        detailTable.setWidths(new int[]{5, 15, 20, 12, 18, 15, 15});
        detailTable.setSpacingAfter(10f);

        addTableHeader(detailTable, "ID");
        addTableHeader(detailTable, "Modalidad");
        addTableHeader(detailTable, "Estudiante(s)");
        addTableHeader(detailTable, "Programa");
        addTableHeader(detailTable, "Estado");
        addTableHeader(detailTable, "Director");
        addTableHeader(detailTable, "Días desde Inicio");

        for (ModalityDetailReportDTO modality : modalities) {
            addDetailRow(detailTable, modality);
        }
        document.add(detailTable);
    }

    private void addFooterSection(Document document, GlobalModalityReportDTO report) throws DocumentException {
        document.newPage();
        addInternalHeader(document);
        addSectionTitle(document, "6. OBSERVACIONES Y NOTAS");

        Paragraph notes = new Paragraph();
        notes.setFont(NORMAL_FONT);
        notes.setAlignment(Element.ALIGN_JUSTIFIED);

        if (report.getAcademicProgramName() != null) {
            notes.add(String.format(
                    "• Este reporte contiene únicamente las modalidades del programa académico: %s (%s).\n\n",
                    report.getAcademicProgramName(),
                    report.getAcademicProgramCode() != null ? report.getAcademicProgramCode() : "N/A"));
        }
        notes.add("• Para más información sobre modalidades específicas, consulte el sistema SIGMA.\n\n");
        notes.add("• La información presentada corresponde a la fecha de generación del reporte.\n\n");
        document.add(notes);

        // Líneas de cierre
        addSpacingParagraph(document, 20f);
        InstitutionalPdfHeader.addRedLine(document);
        InstitutionalPdfHeader.addGoldLine(document);
        addSpacingParagraph(document, 8f);

        Paragraph systemInfo = new Paragraph();
        systemInfo.setFont(new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, TEXT_GRAY));
        systemInfo.setAlignment(Element.ALIGN_CENTER);
        systemInfo.add("Generado por SIGMA — Sistema de Gestión de Modalidades de Grado\n");
        systemInfo.add("Universidad Surcolombiana · " + report.getGeneratedAt().format(DATE_FORMATTER));
        document.add(systemInfo);
    }


    // =========================================================================
    //  HELPERS GENÉRICOS
    // =========================================================================

    private void addSectionTitle(Document document, String title) throws DocumentException {
        // Línea dorada antes del título
        InstitutionalPdfHeader.addGoldLine(document);
        addSpacingParagraph(document, 4f);

        Paragraph section = new Paragraph(title, SECTION_FONT);
        section.setSpacingBefore(4f);
        section.setSpacingAfter(8f);
        document.add(section);
    }

    private void addSubsectionTitle(Document document, String title) throws DocumentException {
        Paragraph subsection = new Paragraph(title, BOLD_FONT);
        subsection.setSpacingBefore(10f);
        subsection.setSpacingAfter(5f);
        document.add(subsection);
    }

    private void addMetricRow(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setPadding(8f);
        labelCell.setBackgroundColor(BaseColor.WHITE);
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderColor(INSTITUTIONAL_GOLD);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, HEADER_TABLE_FONT));
        valueCell.setPadding(8f);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setBackgroundColor(color);
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(INSTITUTIONAL_GOLD);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String header) {
        PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_TABLE_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(INSTITUTIONAL_RED);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(INSTITUTIONAL_GOLD);
        cell.setPadding(8f);
        table.addCell(cell);
    }

    private void addDetailRow(PdfPTable table, ModalityDetailReportDTO modality) {
        table.addCell(createCell(modality.getStudentModalityId().toString(), Element.ALIGN_CENTER));
        table.addCell(createCell(modality.getModalityName(), Element.ALIGN_LEFT));

        String students = modality.getStudents().stream()
                .map(s -> s.getFullName() + (s.getIsLeader() ? " (L)" : ""))
                .collect(java.util.stream.Collectors.joining(", "));
        table.addCell(createCell(students, Element.ALIGN_LEFT));

        table.addCell(createCell(modality.getAcademicProgram(), Element.ALIGN_LEFT));
        table.addCell(createCell(modality.getStatusDescription(), Element.ALIGN_LEFT));

        String director;
        if (modality.getDirector() != null) {
            director = modality.getDirector().getFullName();
        } else {
            director = isDirectorNotRequired(modality.getModalityName()) ? "No requerido" : "Sin asignar";
        }
        table.addCell(createCell(director, Element.ALIGN_LEFT));
        table.addCell(createCell(modality.getDaysSinceStart() + " días", Element.ALIGN_CENTER));
    }

    private boolean isDirectorNotRequired(String modalityName) {
        if (modalityName == null) return false;
        String n = modalityName.toUpperCase().trim();
        return n.contains("PLAN COMPLEMENTARIO") ||
               n.contains("PRODUCCIÓN ACADEMICA") ||
               n.contains("PRODUCCION ACADEMICA") ||
               n.contains("SEMINARIO");
    }

    private PdfPCell createCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", SMALL_FONT));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5f);
        cell.setBackgroundColor(BaseColor.WHITE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(INSTITUTIONAL_GOLD);
        return cell;
    }

    private PdfPTable createEnhancedDistributionTable(Map<String, Long> distribution, Integer total) {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(95);
        table.setSpacingAfter(15f);
        try { table.setWidths(new int[]{40, 15, 45}); } catch (DocumentException ignored) {}

        addTableHeader(table, "Categoría");
        addTableHeader(table, "Cantidad");
        addTableHeader(table, "Distribución Visual");

        boolean alternate = false;
        for (Map.Entry<String, Long> entry : distribution.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList()) {

            double percentage = total > 0 ? (entry.getValue() * 100.0 / total) : 0;
            BaseColor bg = alternate ? LIGHT_GOLD : BaseColor.WHITE;

            PdfPCell categoryCell = new PdfPCell(new Phrase(entry.getKey(), NORMAL_FONT));
            categoryCell.setPadding(8f);
            categoryCell.setBackgroundColor(bg);
            categoryCell.setBorder(Rectangle.BOX);
            categoryCell.setBorderColor(INSTITUTIONAL_GOLD);
            table.addCell(categoryCell);

            String quantityText = String.format("%d (%.1f%%)", entry.getValue(), percentage);
            PdfPCell quantityCell = new PdfPCell(new Phrase(quantityText, BOLD_FONT));
            quantityCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            quantityCell.setPadding(8f);
            quantityCell.setBackgroundColor(bg);
            quantityCell.setBorder(Rectangle.BOX);
            quantityCell.setBorderColor(INSTITUTIONAL_GOLD);
            table.addCell(quantityCell);

            PdfPCell barCell = createProgressBar(percentage);
            barCell.setBackgroundColor(bg);
            table.addCell(barCell);

            alternate = !alternate;
        }
        return table;
    }

    private PdfPCell createProgressBar(double percentage) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5f);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(INSTITUTIONAL_GOLD);

        PdfPTable barTable = new PdfPTable(2);
        barTable.setWidthPercentage(100);

        try {
            int filledWidth = Math.max(1, (int) percentage);
            int emptyWidth  = Math.max(1, 100 - filledWidth);
            barTable.setWidths(new int[]{filledWidth, emptyWidth});
        } catch (DocumentException ignored) {}

        PdfPCell filledCell = new PdfPCell();
        filledCell.setBackgroundColor(percentage > 50 ? INSTITUTIONAL_RED : INSTITUTIONAL_GOLD);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setMinimumHeight(12f);
        barTable.addCell(filledCell);

        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(new BaseColor(240, 240, 240));
        emptyCell.setBorder(Rectangle.NO_BORDER);
        emptyCell.setMinimumHeight(12f);
        barTable.addCell(emptyCell);

        cell.addElement(barTable);
        return cell;
    }

    private PdfPCell createHighlightedCell(String text, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", BOLD_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8f);
        cell.setBackgroundColor(bgColor);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(INSTITUTIONAL_GOLD);
        return cell;
    }

    private void addSpacingParagraph(Document document, float height) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(height / 2f);
        spacer.setSpacingAfter(height / 2f);
        document.add(spacer);
    }
}

