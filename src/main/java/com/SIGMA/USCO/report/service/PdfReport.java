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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class PdfReport {

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
        InstitutionalPdfHeader.addInternalHeader(document, "Reporte de Modalidades Activas");
        addExecutiveSummary(document, report.getExecutiveSummary());

        // 3. Indicadores de gestión
        document.newPage();
        InstitutionalPdfHeader.addInternalHeader(document, "Reporte de Modalidades Activas");
        addManagementIndicators(document, report.getExecutiveSummary(), report.getModalities());

        // 4. Distribuciones visuales
        InstitutionalPdfHeader.addInternalHeader(document, "Reporte de Modalidades Activas");
        addVisualDistributions(document, report.getExecutiveSummary());

        // 5. Análisis de directores
        document.newPage();
        InstitutionalPdfHeader.addInternalHeader(document, "Reporte de Modalidades Activas");
        addDirectorAnalysis(document, report.getModalities());

        // 6. Detalle de modalidades
        addModalityDetails(document, report.getModalities());

        // 7. Observaciones y pie
        document.newPage();
        InstitutionalPdfHeader.addInternalHeader(document, "Reporte de Modalidades Activas");
        InstitutionalPdfHeader.addSectionTitle(document, "6. OBSERVACIONES Y NOTAS");

        StringBuilder noteText = new StringBuilder();
        if (report.getAcademicProgramName() != null) {
            noteText.append("• Este reporte contiene únicamente las modalidades del programa académico: ")
                    .append(report.getAcademicProgramName())
                    .append(" (")
                    .append(report.getAcademicProgramCode() != null ? report.getAcademicProgramCode() : "N/A")
                    .append(").\n\n");
        }
        noteText.append("• Para más información sobre modalidades específicas, consulte el sistema SIGMA.\n\n");
        noteText.append("• La información presentada corresponde a la fecha de generación del reporte.");

        String closingText = "Generado por SIGMA — Sistema de Gestión de Modalidades de Grado\nUniversidad Surcolombiana · " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_FULL);

        InstitutionalPdfHeader.addFooterSection(document, noteText.toString(), closingText);

        document.close();
        return outputStream;
    }


    // =========================================================================
    //  PORTADA INSTITUCIONAL
    // =========================================================================

    private void addCoverPage(Document document, GlobalModalityReportDTO report)
            throws DocumentException, IOException {

        List<String[]> infoRows = new ArrayList<>();
        infoRows.add(new String[]{"Programa:", report.getAcademicProgramName() != null
                ? report.getAcademicProgramName() : "Todos los programas"});
        if (report.getAcademicProgramCode() != null) {
            infoRows.add(new String[]{"Código:", report.getAcademicProgramCode()});
        }
        infoRows.add(new String[]{"Fecha de generación:",
                report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_FULL)});
        infoRows.add(new String[]{"Generado por:", report.getGeneratedBy()});
        if (report.getMetadata() != null) {
            infoRows.add(new String[]{"Total de registros:",
                    String.valueOf(report.getMetadata().getTotalRecords())});
        }

        InstitutionalPdfHeader.addCoverPage(
                document,
                report.getAcademicProgramName() != null
                        ? report.getAcademicProgramName()
                          + (report.getAcademicProgramCode() != null ? " — Cód. " + report.getAcademicProgramCode() : "")
                        : "Sistema de Gestión de Modalidades de Grado",
                "Reporte General de Modalidades Activas",
                "REPORTE GENERAL DE\nMODALIDADES ACTIVAS",
                report.getAcademicProgramName() != null
                        ? List.of(report.getAcademicProgramName().toUpperCase()) : List.of(),
                infoRows,
                null);

        document.newPage();
    }




    // =========================================================================
    //  ENCABEZADO INTERNO (páginas interiores)
    // =========================================================================




    // =========================================================================
    //  SECCIONES DE CONTENIDO
    // =========================================================================

    private void addExecutiveSummary(Document document, ExecutiveSummaryDTO summary) throws DocumentException {
        InstitutionalPdfHeader.addSectionTitle(document, "1. RESUMEN EJECUTIVO");

        PdfPTable metricsTable = new PdfPTable(2);
        metricsTable.setWidthPercentage(100);
        metricsTable.setSpacingAfter(15f);

        addMetricRow(metricsTable, "Total de Modalidades Activas",
                summary.getTotalActiveModalities().toString(), InstitutionalPdfHeader.INST_RED);
        addMetricRow(metricsTable, "Total de Estudiantes Activos en Modalidades",
                summary.getTotalActiveStudents().toString(), InstitutionalPdfHeader.INST_GOLD);
        addMetricRow(metricsTable, "Total de Directores Asignados",
                summary.getTotalActiveDirectors().toString(), InstitutionalPdfHeader.INST_RED);
        addMetricRow(metricsTable, "Modalidades Individuales",
                summary.getIndividualModalities().toString(), InstitutionalPdfHeader.INST_GOLD);
        addMetricRow(metricsTable, "Modalidades Grupales",
                summary.getGroupModalities().toString(), InstitutionalPdfHeader.INST_RED);
        addMetricRow(metricsTable, "En Proceso de Revisión",
                summary.getModalitiesInReview().toString(), InstitutionalPdfHeader.INST_GOLD);

        document.add(metricsTable);

        InstitutionalPdfHeader.addSubsectionTitle(document, "1.1 Indicadores de Eficiencia");
        PdfPTable efficiencyTable = new PdfPTable(2);
        efficiencyTable.setWidthPercentage(100);
        efficiencyTable.setSpacingAfter(15f);

        if (summary.getAverageStudentsPerGroup() != null) {
            addMetricRow(efficiencyTable, "Promedio de Estudiantes por Modalidad Grupal",
                    String.format("%.2f", summary.getAverageStudentsPerGroup()), InstitutionalPdfHeader.INST_GOLD);
        }
        if (summary.getModalitiesWithoutDirector() != null) {
            BaseColor alertColor = summary.getModalitiesWithoutDirector() > 0
                    ? new BaseColor(180, 50, 50) : InstitutionalPdfHeader.INST_GOLD;
            addMetricRow(efficiencyTable, "⚠ Modalidades sin Director Asignado",
                    summary.getModalitiesWithoutDirector().toString(), alertColor);
        }
        if (summary.getOverallProgressRate() != null) {
            addMetricRow(efficiencyTable, "Tasa de Progreso General",
                    String.format("%.1f%%", summary.getOverallProgressRate()), InstitutionalPdfHeader.INST_RED);
        }
        document.add(efficiencyTable);

        InstitutionalPdfHeader.addSubsectionTitle(document, "1.2 Distribución por Tipo de Modalidad");
        document.add(createEnhancedDistributionTable(
                summary.getModalitiesByType(), summary.getTotalActiveModalities()));

        InstitutionalPdfHeader.addSubsectionTitle(document, "1.3 Distribución por Estado");
        document.add(createEnhancedDistributionTable(
                summary.getModalitiesByStatus(), summary.getTotalActiveModalities()));
    }

    private void addManagementIndicators(Document document, ExecutiveSummaryDTO summary,
            java.util.List<ModalityDetailReportDTO> modalities) throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "2. INDICADORES DE GESTIÓN");

        InstitutionalPdfHeader.addSubsectionTitle(document, "2.1 Alertas y Observaciones");
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
                    InstitutionalPdfHeader.INST_GOLD);
        }

        long withoutDirector = modalities.stream()
                .filter(m -> m.getDirector() == null && !isDirectorNotRequired(m.getModalityName()))
                .count();
        if (withoutDirector > 0) {
            addMetricRow(alertsTable, "⚠ Modalidades requieren director",
                    String.valueOf(withoutDirector), new BaseColor(200, 100, 50));
        }
        document.add(alertsTable);

        InstitutionalPdfHeader.addSubsectionTitle(document, "2.2 Eficiencia Operativa");
        PdfPTable efficiencyTable = new PdfPTable(2);
        efficiencyTable.setWidthPercentage(100);
        efficiencyTable.setSpacingAfter(15f);

        double avgDays = modalities.stream()
                .mapToLong(ModalityDetailReportDTO::getDaysSinceStart).average().orElse(0);
        addMetricRow(efficiencyTable, "Promedio de Días en Proceso",
                String.format("%.0f días", avgDays), InstitutionalPdfHeader.INST_RED);

        if (summary.getTotalActiveDirectors() > 0) {
            double ratio = (double) summary.getTotalActiveStudents() / summary.getTotalActiveDirectors();
            addMetricRow(efficiencyTable, "Ratio Estudiantes/Director",
                    String.format("%.2f", ratio), InstitutionalPdfHeader.INST_GOLD);
        }
        document.add(efficiencyTable);

        if (!topLongest.isEmpty()) {
            InstitutionalPdfHeader.addSubsectionTitle(document, "2.3 Modalidades con Mayor Tiempo Activo");

            PdfPTable topTable = new PdfPTable(4);
            topTable.setWidthPercentage(100);
            topTable.setSpacingAfter(15f);
            try { topTable.setWidths(new int[]{30, 30, 20, 20}); } catch (DocumentException ignored) {}

            InstitutionalPdfHeader.addTableHeader(topTable, "Modalidad");
            InstitutionalPdfHeader.addTableHeader(topTable, "Estudiante");
            InstitutionalPdfHeader.addTableHeader(topTable, "Estado");
            InstitutionalPdfHeader.addTableHeader(topTable, "Días Activo");

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

        InstitutionalPdfHeader.addSectionTitle(document, "3. ANÁLISIS VISUAL DE DISTRIBUCIÓN");

        InstitutionalPdfHeader.addSubsectionTitle(document, "3.1 Comparativa Individual vs Grupal");

        PdfPTable comparisonTable = new PdfPTable(3);
        comparisonTable.setWidthPercentage(90);
        comparisonTable.setSpacingAfter(20f);
        try { comparisonTable.setWidths(new int[]{40, 30, 30}); } catch (DocumentException ignored) {}

        InstitutionalPdfHeader.addTableHeader(comparisonTable, "Tipo");
        InstitutionalPdfHeader.addTableHeader(comparisonTable, "Cantidad");
        InstitutionalPdfHeader.addTableHeader(comparisonTable, "Porcentaje");

        int total = summary.getIndividualModalities() + summary.getGroupModalities();
        double individualPct = total > 0 ? (summary.getIndividualModalities() * 100.0 / total) : 0;
        double groupPct      = total > 0 ? (summary.getGroupModalities()       * 100.0 / total) : 0;

        comparisonTable.addCell(createHighlightedCell("Individual", InstitutionalPdfHeader.LIGHT_GOLD));
        comparisonTable.addCell(createHighlightedCell(summary.getIndividualModalities().toString(), BaseColor.WHITE));
        comparisonTable.addCell(createHighlightedCell(String.format("%.1f%%", individualPct), InstitutionalPdfHeader.INST_GOLD));

        comparisonTable.addCell(createHighlightedCell("Grupal", InstitutionalPdfHeader.LIGHT_GOLD));
        comparisonTable.addCell(createHighlightedCell(summary.getGroupModalities().toString(), BaseColor.WHITE));
        comparisonTable.addCell(createHighlightedCell(String.format("%.1f%%", groupPct), InstitutionalPdfHeader.INST_RED));

        document.add(comparisonTable);
    }

    private void addDirectorAnalysis(Document document, java.util.List<ModalityDetailReportDTO> modalities)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "4. ANÁLISIS DE DIRECTORES");

        java.util.Map<String, Long> directorCount = modalities.stream()
                .filter(m -> m.getDirector() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        m -> m.getDirector().getFullName(),
                        java.util.stream.Collectors.counting()
                ));

        if (directorCount.isEmpty()) {
            Paragraph noDirectors = new Paragraph("No hay directores asignados actualmente.", InstitutionalPdfHeader.NORMAL_FONT);
            noDirectors.setSpacingAfter(15f);
            document.add(noDirectors);
            return;
        }

        InstitutionalPdfHeader.addSubsectionTitle(document, "4.1 Directores con Mayor Carga de Trabajo");

        java.util.List<java.util.Map.Entry<String, Long>> topDirectors = directorCount.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5).toList();

        PdfPTable directorTable = new PdfPTable(3);
        directorTable.setWidthPercentage(95);
        directorTable.setSpacingAfter(15f);
        try { directorTable.setWidths(new int[]{50, 20, 30}); } catch (DocumentException ignored) {}

        InstitutionalPdfHeader.addTableHeader(directorTable, "Director");
        InstitutionalPdfHeader.addTableHeader(directorTable, "Modalidades");
        InstitutionalPdfHeader.addTableHeader(directorTable, "Carga Relativa");

        long maxLoad = topDirectors.isEmpty() ? 1 : topDirectors.getFirst().getValue();
        for (java.util.Map.Entry<String, Long> entry : topDirectors) {
            double loadPercentage = (entry.getValue() * 100.0) / maxLoad;
            directorTable.addCell(createCell(entry.getKey(), Element.ALIGN_LEFT));
            directorTable.addCell(createCell(entry.getValue().toString(), Element.ALIGN_CENTER));
            directorTable.addCell(InstitutionalPdfHeader.createProgressBar(loadPercentage));
        }
        document.add(directorTable);

        InstitutionalPdfHeader.addSubsectionTitle(document, "4.2 Estadísticas de Distribución");

        PdfPTable statsTable = new PdfPTable(2);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingAfter(15f);

        addMetricRow(statsTable, "Total de Directores Activos",
                String.valueOf(directorCount.size()), InstitutionalPdfHeader.INST_RED);

        double avgModalitiesPerDirector = directorCount.values().stream()
                .mapToLong(Long::longValue).average().orElse(0);
        addMetricRow(statsTable, "Promedio Modalidades por Director",
                String.format("%.2f", avgModalitiesPerDirector), InstitutionalPdfHeader.INST_GOLD);
        addMetricRow(statsTable, "Director con Mayor Carga",
                maxLoad + " modalidades", InstitutionalPdfHeader.INST_RED);

        document.add(statsTable);
    }

    private void addModalityDetails(Document document, java.util.List<ModalityDetailReportDTO> modalities)
            throws DocumentException {

        document.newPage();
        InstitutionalPdfHeader.addInternalHeader(document, "Reporte de Modalidades Activas");
        InstitutionalPdfHeader.addSectionTitle(document, "5. DETALLE DE MODALIDADES ACTIVAS");

        Paragraph totalPara = new Paragraph(
            String.format("Total: %d modalidades activas", modalities.size()), InstitutionalPdfHeader.BOLD_FONT);
        totalPara.setSpacingAfter(15f);
        document.add(totalPara);

        PdfPTable detailTable = new PdfPTable(7);
        detailTable.setWidthPercentage(100);
        detailTable.setWidths(new int[]{5, 15, 20, 12, 18, 15, 15});
        detailTable.setSpacingAfter(10f);

        InstitutionalPdfHeader.addTableHeader(detailTable, "ID");
        InstitutionalPdfHeader.addTableHeader(detailTable, "Modalidad");
        InstitutionalPdfHeader.addTableHeader(detailTable, "Estudiante(s)");
        InstitutionalPdfHeader.addTableHeader(detailTable, "Programa");
        InstitutionalPdfHeader.addTableHeader(detailTable, "Estado");
        InstitutionalPdfHeader.addTableHeader(detailTable, "Director");
        InstitutionalPdfHeader.addTableHeader(detailTable, "Días desde Inicio");

        for (ModalityDetailReportDTO modality : modalities) {
            addDetailRow(detailTable, modality);
        }
        document.add(detailTable);
    }




    // =========================================================================
    //  HELPERS GENÉRICOS
    // =========================================================================



    private void addMetricRow(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, InstitutionalPdfHeader.BOLD_FONT));
        labelCell.setPadding(8f);
        labelCell.setBackgroundColor(BaseColor.WHITE);
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, InstitutionalPdfHeader.TABLE_HEADER_FONT));
        valueCell.setPadding(8f);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setBackgroundColor(color);
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        table.addCell(valueCell);
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
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", InstitutionalPdfHeader.SMALL_FONT));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5f);
        cell.setBackgroundColor(BaseColor.WHITE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        return cell;
    }

    private PdfPTable createEnhancedDistributionTable(Map<String, Long> distribution, Integer total) {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(95);
        table.setSpacingAfter(15f);
        try { table.setWidths(new int[]{40, 15, 45}); } catch (DocumentException ignored) {}

        InstitutionalPdfHeader.addTableHeader(table, "Categoría");
        InstitutionalPdfHeader.addTableHeader(table, "Cantidad");
        InstitutionalPdfHeader.addTableHeader(table, "Distribución Visual");

        boolean alternate = false;
        for (Map.Entry<String, Long> entry : distribution.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList()) {

            double percentage = total > 0 ? (entry.getValue() * 100.0 / total) : 0;
            BaseColor bg = alternate ? InstitutionalPdfHeader.LIGHT_GOLD : BaseColor.WHITE;

            PdfPCell categoryCell = new PdfPCell(new Phrase(entry.getKey(), InstitutionalPdfHeader.NORMAL_FONT));
            categoryCell.setPadding(8f);
            categoryCell.setBackgroundColor(bg);
            categoryCell.setBorder(Rectangle.BOX);
            categoryCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
            table.addCell(categoryCell);

            String quantityText = String.format("%d (%.1f%%)", entry.getValue(), percentage);
            PdfPCell quantityCell = new PdfPCell(new Phrase(quantityText, InstitutionalPdfHeader.BOLD_FONT));
            quantityCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            quantityCell.setPadding(8f);
            quantityCell.setBackgroundColor(bg);
            quantityCell.setBorder(Rectangle.BOX);
            quantityCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
            table.addCell(quantityCell);

            PdfPCell barCell = InstitutionalPdfHeader.createProgressBar(percentage);
            barCell.setBackgroundColor(bg);
            table.addCell(barCell);

            alternate = !alternate;
        }
        return table;
    }


    private PdfPCell createHighlightedCell(String text, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", InstitutionalPdfHeader.BOLD_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8f);
        cell.setBackgroundColor(bgColor);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        return cell;
    }


}

