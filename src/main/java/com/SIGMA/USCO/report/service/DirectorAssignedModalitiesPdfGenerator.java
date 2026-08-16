package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.report.dto.DirectorAssignedModalitiesReportDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Servicio para generar PDF del reporte de modalidades por director asignado
 * RF-49 - Generación de Reportes por Director Asignado
 */
@Service
public class DirectorAssignedModalitiesPdfGenerator extends BaseReportPdfGenerator {


    public ByteArrayOutputStream generatePDF(DirectorAssignedModalitiesReportDTO report)
            throws DocumentException, IOException {

        PdfSession session = openDocument(PageSize.A4, 50, 50, 50, 50, report.getAcademicProgramName(), null);

        // 1. Portada
        addCoverPage(session.document(), report);

        // 2. Resumen Ejecutivo
        newPageWithHeader(session, "Director Asignado \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addExecutiveSummary(session.document(), report);

        // 3. Análisis de Carga de Trabajo (si está disponible)
        if (report.getWorkloadAnalysis() != null) {
            newPageWithHeader(session, "Director Asignado \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
            addWorkloadAnalysis(session.document(), report);
        }

        // 4. Directores y sus Modalidades
        newPageWithHeader(session, "Director Asignado \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addDirectorsAndModalities(session.document(), report);

        // 5. Estadísticas por Estado y Tipo
        newPageWithHeader(session, "Director Asignado \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addStatisticsByStatusAndType(session.document(), report);

        // 6. Recomendaciones
        newPageWithHeader(session, "Director Asignado \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addRecommendations(session.document(), report);

        // 7. Pie institucional de cierre
        InstitutionalPdfHeader.addFooterSection(session.document(),
                "Este reporte fue generado autom\u00e1ticamente por el sistema SIGMA a partir de los datos acad\u00e9micos " +
                "registrados para el programa: " + report.getAcademicProgramName() + ". " +
                "Para consultas o asignaciones de directores, contacte con la coordinaci\u00f3n del programa acad\u00e9mico.",
                "Sistema Integral de Gesti\u00f3n de Modalidades de Grado \u2014 SIGMA\n" +
                "Universidad Surcolombiana | Facultad de Ingenier\u00eda | Neiva \u2013 Huila");

        close(session);
        return session.out();
    }

    /**
     * Portada institucional del reporte
     */
    private void addCoverPage(Document document, DirectorAssignedModalitiesReportDTO report)
            throws DocumentException, IOException {

        List<String> boxLines = new ArrayList<>();
        if (report.getDirectorInfo() != null) {
            boxLines.add(report.getDirectorInfo().getFullName());
        }

        List<String[]> infoRows = new ArrayList<>();
        infoRows.add(new String[]{"Programa académico:", report.getAcademicProgramName()});
        if (report.getAcademicProgramCode() != null) {
            infoRows.add(new String[]{"Código del programa:", report.getAcademicProgramCode()});
        }
        infoRows.add(new String[]{"Fecha de generación:",
                report.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy  HH:mm"))});
        infoRows.add(new String[]{"Generado por:", report.getGeneratedBy().split(" \\(")[0]});
        infoRows.add(new String[]{"Total de directores:",
                String.valueOf(report.getSummary().getTotalDirectors())});
        infoRows.add(new String[]{"Total de modalidades asignadas:",
                String.valueOf(report.getSummary().getTotalModalitiesAssigned())});

        InstitutionalPdfHeader.addCoverPage(
                document,
                report.getAcademicProgramName() + (report.getAcademicProgramCode() != null
                        ? " — Cód. " + report.getAcademicProgramCode() : ""),
                "Reporte de Modalidades por Director Asignado",
                "REPORTE DE MODALIDADES\nPOR DIRECTOR ASIGNADO",
                boxLines,
                infoRows,
                null);
    }









    /**
     * Resumen ejecutivo con diseño mejorado
     */
    private void addExecutiveSummary(Document document, DirectorAssignedModalitiesReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "1. RESUMEN EJECUTIVO");

        DirectorAssignedModalitiesReportDTO.DirectorSummaryDTO summary = report.getSummary();

        // Tabla de resumen principal con diseño mejorado
        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new int[]{25, 25, 25, 25});
        summaryTable.setSpacingAfter(25);

        // Tarjeta 1: Total de directores
        addMetricCard(summaryTable, "Directores",
                String.valueOf(summary.getTotalDirectors()), InstitutionalPdfHeader.INST_GOLD);

        // Tarjeta 2: Total de modalidades
        addMetricCard(summaryTable, "Modalidades Asignadas",
                String.valueOf(summary.getTotalModalitiesAssigned()), InstitutionalPdfHeader.INST_RED);

        // Tarjeta 3: Modalidades activas
        addMetricCard(summaryTable, "Modalidades Activas",
                String.valueOf(summary.getTotalActiveModalities()), InstitutionalPdfHeader.INST_GOLD);

        // Tarjeta 4: Estudiantes supervisados
        addMetricCard(summaryTable, "Estudiantes Supervisados",
                String.valueOf(summary.getTotalStudentsSupervised()), InstitutionalPdfHeader.INST_RED);

        document.add(summaryTable);

        // NUEVO: Gráfico visual de distribución de directores - Top 5
        addTop5DirectorsChart(document, report);

        // Director con más modalidades
        if (summary.getDirectorWithMostModalities() != null) {
            PdfPTable mostTable = new PdfPTable(1);
            mostTable.setWidthPercentage(100);
            mostTable.setSpacingAfter(12);

            PdfPCell mostCell = new PdfPCell();
            mostCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
            mostCell.setPadding(12);
            mostCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
            mostCell.setBorderWidth(2f);

            Paragraph mostText = new Paragraph();
            mostText.add(new Chunk("🏆 DIRECTOR CON MÁS MODALIDADES: ",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, InstitutionalPdfHeader.INST_RED)));
            mostText.add(new Chunk(summary.getDirectorWithMostModalities() + " ",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, InstitutionalPdfHeader.TEXT_BLACK)));
            mostText.add(new Chunk("(" + summary.getMaxModalitiesCount() + " modalidades)",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, InstitutionalPdfHeader.INST_RED)));
            mostCell.addElement(mostText);
            mostTable.addCell(mostCell);

            document.add(mostTable);
        }

        // Promedio de modalidades por director con indicador visual
        PdfPTable avgTable = new PdfPTable(1);
        avgTable.setWidthPercentage(100);
        avgTable.setSpacingAfter(20);

        PdfPCell avgCell = new PdfPCell();
        avgCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        avgCell.setPadding(12);
        avgCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        avgCell.setBorderWidth(1.5f);

        Paragraph avgText = new Paragraph();
        avgText.add(new Chunk("📊 PROMEDIO DE MODALIDADES POR DIRECTOR: ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, InstitutionalPdfHeader.INST_RED)));
        avgText.add(new Chunk(String.valueOf(summary.getAverageModalitiesPerDirector()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, InstitutionalPdfHeader.INST_RED)));
        avgCell.addElement(avgText);
        avgTable.addCell(avgCell);

        document.add(avgTable);

        // NUEVO: Ratio de eficiencia (estudiantes por director)
        addEfficiencyRatio(document, summary);

        // Directores sobrecargados y disponibles
        PdfPTable statusTable = new PdfPTable(2);
        statusTable.setWidthPercentage(100);
        statusTable.setSpacingAfter(10);

        addDirectorStatusCard(statusTable, "Directores con Carga Alta/Sobrecarga",
                String.valueOf(summary.getDirectorsOverloaded()), InstitutionalPdfHeader.INST_RED);

        addDirectorStatusCard(statusTable, "Directores Disponibles",
                String.valueOf(summary.getDirectorsAvailable()), InstitutionalPdfHeader.INST_GOLD);

        document.add(statusTable);
    }

    /**
     * Agregar tarjeta de métrica con diseño mejorado
     */
    private void addMetricCard(PdfPTable table, String label, String value, BaseColor color) {
        // Usar solo colores institucionales: rojo o dorado
        BaseColor cardColor = (color == InstitutionalPdfHeader.INST_GOLD || color == InstitutionalPdfHeader.INST_RED) ?
                color : InstitutionalPdfHeader.INST_RED;

        PdfPCell card = new PdfPCell();
        card.setPadding(15);
        card.setBorderColor(cardColor);
        card.setBorderWidth(2f);
        card.setBackgroundColor(InstitutionalPdfHeader.WHITE);

        // Valor grande
        Paragraph valuePara = new Paragraph(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, cardColor));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        valuePara.setSpacingAfter(5);
        card.addElement(valuePara);

        // Etiqueta
        Paragraph labelPara = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA, 9, InstitutionalPdfHeader.TEXT_GRAY));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        card.addElement(labelPara);

        table.addCell(card);
    }

    /**
     * Agregar tarjeta de estado de directores
     */
    private void addDirectorStatusCard(PdfPTable table, String label, String value, BaseColor color) {
        // Usar solo rojo o dorado institucional
        BaseColor cardColor = (color == InstitutionalPdfHeader.INST_RED) ? InstitutionalPdfHeader.INST_RED : InstitutionalPdfHeader.INST_GOLD;

        PdfPCell card = new PdfPCell();
        card.setPadding(12);
        card.setBorderColor(cardColor);
        card.setBorderWidth(2f);
        card.setBackgroundColor(InstitutionalPdfHeader.WHITE);

        Paragraph text = new Paragraph();
        text.add(new Chunk(label + ": ", InstitutionalPdfHeader.NORMAL_FONT));
        text.add(new Chunk(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, cardColor)));
        text.setAlignment(Element.ALIGN_CENTER);
        card.addElement(text);

        table.addCell(card);
    }

    /**
     * Análisis de carga de trabajo
     */
    private void addWorkloadAnalysis(Document document, DirectorAssignedModalitiesReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "2. ANÁLISIS DE CARGA DE TRABAJO");

        DirectorAssignedModalitiesReportDTO.WorkloadAnalysisDTO workload = report.getWorkloadAnalysis();

        // Estado general
        PdfPTable overallTable = new PdfPTable(1);
        overallTable.setWidthPercentage(100);
        overallTable.setSpacingAfter(20);

        BaseColor statusColor = "BALANCED".equals(workload.getOverallWorkloadStatus()) ?
                InstitutionalPdfHeader.INST_GOLD : InstitutionalPdfHeader.INST_RED;
        String statusIcon = "BALANCED".equals(workload.getOverallWorkloadStatus()) ? "✓" : "⚠";

        PdfPCell statusCell = new PdfPCell();
        statusCell.setBackgroundColor(statusColor);
        statusCell.setPadding(12);
        statusCell.setBorder(Rectangle.NO_BORDER);

        Paragraph statusText = new Paragraph();
        statusText.add(new Chunk(statusIcon + " ESTADO GENERAL DE CARGA: ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, InstitutionalPdfHeader.WHITE)));
        statusText.add(new Chunk(
                "BALANCED".equals(workload.getOverallWorkloadStatus()) ? "EQUILIBRADA" : "DESEQUILIBRADA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, InstitutionalPdfHeader.WHITE)));
        statusCell.addElement(statusText);
        overallTable.addCell(statusCell);

        document.add(overallTable);

        // Información clave
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(90);
        infoTable.setSpacingAfter(20);

        addWorkloadInfoRow(infoTable, "Máximo Recomendado por Director:",
                workload.getRecommendedMaxModalities() + " modalidades");
        addWorkloadInfoRow(infoTable, "Carga Promedio Actual:",
                workload.getAverageWorkload() + " modalidades");

        document.add(infoTable);

        // Distribución de carga - Título
        Paragraph distTitle = new Paragraph("📊 Distribución de Carga de Trabajo:", InstitutionalPdfHeader.SECTION_FONT);
        distTitle.setSpacingBefore(15);
        distTitle.setSpacingAfter(10);
        document.add(distTitle);

        Map<String, Integer> distribution = workload.getWorkloadDistribution();
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();

        // NUEVO: Gráfico visual mejorado de distribución
        addWorkloadDistributionChart(document, distribution, total);

        // Directores sobrecargados
        if (workload.getDirectorsOverloaded() != null && !workload.getDirectorsOverloaded().isEmpty()) {
            Paragraph overloadedTitle = new Paragraph("⚠ Directores con Sobrecarga:", InstitutionalPdfHeader.SECTION_FONT);
            overloadedTitle.setSpacingBefore(10);
            overloadedTitle.setSpacingAfter(5);
            document.add(overloadedTitle);

            for (String director : workload.getDirectorsOverloaded()) {
                Paragraph dirPara = new Paragraph("• " + director, InstitutionalPdfHeader.NORMAL_FONT);
                dirPara.setIndentationLeft(20);
                dirPara.setSpacingAfter(3);
                document.add(dirPara);
            }
            document.add(Chunk.NEWLINE);
        }

        // Directores disponibles
        if (workload.getDirectorsAvailable() != null && !workload.getDirectorsAvailable().isEmpty()) {
            Paragraph availableTitle = new Paragraph("✓ Directores Disponibles para Nuevas Asignaciones:",
                    InstitutionalPdfHeader.SECTION_FONT);
            availableTitle.setSpacingBefore(10);
            availableTitle.setSpacingAfter(5);
            document.add(availableTitle);

            for (String director : workload.getDirectorsAvailable()) {
                Paragraph dirPara = new Paragraph("• " + director, InstitutionalPdfHeader.NORMAL_FONT);
                dirPara.setIndentationLeft(20);
                dirPara.setSpacingAfter(3);
                document.add(dirPara);
            }
        }
    }

    /**
     * Agregar fila de información de carga
     */
    private void addWorkloadInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, InstitutionalPdfHeader.BOLD_FONT));
        labelCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        labelCell.setPadding(8);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, InstitutionalPdfHeader.INST_RED)));
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setPadding(8);
        table.addCell(valueCell);
    }

    /**
     * Directores y sus modalidades asignadas
     */
    private void addDirectorsAndModalities(Document document, DirectorAssignedModalitiesReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "3. DIRECTORES Y MODALIDADES ASIGNADAS");

        List<DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO> directors = report.getDirectors();

        for (int i = 0; i < directors.size(); i++) {
            DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO director = directors.get(i);

            // Si no es el primero, agregar nueva página para cada director
            if (i > 0) {
                document.newPage();
            }

            // Encabezado del director
            PdfPTable directorHeader = new PdfPTable(1);
            directorHeader.setWidthPercentage(100);
            directorHeader.setSpacingAfter(15);

            PdfPCell headerCell = new PdfPCell();
            BaseColor workloadColor = getWorkloadColor(director.getWorkloadStatus());
            headerCell.setBackgroundColor(workloadColor);
            headerCell.setPadding(12);
            headerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph directorName = new Paragraph(director.getFullName(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.WHITE));
            directorName.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(directorName);

            if (director.getAcademicTitle() != null) {
                Paragraph title = new Paragraph(director.getAcademicTitle(),
                        FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.WHITE));
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingBefore(3);
                headerCell.addElement(title);
            }

            directorHeader.addCell(headerCell);
            document.add(directorHeader);

            // Estadísticas del director
            PdfPTable statsTable = new PdfPTable(4);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingAfter(15);

            addDirectorStatCell(statsTable, "Total Asignadas",
                    String.valueOf(director.getTotalAssignedModalities()), InstitutionalPdfHeader.INST_RED);
            addDirectorStatCell(statsTable, "Activas",
                    String.valueOf(director.getActiveModalities()), InstitutionalPdfHeader.INST_GOLD);
            addDirectorStatCell(statsTable, "Completadas",
                    String.valueOf(director.getCompletedModalities()), InstitutionalPdfHeader.INST_GOLD);
            addDirectorStatCell(statsTable, "En Revisión",
                    String.valueOf(director.getPendingApprovalModalities()), InstitutionalPdfHeader.INST_RED);

            document.add(statsTable);

            // Modalidades del director
            if (director.getModalities() != null && !director.getModalities().isEmpty()) {
                Paragraph modalitiesTitle = new Paragraph("Modalidades Asignadas:", InstitutionalPdfHeader.SECTION_FONT);
                modalitiesTitle.setSpacingBefore(10);
                modalitiesTitle.setSpacingAfter(10);
                document.add(modalitiesTitle);

                // Limitar a las primeras modalidades si hay muchas
                int maxToShow = Math.min(director.getModalities().size(), 10);
                for (int j = 0; j < maxToShow; j++) {
                    DirectorAssignedModalitiesReportDTO.ModalityDetailDTO modality = director.getModalities().get(j);
                    addModalityDetail(document, modality, j + 1);
                }

                if (director.getModalities().size() > maxToShow) {
                    Paragraph moreInfo = new Paragraph(
                            "... y " + (director.getModalities().size() - maxToShow) + " modalidades más.",
                            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.GRAY));
                    moreInfo.setAlignment(Element.ALIGN_CENTER);
                    moreInfo.setSpacingBefore(10);
                    document.add(moreInfo);
                }
            }
        }
    }

    /**
     * Obtener color según carga de trabajo - Solo colores institucionales
     */
    private BaseColor getWorkloadColor(String workloadStatus) {
        switch (workloadStatus) {
            case "LOW":
            case "NORMAL":
                return InstitutionalPdfHeader.INST_GOLD;  // Carga baja/normal: dorado
            case "HIGH":
            case "OVERLOADED":
                return InstitutionalPdfHeader.INST_RED;   // Carga alta/sobrecarga: rojo
            default:
                return InstitutionalPdfHeader.INST_GOLD;
        }
    }

    /**
     * Agregar celda de estadística del director - Solo colores institucionales
     */
    private void addDirectorStatCell(PdfPTable table, String label, String value, BaseColor color) {
        // Usar solo rojo o dorado
        BaseColor statColor = (color == InstitutionalPdfHeader.INST_RED || color == InstitutionalPdfHeader.INST_GOLD) ?
                color : InstitutionalPdfHeader.INST_RED;

        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBorderColor(statColor);
        cell.setBorderWidth(2f);
        cell.setBackgroundColor(InstitutionalPdfHeader.WHITE);

        Paragraph valuePara = new Paragraph(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, statColor));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);

        Paragraph labelPara = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA, 8, InstitutionalPdfHeader.TEXT_GRAY));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        labelPara.setSpacingBefore(3);
        cell.addElement(labelPara);

        table.addCell(cell);
    }

    /**
     * Agregar detalle de una modalidad
     */
    private void addModalityDetail(Document document, DirectorAssignedModalitiesReportDTO.ModalityDetailDTO modality, int number)
            throws DocumentException {

        PdfPTable modalityTable = new PdfPTable(1);
        modalityTable.setWidthPercentage(100);
        modalityTable.setSpacingAfter(10);

        PdfPCell modalityCell = new PdfPCell();
        modalityCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        modalityCell.setPadding(10);
        modalityCell.setBorder(Rectangle.BOX);
        modalityCell.setBorderColor(InstitutionalPdfHeader.LIGHT_GOLD);

        // Título
        Paragraph title = new Paragraph(number + ". " + modality.getModalityTypeName(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, InstitutionalPdfHeader.TEXT_BLACK));
        modalityCell.addElement(title);

        // Estudiantes
        StringBuilder students = new StringBuilder("Estudiantes: ");
        for (int i = 0; i < modality.getStudents().size(); i++) {
            if (i > 0) students.append(", ");
            DirectorAssignedModalitiesReportDTO.StudentBasicInfoDTO student = modality.getStudents().get(i);
            students.append(student.getFullName());
            if (student.getIsLeader()) students.append(" (Líder)");
        }
        Paragraph studentsPara = new Paragraph(students.toString(), InstitutionalPdfHeader.SMALL_FONT);
        studentsPara.setSpacingBefore(3);
        modalityCell.addElement(studentsPara);

        // Estado
        Paragraph status = new Paragraph("Estado: " + modality.getStatusDescription(),
                FontFactory.getFont(FontFactory.HELVETICA, 9, InstitutionalPdfHeader.INST_RED));
        status.setSpacingBefore(3);
        modalityCell.addElement(status);

        // Observaciones si hay
        if (modality.getObservations() != null && !"Sin observaciones".equals(modality.getObservations())) {
            Paragraph obs = new Paragraph("⚡ " + modality.getObservations(),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, InstitutionalPdfHeader.INST_RED));
            obs.setSpacingBefore(3);
            modalityCell.addElement(obs);
        }

        modalityTable.addCell(modalityCell);
        document.add(modalityTable);
    }

    /**
     * Estadísticas por estado y tipo
     */
    private void addStatisticsByStatusAndType(Document document, DirectorAssignedModalitiesReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "4. ESTADÍSTICAS POR ESTADO Y TIPO");

        // NUEVO: Resumen visual con tarjetas
        PdfPTable summaryCards = new PdfPTable(3);
        summaryCards.setWidthPercentage(100);
        summaryCards.setSpacingAfter(20);

        Map<String, Integer> byStatus = report.getModalitiesByStatus();
        Map<String, Integer> byType = report.getModalitiesByType();

        int totalStatus = byStatus.values().stream().mapToInt(Integer::intValue).sum();
        int totalTypes = byType.values().stream().mapToInt(Integer::intValue).sum();
        int uniqueStatuses = byStatus.size();
        int uniqueTypes = byType.size();

        addStatsCard(summaryCards, "Total Estados", String.valueOf(uniqueStatuses), InstitutionalPdfHeader.INST_GOLD);
        addStatsCard(summaryCards, "Total Tipos", String.valueOf(uniqueTypes), InstitutionalPdfHeader.INST_RED);
        addStatsCard(summaryCards, "Modalidades Totales", String.valueOf(totalStatus), InstitutionalPdfHeader.INST_GOLD);

        document.add(summaryCards);

        // Por estado con gráfico mejorado
        Paragraph statusTitle = new Paragraph("📌 Distribución por Estado:", InstitutionalPdfHeader.SECTION_FONT);
        statusTitle.setSpacingAfter(10);
        document.add(statusTitle);

        // Ordenar por cantidad
        List<Map.Entry<String, Integer>> sortedByStatus = byStatus.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();

        for (Map.Entry<String, Integer> entry : sortedByStatus) {
            addEnhancedStatisticBar(document, entry.getKey(), entry.getValue(), totalStatus, InstitutionalPdfHeader.INST_GOLD);
        }

        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);

        // Por tipo con gráfico mejorado
        Paragraph typeTitle = new Paragraph("📂 Distribución por Tipo de Modalidad:", InstitutionalPdfHeader.SECTION_FONT);
        typeTitle.setSpacingAfter(10);
        document.add(typeTitle);

        List<Map.Entry<String, Integer>> sortedByType = byType.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();

        for (Map.Entry<String, Integer> entry : sortedByType) {
            addEnhancedStatisticBar(document, entry.getKey(), entry.getValue(), totalTypes, InstitutionalPdfHeader.INST_RED);
        }
    }

    /**
     * Agregar tarjeta de estadística mejorada
     */
    private void addStatsCard(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell card = new PdfPCell();
        card.setPadding(15);
        card.setBorderColor(color);
        card.setBorderWidth(2f);
        card.setBackgroundColor(InstitutionalPdfHeader.WHITE);

        Paragraph valuePara = new Paragraph(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, color));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        card.addElement(valuePara);

        Paragraph labelPara = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA, 9, InstitutionalPdfHeader.TEXT_GRAY));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        labelPara.setSpacingBefore(5);
        card.addElement(labelPara);

        table.addCell(card);
    }

    /**
     * Agregar barra de estadística mejorada con diseño profesional
     */
    private void addEnhancedStatisticBar(Document document, String label, int count, int total, BaseColor color)
            throws DocumentException {

        PdfPTable barTable = new PdfPTable(1);
        barTable.setWidthPercentage(100);
        barTable.setSpacingAfter(8);

        // Encabezado con el label
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(color);
        headerCell.setPadding(6);
        headerCell.setBorder(Rectangle.NO_BORDER);

        Paragraph headerText = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE));
        headerCell.addElement(headerText);
        barTable.addCell(headerCell);

        // Barra de progreso con información
        PdfPCell barCell = new PdfPCell();
        barCell.setPadding(0);
        barCell.setBorder(Rectangle.BOX);
        barCell.setBorderColor(color);
        barCell.setBorderWidth(0.5f);

        PdfPTable innerTable = new PdfPTable(2);
        float percentage = total > 0 ? ((float) count / total * 100) : 0;
        float barWidth = Math.max(percentage, 5); // Mínimo 5% para visibilidad
        float emptyWidth = 100 - barWidth;

        try {
            innerTable.setWidths(new float[]{barWidth, emptyWidth});
        } catch (Exception e) {
            try {
                innerTable.setWidths(new float[]{50, 50});
            } catch (Exception ex) {
                // Ignorar
            }
        }

        // Parte llena
        PdfPCell filledCell = new PdfPCell();
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(5);

        Paragraph filledText = new Paragraph(count + " modalidades",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE));
        filledCell.addElement(filledText);
        innerTable.addCell(filledCell);

        // Parte vacía con porcentaje
        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        emptyCell.setBorder(Rectangle.NO_BORDER);
        emptyCell.setPadding(5);

        Paragraph percentText = new Paragraph(String.format("%.1f%% del total", percentage),
                FontFactory.getFont(FontFactory.HELVETICA, 8, color));
        emptyCell.addElement(percentText);
        innerTable.addCell(emptyCell);

        barCell.addElement(innerTable);
        barTable.addCell(barCell);

        document.add(barTable);
    }

    /**
     * Recomendaciones
     */
    private void addRecommendations(Document document, DirectorAssignedModalitiesReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "5. RECOMENDACIONES");

        List<String> recommendations = generateRecommendations(report);

        for (int i = 0; i < recommendations.size(); i++) {
            Paragraph recommendation = new Paragraph((i + 1) + ". " + recommendations.get(i), InstitutionalPdfHeader.NORMAL_FONT);
            recommendation.setSpacingAfter(10);
            recommendation.setIndentationLeft(20);
            document.add(recommendation);
        }

        // Footer informativo
        PdfPTable footerTable = new PdfPTable(1);
        footerTable.setWidthPercentage(100);
        footerTable.setSpacingBefore(30);

        PdfPCell footerCell = new PdfPCell();
        footerCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        footerCell.setPadding(10);
        footerCell.setBorder(Rectangle.BOX);
        footerCell.setBorderColor(InstitutionalPdfHeader.INST_RED);

        Paragraph footerText = new Paragraph();
        footerText.add(new Chunk("ℹ NOTA: ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, InstitutionalPdfHeader.INST_RED)));
        footerText.add(new Chunk(
                "Este reporte fue generado automáticamente por el Sistema SIGMA. " +
                        "Los datos presentados corresponden al programa académico " +
                        report.getAcademicProgramName() + ". " +
                        "Para consultas o asignaciones de directores, contacte con la coordinación del programa.",
                FontFactory.getFont(FontFactory.HELVETICA, 8, InstitutionalPdfHeader.TEXT_GRAY)));
        footerCell.addElement(footerText);
        footerTable.addCell(footerCell);

        document.add(footerTable);
    }

    /**
     * Generar recomendaciones automáticas
     */
    private List<String> generateRecommendations(DirectorAssignedModalitiesReportDTO report) {
        List<String> recommendations = new java.util.ArrayList<>();

        DirectorAssignedModalitiesReportDTO.DirectorSummaryDTO summary = report.getSummary();

        // Recomendación sobre distribución
        recommendations.add("El programa cuenta con " + summary.getTotalDirectors() +
                " directores activos supervisando " + summary.getTotalModalitiesAssigned() +
                " modalidades de grado.");

        // Recomendación sobre promedio
        if (summary.getAverageModalitiesPerDirector() > 6) {
            recommendations.add("La carga promedio de " + summary.getAverageModalitiesPerDirector() +
                    " modalidades por director está por encima del recomendado (6). " +
                    "Se sugiere considerar la asignación de nuevos directores.");
        } else if (summary.getAverageModalitiesPerDirector() < 3) {
            recommendations.add("La carga promedio es de " + summary.getAverageModalitiesPerDirector() +
                    " modalidades por director, lo cual es apropiado y permite una supervisión de calidad.");
        }

        // Recomendación sobre sobrecarga
        if (summary.getDirectorsOverloaded() > 0) {
            recommendations.add("Se identificaron " + summary.getDirectorsOverloaded() +
                    " director(es) con carga alta o sobrecarga. Se recomienda redistribuir modalidades " +
                    "o asignar co-directores para equilibrar la carga de trabajo.");
        }

        // Recomendación sobre disponibilidad
        if (summary.getDirectorsAvailable() > 0) {
            recommendations.add("Existen " + summary.getDirectorsAvailable() +
                    " director(es) disponibles con capacidad para supervisar nuevas modalidades.");
        }

        // Recomendación sobre análisis de carga
        if (report.getWorkloadAnalysis() != null) {
            DirectorAssignedModalitiesReportDTO.WorkloadAnalysisDTO workload = report.getWorkloadAnalysis();
            if ("UNBALANCED".equals(workload.getOverallWorkloadStatus())) {
                recommendations.add("La distribución de carga de trabajo es desequilibrada. " +
                        "Se recomienda implementar un plan de redistribución de modalidades para " +
                        "optimizar la supervisión académica.");
            }
        }

        // Recomendación general
        recommendations.add("Se recomienda realizar seguimiento continuo de la carga de trabajo de " +
                "los directores y mantener una comunicación fluida para identificar necesidades de apoyo.");

        return recommendations;
    }

    // ==================== MÉTODOS HELPER ====================

    /**
     * Agregar gráfico de Top 5 directores con más modalidades
     */
    private void addTop5DirectorsChart(Document document, DirectorAssignedModalitiesReportDTO report)
            throws DocumentException {

        if (report.getDirectors() == null || report.getDirectors().isEmpty()) {
            return;
        }

        Paragraph chartTitle = new Paragraph("🏆 Top 5 Directores con Más Modalidades", InstitutionalPdfHeader.SECTION_FONT);
        chartTitle.setSpacingBefore(15);
        chartTitle.setSpacingAfter(10);
        document.add(chartTitle);

        // Ordenar directores por total de modalidades asignadas
        List<DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO> topDirectors = report.getDirectors()
                .stream()
                .sorted(Comparator.comparingInt(DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO::getTotalAssignedModalities).reversed())
                .limit(5)
                .toList();

        // Encontrar el máximo para escalar las barras
        int maxModalities = topDirectors.stream()
                .mapToInt(DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO::getTotalAssignedModalities)
                .max()
                .orElse(1);

        for (int i = 0; i < topDirectors.size(); i++) {
            DirectorAssignedModalitiesReportDTO.DirectorWithModalitiesDTO director = topDirectors.get(i);

            PdfPTable directorBar = new PdfPTable(1);
            directorBar.setWidthPercentage(100);
            directorBar.setSpacingAfter(8);

            // Encabezado con posición y nombre
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(i == 0 ? InstitutionalPdfHeader.INST_RED : InstitutionalPdfHeader.INST_GOLD);
            headerCell.setPadding(6);
            headerCell.setBorder(Rectangle.NO_BORDER);

            String position = (i + 1) + "º";
            Paragraph headerText = new Paragraph(position + " - " + director.getFullName(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE));
            headerCell.addElement(headerText);
            directorBar.addCell(headerCell);

            // Barra de progreso
            PdfPCell barCell = new PdfPCell();
            barCell.setPadding(0);
            barCell.setBorder(Rectangle.BOX);
            barCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
            barCell.setBorderWidth(0.5f);

            PdfPTable innerBar = new PdfPTable(2);
            float percentage = (float) director.getTotalAssignedModalities() / maxModalities * 100;
            float barWidth = Math.max(percentage, 5); // Mínimo 5% para visibilidad
            float emptyWidth = 100 - barWidth;

            try {
                innerBar.setWidths(new float[]{barWidth, emptyWidth});
            } catch (Exception e) {
                try {
                    innerBar.setWidths(new float[]{50, 50});
                } catch (Exception ex) {
                    // Ignorar
                }
            }

            // Parte llena de la barra
            PdfPCell filledCell = new PdfPCell();
            filledCell.setBackgroundColor(i == 0 ? InstitutionalPdfHeader.INST_RED : InstitutionalPdfHeader.INST_GOLD);
            filledCell.setBorder(Rectangle.NO_BORDER);
            filledCell.setPadding(5);

            Paragraph barText = new Paragraph(director.getTotalAssignedModalities() + " modalidades (" +
                    director.getActiveModalities() + " activas)",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE));
            filledCell.addElement(barText);
            innerBar.addCell(filledCell);

            // Parte vacía
            PdfPCell emptyCell = new PdfPCell();
            emptyCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
            emptyCell.setBorder(Rectangle.NO_BORDER);
            innerBar.addCell(emptyCell);

            barCell.addElement(innerBar);
            directorBar.addCell(barCell);

            document.add(directorBar);
        }

        document.add(Chunk.NEWLINE);
    }

    /**
     * Agregar indicador de ratio de eficiencia
     */
    private void addEfficiencyRatio(Document document, DirectorAssignedModalitiesReportDTO.DirectorSummaryDTO summary)
            throws DocumentException {

        PdfPTable ratioTable = new PdfPTable(2);
        ratioTable.setWidthPercentage(100);
        ratioTable.setSpacingBefore(10);
        ratioTable.setSpacingAfter(20);

        // Ratio estudiantes por director
        double studentsPerDirector = summary.getTotalDirectors() > 0 ?
                (double) summary.getTotalStudentsSupervised() / summary.getTotalDirectors() : 0;

        PdfPCell ratioCell1 = new PdfPCell();
        ratioCell1.setPadding(15);
        ratioCell1.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        ratioCell1.setBorderWidth(2f);
        ratioCell1.setBackgroundColor(InstitutionalPdfHeader.WHITE);

        Paragraph ratioLabel1 = new Paragraph("👥 Estudiantes por Director",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, InstitutionalPdfHeader.TEXT_GRAY));
        ratioLabel1.setAlignment(Element.ALIGN_CENTER);
        ratioCell1.addElement(ratioLabel1);

        Paragraph ratioValue1 = new Paragraph(String.format("%.1f", studentsPerDirector),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, InstitutionalPdfHeader.INST_GOLD));
        ratioValue1.setAlignment(Element.ALIGN_CENTER);
        ratioValue1.setSpacingBefore(5);
        ratioCell1.addElement(ratioValue1);

        ratioTable.addCell(ratioCell1);

        // Ratio modalidades por estudiante
        double modalitiesPerStudent = summary.getTotalStudentsSupervised() > 0 ?
                (double) summary.getTotalModalitiesAssigned() / summary.getTotalStudentsSupervised() : 0;

        PdfPCell ratioCell2 = new PdfPCell();
        ratioCell2.setPadding(15);
        ratioCell2.setBorderColor(InstitutionalPdfHeader.INST_RED);
        ratioCell2.setBorderWidth(2f);
        ratioCell2.setBackgroundColor(InstitutionalPdfHeader.WHITE);

        Paragraph ratioLabel2 = new Paragraph("📑 Modalidades por Estudiante",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, InstitutionalPdfHeader.TEXT_GRAY));
        ratioLabel2.setAlignment(Element.ALIGN_CENTER);
        ratioCell2.addElement(ratioLabel2);

        Paragraph ratioValue2 = new Paragraph(String.format("%.2f", modalitiesPerStudent),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, InstitutionalPdfHeader.INST_RED));
        ratioValue2.setAlignment(Element.ALIGN_CENTER);
        ratioValue2.setSpacingBefore(5);
        ratioCell2.addElement(ratioValue2);

        ratioTable.addCell(ratioCell2);

        document.add(ratioTable);
    }

    /**
     * Agregar gráfico visual de distribución de carga de trabajo
     */
    private void addWorkloadDistributionChart(Document document, Map<String, Integer> distribution, int total)
            throws DocumentException {

        // Datos de carga
        int lowCount = distribution.getOrDefault("LOW", 0);
        int normalCount = distribution.getOrDefault("NORMAL", 0);
        int highCount = distribution.getOrDefault("HIGH", 0);
        int overloadedCount = distribution.getOrDefault("OVERLOADED", 0);

        // Tabla de 4 columnas para mostrar cada categoría
        PdfPTable chartTable = new PdfPTable(4);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingAfter(20);

        // Baja
        addWorkloadCategoryBox(chartTable, "BAJA", lowCount, total,
                "< 2 modalidades", InstitutionalPdfHeader.INST_GOLD);

        // Normal
        addWorkloadCategoryBox(chartTable, "NORMAL", normalCount, total,
                "2-4 modalidades", InstitutionalPdfHeader.INST_GOLD);

        // Alta
        addWorkloadCategoryBox(chartTable, "ALTA", highCount, total,
                "5-7 modalidades", InstitutionalPdfHeader.INST_RED);

        // Sobrecarga
        addWorkloadCategoryBox(chartTable, "SOBRECARGA", overloadedCount, total,
                "≥ 8 modalidades", InstitutionalPdfHeader.INST_RED);

        document.add(chartTable);

        // Gráfico de barras horizontales detallado
        PdfPTable barsTable = new PdfPTable(1);
        barsTable.setWidthPercentage(100);
        barsTable.setSpacingAfter(15);

        addWorkloadBarRow(barsTable, "Carga Baja", lowCount, total, InstitutionalPdfHeader.INST_GOLD);
        addWorkloadBarRow(barsTable, "Carga Normal", normalCount, total, InstitutionalPdfHeader.INST_GOLD);
        addWorkloadBarRow(barsTable, "Carga Alta", highCount, total, InstitutionalPdfHeader.INST_RED);
        addWorkloadBarRow(barsTable, "Sobrecarga", overloadedCount, total, InstitutionalPdfHeader.INST_RED);

        document.add(barsTable);
    }

    /**
     * Agregar caja de categoría de carga de trabajo
     */
    private void addWorkloadCategoryBox(PdfPTable table, String label, int count, int total,
                                        String description, BaseColor color) {
        PdfPCell box = new PdfPCell();
        box.setPadding(12);
        box.setBorderColor(color);
        box.setBorderWidth(2f);
        box.setBackgroundColor(InstitutionalPdfHeader.WHITE);

        // Etiqueta
        Paragraph labelPara = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, color));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        box.addElement(labelPara);

        // Cantidad grande
        Paragraph countPara = new Paragraph(String.valueOf(count),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 32, color));
        countPara.setAlignment(Element.ALIGN_CENTER);
        countPara.setSpacingBefore(5);
        countPara.setSpacingAfter(5);
        box.addElement(countPara);

        // Porcentaje
        double percentage = total > 0 ? ((double) count / total * 100) : 0;
        Paragraph percentPara = new Paragraph(String.format("%.1f%%", percentage),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, color));
        percentPara.setAlignment(Element.ALIGN_CENTER);
        box.addElement(percentPara);

        // Descripción
        Paragraph descPara = new Paragraph(description,
                FontFactory.getFont(FontFactory.HELVETICA, 7, InstitutionalPdfHeader.TEXT_GRAY));
        descPara.setAlignment(Element.ALIGN_CENTER);
        descPara.setSpacingBefore(3);
        box.addElement(descPara);

        table.addCell(box);
    }

    /**
     * Agregar fila de barra horizontal de carga
     */
    private void addWorkloadBarRow(PdfPTable table, String label, int count, int total, BaseColor color) {
        PdfPCell barContainer = new PdfPCell();
        barContainer.setPadding(8);
        barContainer.setBorder(Rectangle.BOX);
        barContainer.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        barContainer.setBorderWidth(0.5f);

        // Tabla interna: label + barra + valor
        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{25, 60, 15});
        } catch (Exception e) {
            // Ignorar
        }

        // Label
        PdfPCell labelCell = new PdfPCell(new Phrase(label, InstitutionalPdfHeader.SMALL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPadding(3);
        innerTable.addCell(labelCell);

        // Barra de progreso
        PdfPCell barCell = new PdfPCell();
        barCell.setBorder(Rectangle.NO_BORDER);
        barCell.setPadding(3);

        PdfPTable barTable = new PdfPTable(2);
        float percentage = total > 0 ? ((float) count / total * 100) : 0;
        float barWidth = Math.max(percentage, 1);
        float emptyWidth = 100 - barWidth;

        try {
            barTable.setWidths(new float[]{barWidth, emptyWidth});
        } catch (Exception e) {
            try {
                barTable.setWidths(new float[]{50, 50});
            } catch (Exception ex) {
                // Ignorar
            }
        }

        PdfPCell filledCell = new PdfPCell();
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(4);
        barTable.addCell(filledCell);

        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        emptyCell.setBorder(Rectangle.NO_BORDER);
        emptyCell.setPadding(4);
        barTable.addCell(emptyCell);

        barCell.addElement(barTable);
        innerTable.addCell(barCell);

        // Valor
        PdfPCell valueCell = new PdfPCell(new Phrase(count + " (" + String.format("%.0f", percentage) + "%)",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, color)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setPadding(3);
        innerTable.addCell(valueCell);

        barContainer.addElement(innerTable);
        table.addCell(barContainer);
    }




}

