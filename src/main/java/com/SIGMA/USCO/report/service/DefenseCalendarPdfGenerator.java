package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.report.dto.DefenseCalendarReportDTO;
import com.SIGMA.USCO.report.dto.DefenseCalendarReportDTO.*;
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

@Service
@RequiredArgsConstructor
public class DefenseCalendarPdfGenerator {

    public byte[] generatePdf(DefenseCalendarReportDTO report) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        writer.setPageEvent(new InstitutionalPageEventHelper(report.getAcademicProgramName()));
        document.open();

        // Portada
        addCoverPage(document, report);
        document.newPage();

        // Resumen Ejecutivo
        InstitutionalPdfHeader.addInternalHeaderLight(document, report.getAcademicProgramName());
        addExecutiveSummary(document, report);

        // Sustentaciones Próximas
        if (report.getUpcomingDefenses() != null && !report.getUpcomingDefenses().isEmpty()) {
            document.newPage();
            InstitutionalPdfHeader.addInternalHeaderLight(document, report.getAcademicProgramName());
            addUpcomingDefenses(document, report.getUpcomingDefenses());
        }

        // Sustentaciones en Progreso
        if (report.getInProgressDefenses() != null && !report.getInProgressDefenses().isEmpty()) {
            document.newPage();
            InstitutionalPdfHeader.addInternalHeaderLight(document, report.getAcademicProgramName());
            addInProgressDefenses(document, report.getInProgressDefenses());
        }

        // Sustentaciones Completadas
        if (report.getRecentCompletedDefenses() != null && !report.getRecentCompletedDefenses().isEmpty()) {
            document.newPage();
            InstitutionalPdfHeader.addInternalHeaderLight(document, report.getAcademicProgramName());
            addCompletedDefenses(document, report.getRecentCompletedDefenses());
        }

        // Estadísticas
        document.newPage();
        InstitutionalPdfHeader.addInternalHeaderLight(document, report.getAcademicProgramName());
        addStatistics(document, report.getStatistics());

        // Análisis Mensual
        if (report.getMonthlyAnalysis() != null && !report.getMonthlyAnalysis().isEmpty()) {
            document.newPage();
            InstitutionalPdfHeader.addInternalHeaderLight(document, report.getAcademicProgramName());
            addMonthlyAnalysis(document, report.getMonthlyAnalysis());
        }

        // Alertas
        if (report.getAlerts() != null && !report.getAlerts().isEmpty()) {
            document.newPage();
            InstitutionalPdfHeader.addInternalHeaderLight(document, report.getAcademicProgramName());
            addAlerts(document, report.getAlerts());
        }

        // Información del reporte (cierre)
        document.newPage();
        InstitutionalPdfHeader.addInternalHeaderLight(document, report.getAcademicProgramName());
        addFooter(document, report);
        InstitutionalPdfHeader.addFooterSection(document,
                "Documento generado autom\u00e1ticamente por el Sistema SIGMA.\n" +
                "Universidad Surcolombiana | Facultad de Ingenier\u00eda | Neiva \u2013 Huila\n" +
                "www.usco.edu.co  \u2022  NIT: 891180084-2",
                "Sistema Integral de Gesti\u00f3n de Modalidades de Grado \u2014 SIGMA\n" +
                "Universidad Surcolombiana | Facultad de Ingenier\u00eda | Neiva \u2013 Huila");

        document.close();
        return outputStream.toByteArray();
    }

    private void addCoverPage(Document document, DefenseCalendarReportDTO report) throws DocumentException, IOException {
        List<String[]> infoRows = new ArrayList<>();
        infoRows.add(new String[]{"Programa académico:", report.getAcademicProgramName()});
        if (report.getAcademicProgramCode() != null) {
            infoRows.add(new String[]{"Código del programa:", report.getAcademicProgramCode()});
        }
        infoRows.add(new String[]{"Fecha de generación:",
                report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_FULL)});
        infoRows.add(new String[]{"Generado por:", report.getGeneratedBy()});
        if (report.getAppliedFilters() != null && report.getAppliedFilters().getHasFilters()) {
            infoRows.add(new String[]{"Filtros aplicados:",
                    report.getAppliedFilters().getFilterDescription()});
        }

        InstitutionalPdfHeader.addCoverPage(
                document,
                report.getAcademicProgramName() + (report.getAcademicProgramCode() != null
                        ? " \u2014 C\u00f3d. " + report.getAcademicProgramCode() : ""),
                "Reporte de Calendario de Sustentaciones y Evaluaciones",
                "REPORTE DE CALENDARIO DE\nSUSTENTACIONES Y EVALUACIONES",
                List.of(),
                infoRows,
                "Este reporte presenta el calendario de sustentaciones y evaluaciones de modalidades de grado, " +
                "incluyendo sustentaciones pr\u00f3ximas, en progreso, completadas y estad\u00edsticas de desempe\u00f1o acad\u00e9mico. " +
                "La informaci\u00f3n es generada autom\u00e1ticamente por el Sistema SIGMA.");
    }

    private void addExecutiveSummary(Document document, DefenseCalendarReportDTO report) throws DocumentException {
        InstitutionalPdfHeader.addSectionTitle(document, "1. RESUMEN EJECUTIVO", 20f);

        ExecutiveSummaryDTO summary = report.getExecutiveSummary();

        PdfPTable summaryGrid = new PdfPTable(3);
        summaryGrid.setWidthPercentage(100);
        summaryGrid.setSpacingAfter(20);

        addSummaryCard(summaryGrid, "TOTAL PROGRAMADAS", String.valueOf(summary.getTotalScheduled()), InstitutionalPdfHeader.INST_RED);
        addSummaryCard(summaryGrid, "ESTA SEMANA", String.valueOf(summary.getUpcomingThisWeek()), InstitutionalPdfHeader.INST_RED);
        addSummaryCard(summaryGrid, "HOY", String.valueOf(summary.getDefensesToday()), InstitutionalPdfHeader.INST_RED);

        addSummaryCard(summaryGrid, "ESTE MES", String.valueOf(summary.getUpcomingThisMonth()), InstitutionalPdfHeader.INST_GOLD);
        addSummaryCard(summaryGrid, "EN PROGRESO", String.valueOf(summary.getPendingScheduling()), InstitutionalPdfHeader.INST_RED);
        addSummaryCard(summaryGrid, "COMPLETADAS (MES)", String.valueOf(summary.getCompletedThisMonth()), InstitutionalPdfHeader.INST_GOLD);

        document.add(summaryGrid);

        addSuccessRateIndicator(document, summary.getAverageSuccessRate());

        InstitutionalPdfHeader.addSubsectionTitle(document, "Informaci\u00f3n Clave");

        PdfPTable detailsTable = new PdfPTable(new float[]{1, 2});
        detailsTable.setWidthPercentage(100);
        detailsTable.setSpacingAfter(20);

        InstitutionalPdfHeader.addDetailRow(detailsTable, "Pr\u00f3xima Sustentaci\u00f3n:", summary.getNextDefenseDate());
        InstitutionalPdfHeader.addDetailRow(detailsTable, "Tasa de \u00c9xito Promedio:", String.format("%.2f%%", summary.getAverageSuccessRate()));
        InstitutionalPdfHeader.addDetailRow(detailsTable, "Total Jurados Involucrados:", String.valueOf(summary.getTotalExaminersInvolved()));
        InstitutionalPdfHeader.addDetailRow(detailsTable, "Pendientes Vencidas:", String.valueOf(summary.getOverduePending()));

        document.add(detailsTable);
    }

    private void addUpcomingDefenses(Document document, List<UpcomingDefenseDTO> defenses) throws DocumentException {
        InstitutionalPdfHeader.addSectionTitle(document, "2. CALENDARIO DE SUSTENTACIONES PR\u00d3XIMAS", 20f);

        for (UpcomingDefenseDTO defense : defenses) {
            PdfPTable card = new PdfPTable(1);
            card.setWidthPercentage(100);
            card.setSpacingAfter(15);

            BaseColor urgencyColor = defense.getUrgency().equals("URGENT") ? InstitutionalPdfHeader.INST_RED
                    : defense.getUrgency().equals("SOON") ? InstitutionalPdfHeader.INST_RED : InstitutionalPdfHeader.INST_GOLD;

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(urgencyColor);
            headerCell.setPadding(10);
            headerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph headerText = new Paragraph();
            headerText.add(new Chunk(defense.getModalityTypeName() + " - ", new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE)));
            headerText.add(new Chunk("ID: " + defense.getModalityId(), new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.WHITE)));
            headerText.add(Chunk.NEWLINE);
            headerText.add(new Chunk(defense.getDefenseDate().format(InstitutionalPdfHeader.DATE_FULL), new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE)));
            headerCell.addElement(headerText);
            card.addCell(headerCell);

            PdfPCell contentCell = new PdfPCell();
            contentCell.setPadding(15);
            contentCell.setBorder(Rectangle.BOX);
            contentCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);

            Paragraph info = new Paragraph();
            info.add(new Chunk("D\u00edas hasta sustentaci\u00f3n: ", InstitutionalPdfHeader.BOLD_FONT));
            info.add(new Chunk(defense.getDaysUntilDefense() + " d\u00edas", InstitutionalPdfHeader.NORMAL_FONT));
            info.add(Chunk.NEWLINE);
            info.add(new Chunk("Ubicaci\u00f3n: ", InstitutionalPdfHeader.BOLD_FONT));
            info.add(new Chunk(defense.getDefenseLocation() != null ? defense.getDefenseLocation() : "Por definir", InstitutionalPdfHeader.NORMAL_FONT));
            info.add(Chunk.NEWLINE);
            info.add(new Chunk("Preparaci\u00f3n: ", InstitutionalPdfHeader.BOLD_FONT));
            info.add(new Chunk(String.format("%.0f%%", defense.getReadinessPercentage()), InstitutionalPdfHeader.NORMAL_FONT));
            info.add(Chunk.NEWLINE);
            info.add(Chunk.NEWLINE);

            info.add(new Chunk("Estudiantes:", InstitutionalPdfHeader.BOLD_FONT));
            info.add(Chunk.NEWLINE);
            for (StudentBasicInfoDTO student : defense.getStudents()) {
                info.add(new Chunk("  \u2022 " + student.getFullName() + (student.getIsLeader() ? " (L\u00edder)" : ""), InstitutionalPdfHeader.TINY_FONT));
                info.add(Chunk.NEWLINE);
            }
            info.add(Chunk.NEWLINE);

            info.add(new Chunk("Director: ", InstitutionalPdfHeader.BOLD_FONT));
            info.add(new Chunk(defense.getDirectorName(), InstitutionalPdfHeader.NORMAL_FONT));
            info.add(Chunk.NEWLINE);
            info.add(Chunk.NEWLINE);

            if (!defense.getExaminers().isEmpty()) {
                info.add(new Chunk("Jurados:", InstitutionalPdfHeader.BOLD_FONT));
                info.add(Chunk.NEWLINE);
                for (ExaminerInfoDTO examiner : defense.getExaminers()) {
                    info.add(new Chunk("  \u2022 " + examiner.getFullName() + " - " + examiner.getExaminerType(), InstitutionalPdfHeader.TINY_FONT));
                    info.add(Chunk.NEWLINE);
                }
            } else {
                info.add(new Chunk("\u26a0 Sin jurados asignados", new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, InstitutionalPdfHeader.INST_RED)));
                info.add(Chunk.NEWLINE);
            }

            if (!defense.getPendingTasks().isEmpty()) {
                info.add(Chunk.NEWLINE);
                info.add(new Chunk("Tareas Pendientes:", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, InstitutionalPdfHeader.INST_RED)));
                info.add(Chunk.NEWLINE);
                for (String task : defense.getPendingTasks()) {
                    info.add(new Chunk("  \u2022 " + task, InstitutionalPdfHeader.TINY_FONT));
                    info.add(Chunk.NEWLINE);
                }
            }

            contentCell.addElement(info);
            card.addCell(contentCell);

            document.add(card);
        }
    }

    private void addInProgressDefenses(Document document, List<InProgressDefenseDTO> defenses) throws DocumentException {
        InstitutionalPdfHeader.addSectionTitle(document, "3. SUSTENTACIONES EN PROGRESO", 20f);

        PdfPTable table = new PdfPTable(new float[]{2, 2, 2, 1, 2, 1});
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        InstitutionalPdfHeader.addTableHeader(table, "Modalidad");
        InstitutionalPdfHeader.addTableHeader(table, "Estudiantes");
        InstitutionalPdfHeader.addTableHeader(table, "Director");
        InstitutionalPdfHeader.addTableHeader(table, "Estado");
        InstitutionalPdfHeader.addTableHeader(table, "Siguiente Acci\u00f3n");
        InstitutionalPdfHeader.addTableHeader(table, "Progreso");

        for (InProgressDefenseDTO defense : defenses) {
            String students = defense.getStudents().stream()
                    .map(StudentBasicInfoDTO::getFullName)
                    .collect(java.util.stream.Collectors.joining(", "));

            addTableCell(table, defense.getModalityType());
            addTableCell(table, students);
            addTableCell(table, defense.getDirectorName());
            addTableCell(table, defense.getCurrentStatus());
            addTableCell(table, defense.getNextAction());
            addTableCell(table, String.format("%.0f%%", defense.getProgressPercentage()));
        }

        document.add(table);
    }

    private void addCompletedDefenses(Document document, List<CompletedDefenseDTO> defenses) throws DocumentException {
        InstitutionalPdfHeader.addSectionTitle(document, "4. SUSTENTACIONES COMPLETADAS RECIENTES", 20f);

        PdfPTable table = new PdfPTable(new float[]{2, 2, 2, 1, 1, 2, 1});
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        InstitutionalPdfHeader.addTableHeader(table, "Modalidad");
        InstitutionalPdfHeader.addTableHeader(table, "Estudiantes");
        InstitutionalPdfHeader.addTableHeader(table, "Director");
        InstitutionalPdfHeader.addTableHeader(table, "Fecha");
        InstitutionalPdfHeader.addTableHeader(table, "Nota");
        InstitutionalPdfHeader.addTableHeader(table, "Resultado");
        InstitutionalPdfHeader.addTableHeader(table, "D\u00edas");

        for (CompletedDefenseDTO defense : defenses) {
            String students = defense.getStudents().stream()
                    .map(StudentBasicInfoDTO::getFullName)
                    .collect(java.util.stream.Collectors.joining(", "));

            addTableCell(table, defense.getModalityType());
            addTableCell(table, students);
            addTableCell(table, defense.getDirectorName());
            addTableCell(table, defense.getDefenseDate().format(InstitutionalPdfHeader.DATE_SHORT));
            addTableCell(table, defense.getFinalGrade() != null ? String.format("%.2f", defense.getFinalGrade()) : "N/A");

            BaseColor resultColor;
            if ("APROBADO".equals(defense.getResult())) {
                resultColor = InstitutionalPdfHeader.INST_GOLD;
            } else if ("REPROBADO".equals(defense.getResult())) {
                resultColor = InstitutionalPdfHeader.INST_RED;
            } else {
                resultColor = new BaseColor(255, 193, 7);
            }

            PdfPCell resultCell = new PdfPCell(new Phrase(defense.getResult(), InstitutionalPdfHeader.TINY_FONT));
            resultCell.setBackgroundColor(resultColor);
            resultCell.setPadding(5);
            resultCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(resultCell);

            addTableCell(table, defense.getDaysAgo() + "d");
        }

        document.add(table);
    }

    private void addStatistics(Document document, DefenseStatisticsDTO statistics) throws DocumentException {
        InstitutionalPdfHeader.addSectionTitle(document, "5. ESTAD\u00cdSTICAS GENERALES", 20f);

        InstitutionalPdfHeader.addSubsectionTitle(document, "5.1 Resumen de Sustentaciones");

        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingAfter(20);

        addStatCard(statsTable, "Total Programadas", String.valueOf(statistics.getTotalScheduled()), InstitutionalPdfHeader.INST_RED);
        addStatCard(statsTable, "Completadas", String.valueOf(statistics.getTotalCompleted()), InstitutionalPdfHeader.INST_GOLD);
        addStatCard(statsTable, "Pendientes", String.valueOf(statistics.getTotalPending()), InstitutionalPdfHeader.INST_RED);
        addStatCard(statsTable, "Aprobadas", String.valueOf(statistics.getApproved()), InstitutionalPdfHeader.INST_GOLD);

        document.add(statsTable);

        addDefenseDistributionChart(document, statistics);

        InstitutionalPdfHeader.addSubsectionTitle(document, "5.2 Tasas de \u00c9xito y Calificaciones");

        PdfPTable ratesTable = new PdfPTable(new float[]{1, 1});
        ratesTable.setWidthPercentage(100);
        ratesTable.setSpacingAfter(20);

        InstitutionalPdfHeader.addDetailRow(ratesTable, "Tasa de Aprobaci\u00f3n:", String.format("%.2f%%", statistics.getApprovalRate()));
        InstitutionalPdfHeader.addDetailRow(ratesTable, "Tasa de Distinci\u00f3n:", String.format("%.2f%%", statistics.getDistinctionRate()));
        InstitutionalPdfHeader.addDetailRow(ratesTable, "Calificaci\u00f3n Promedio:", String.format("%.2f", statistics.getAverageGrade()));
        InstitutionalPdfHeader.addDetailRow(ratesTable, "Calificaci\u00f3n M\u00e1s Alta:", String.format("%.2f", statistics.getHighestGrade()));
        InstitutionalPdfHeader.addDetailRow(ratesTable, "Calificaci\u00f3n M\u00e1s Baja:", String.format("%.2f", statistics.getLowestGrade()));

        document.add(ratesTable);

        addGradeDistributionChart(document, statistics);
    }

    private void addMonthlyAnalysis(Document document, List<MonthlyDefenseAnalysisDTO> monthlyData) throws DocumentException {
        InstitutionalPdfHeader.addSectionTitle(document, "6. AN\u00c1LISIS MENSUAL", 20f);

        addMonthlyEvolutionChart(document, monthlyData);

        InstitutionalPdfHeader.addSubsectionTitle(document, "Detalle por Mes");

        PdfPTable table = new PdfPTable(new float[]{2, 1, 1, 1, 1, 1, 1});
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        InstitutionalPdfHeader.addTableHeader(table, "Periodo");
        InstitutionalPdfHeader.addTableHeader(table, "Programadas");
        InstitutionalPdfHeader.addTableHeader(table, "Completadas");
        InstitutionalPdfHeader.addTableHeader(table, "Pendientes");
        InstitutionalPdfHeader.addTableHeader(table, "Aprobadas");
        InstitutionalPdfHeader.addTableHeader(table, "Tasa \u00c9xito");
        InstitutionalPdfHeader.addTableHeader(table, "Nota Prom.");

        for (MonthlyDefenseAnalysisDTO month : monthlyData) {
            addTableCell(table, month.getPeriodLabel());
            addTableCell(table, String.valueOf(month.getTotalScheduled()));
            addTableCell(table, String.valueOf(month.getCompleted()));
            addTableCell(table, String.valueOf(month.getPending()));
            addTableCell(table, String.valueOf(month.getApproved()));
            addTableCell(table, String.format("%.2f%%", month.getSuccessRate()));
            addTableCell(table, String.format("%.2f", month.getAverageGrade()));
        }

        document.add(table);
    }

    private void addAlerts(Document document, List<DefenseAlertDTO> alerts) throws DocumentException {
        InstitutionalPdfHeader.addSectionTitle(document, "7. ALERTAS Y RECOMENDACIONES", 20f);

        for (DefenseAlertDTO alert : alerts) {
            BaseColor alertColor;
            if ("URGENT".equals(alert.getAlertType())) {
                alertColor = InstitutionalPdfHeader.INST_RED;
            } else if ("WARNING".equals(alert.getAlertType())) {
                alertColor = new BaseColor(255, 152, 0);
            } else {
                alertColor = InstitutionalPdfHeader.INST_GOLD;
            }

            PdfPTable alertBox = new PdfPTable(1);
            alertBox.setWidthPercentage(100);
            alertBox.setSpacingAfter(15);

            PdfPCell alertCell = new PdfPCell();
            alertCell.setBorderColor(alertColor);
            alertCell.setBorderWidth(2);
            alertCell.setPadding(15);

            Paragraph alertContent = new Paragraph();
            alertContent.add(new Chunk("\u26a0 " + alert.getTitle(), new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, alertColor)));
            alertContent.add(Chunk.NEWLINE);
            alertContent.add(new Chunk(alert.getDescription(), InstitutionalPdfHeader.NORMAL_FONT));
            alertContent.add(Chunk.NEWLINE);

            if (alert.getStudentName() != null) {
                alertContent.add(new Chunk("Estudiante: " + alert.getStudentName(), InstitutionalPdfHeader.BOLD_FONT));
                alertContent.add(Chunk.NEWLINE);
            }

            if (alert.getDefenseDate() != null) {
                alertContent.add(new Chunk("Fecha de Sustentaci\u00f3n: " + alert.getDefenseDate().format(InstitutionalPdfHeader.DATE_FULL), InstitutionalPdfHeader.NORMAL_FONT));
                alertContent.add(Chunk.NEWLINE);
            }

            if (alert.getActionRequired() != null) {
                alertContent.add(Chunk.NEWLINE);
                alertContent.add(new Chunk("Acci\u00f3n Requerida: " + alert.getActionRequired(), new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, InstitutionalPdfHeader.INST_RED)));
            }

            alertCell.addElement(alertContent);
            alertBox.addCell(alertCell);

            document.add(alertBox);
        }
    }

    private void addFooter(Document document, DefenseCalendarReportDTO report) throws DocumentException {
        InstitutionalPdfHeader.addSectionTitle(document, "INFORMACI\u00d3N DEL REPORTE", 20f);

        PdfPTable metaTable = new PdfPTable(new float[]{1, 2});
        metaTable.setWidthPercentage(100);

        if (report.getMetadata() != null) {
            InstitutionalPdfHeader.addDetailRow(metaTable, "Total de Registros:", String.valueOf(report.getMetadata().getTotalRecords()));
            InstitutionalPdfHeader.addDetailRow(metaTable, "Visi\u00f3n del Reporte:", report.getMetadata().getReportVersion());
        }

        InstitutionalPdfHeader.addDetailRow(metaTable, "Generado por:", report.getGeneratedBy());
        InstitutionalPdfHeader.addDetailRow(metaTable, "Fecha y Hora:", report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_FULL));
        InstitutionalPdfHeader.addDetailRow(metaTable, "Programa:", report.getAcademicProgramName());

        document.add(metaTable);
    }

    // ── Helpers específicos de este reporte ──────────────────────────────────


    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, InstitutionalPdfHeader.TINY_FONT));
        cell.setPadding(6);
        cell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        table.addCell(cell);
    }

    private void addSummaryCard(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(color);
        cell.setBorderWidth(2);
        cell.setPadding(15);
        cell.setBackgroundColor(new BaseColor(color.getRed(), color.getGreen(), color.getBlue(), 25));

        Paragraph content = new Paragraph();
        content.setAlignment(Element.ALIGN_CENTER);

        Chunk valueChunk = new Chunk(value, new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, color));
        content.add(valueChunk);
        content.add(Chunk.NEWLINE);
        content.add(new Chunk(label, new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, InstitutionalPdfHeader.TEXT_GRAY)));

        cell.addElement(content);
        table.addCell(cell);
    }

    private void addStatCard(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(color);
        cell.setPadding(15);
        cell.setBackgroundColor(new BaseColor(color.getRed(), color.getGreen(), color.getBlue(), 25));

        Paragraph content = new Paragraph();
        content.setAlignment(Element.ALIGN_CENTER);

        Chunk valueChunk = new Chunk(value, new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, color));
        content.add(valueChunk);
        content.add(Chunk.NEWLINE);
        content.add(new Chunk(label, InstitutionalPdfHeader.TINY_FONT));

        cell.addElement(content);
        table.addCell(cell);
    }

    // ── Visualizaciones específicas ──────────────────────────────────────────

    private void addSuccessRateIndicator(Document document, Double successRate) throws DocumentException {
        InstitutionalPdfHeader.addSubsectionTitle(document, "Indicador de Tasa de \u00c9xito");

        PdfPTable indicatorTable = new PdfPTable(1);
        indicatorTable.setWidthPercentage(90);
        indicatorTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        indicatorTable.setSpacingBefore(10);
        indicatorTable.setSpacingAfter(20);

        PdfPCell indicatorCell = new PdfPCell();
        indicatorCell.setPadding(0);
        indicatorCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{1.5f, 5f, 1f});
        } catch (DocumentException e) {
        }

        PdfPCell labelCell = new PdfPCell(new Phrase("Tasa de \u00c9xito:", InstitutionalPdfHeader.BOLD_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        innerTable.addCell(labelCell);

        float percentage = successRate.floatValue() / 100f;
        BaseColor barColor;
        if (percentage >= 0.8) {
            barColor = InstitutionalPdfHeader.INST_GOLD;
        } else if (percentage >= 0.6) {
            barColor = new BaseColor(255, 193, 7);
        } else {
            barColor = InstitutionalPdfHeader.INST_RED;
        }

        PdfPCell barCell = createSuccessRateBar(successRate, percentage, barColor);
        innerTable.addCell(barCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(String.format("%.2f%%", successRate),
                new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, barColor)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        innerTable.addCell(valueCell);

        indicatorCell.addElement(innerTable);
        indicatorTable.addCell(indicatorCell);

        document.add(indicatorTable);
    }

    private PdfPCell createSuccessRateBar(Double value, float percentage, BaseColor color) {
        PdfPTable barContainer = new PdfPTable(2);
        float barWidth = Math.max(percentage * 100, 3);
        float emptyWidth = 100 - barWidth;

        try {
            barContainer.setWidths(new float[]{barWidth, emptyWidth});
        } catch (DocumentException e) {
            try {
                barContainer.setWidths(new float[]{50, 50});
            } catch (DocumentException ex) {
            }
        }
        barContainer.setWidthPercentage(100);

        PdfPCell filledCell = new PdfPCell(new Phrase(String.format("%.1f%%", value),
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, InstitutionalPdfHeader.WHITE)));
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(6);
        filledCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        barContainer.addCell(filledCell);

        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        emptyCell.setBorder(Rectangle.NO_BORDER);
        barContainer.addCell(emptyCell);

        PdfPCell containerCell = new PdfPCell();
        containerCell.addElement(barContainer);
        containerCell.setBorder(Rectangle.BOX);
        containerCell.setBorderColor(color);
        containerCell.setBorderWidth(1f);
        containerCell.setPadding(0);

        return containerCell;
    }

    private void addDefenseDistributionChart(Document document, DefenseStatisticsDTO statistics)
            throws DocumentException {

        InstitutionalPdfHeader.addSubsectionTitle(document, "Distribuci\u00f3n de Estados");

        int scheduled = statistics.getTotalScheduled();
        int completed = statistics.getTotalCompleted();
        int pending = statistics.getTotalPending();
        int total = scheduled;

        if (total == 0) return;

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(20);

        addDistributionBar(chartTable, "Programadas", scheduled, total, InstitutionalPdfHeader.INST_GOLD);
        addDistributionBar(chartTable, "Completadas", completed, total, InstitutionalPdfHeader.INST_GOLD);
        addDistributionBar(chartTable, "Pendientes", pending, total, InstitutionalPdfHeader.INST_RED);

        document.add(chartTable);
    }

    private void addDistributionBar(PdfPTable table, String label, int count, int total, BaseColor color) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(3);
        containerCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{1.5f, 4.5f, 1.5f});
        } catch (DocumentException e) {
        }

        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, InstitutionalPdfHeader.TEXT_BLACK)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPadding(3);
        innerTable.addCell(labelCell);

        float percentage = total > 0 ? (float) count / total : 0;
        PdfPCell barCell = createDistributionBarCell(count, percentage, color);
        innerTable.addCell(barCell);

        PdfPCell valueCell = new PdfPCell();
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setPadding(3);

        Paragraph valueContent = new Paragraph();
        valueContent.add(new Chunk(count + " ",
                new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, color)));
        valueContent.add(new Chunk("(" + String.format("%.1f%%", percentage * 100) + ")",
                new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, InstitutionalPdfHeader.TEXT_GRAY)));
        valueContent.setAlignment(Element.ALIGN_CENTER);
        valueCell.addElement(valueContent);
        innerTable.addCell(valueCell);

        containerCell.addElement(innerTable);
        table.addCell(containerCell);
    }

    private PdfPCell createDistributionBarCell(int value, float percentage, BaseColor color) {
        PdfPTable barContainer = new PdfPTable(2);
        float barWidth = Math.max(percentage * 100, 3);
        float emptyWidth = 100 - barWidth;

        try {
            barContainer.setWidths(new float[]{barWidth, emptyWidth});
        } catch (DocumentException e) {
            try {
                barContainer.setWidths(new float[]{50, 50});
            } catch (DocumentException ex) {
            }
        }
        barContainer.setWidthPercentage(100);

        PdfPCell filledCell = new PdfPCell(new Phrase(String.valueOf(value),
                new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, InstitutionalPdfHeader.WHITE)));
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(5);
        filledCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        barContainer.addCell(filledCell);

        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
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

    private void addGradeDistributionChart(Document document, DefenseStatisticsDTO statistics)
            throws DocumentException {

        InstitutionalPdfHeader.addSubsectionTitle(document, "Distribuci\u00f3n de Calificaciones");

        Double lowest = statistics.getLowestGrade();
        Double average = statistics.getAverageGrade();
        Double highest = statistics.getHighestGrade();

        if (lowest == null || highest == null) return;

        double maxValue = 5.0;

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(15);

        addGradeBar(chartTable, "M\u00e1s Baja", lowest, maxValue, InstitutionalPdfHeader.INST_RED);
        addGradeBar(chartTable, "Promedio", average, maxValue, InstitutionalPdfHeader.INST_GOLD);
        addGradeBar(chartTable, "M\u00e1s Alta", highest, maxValue, InstitutionalPdfHeader.INST_GOLD);

        document.add(chartTable);
    }

    private void addGradeBar(PdfPTable table, String label, double grade, double maxGrade, BaseColor color) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(3);
        containerCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{1.5f, 4.5f, 1f});
        } catch (DocumentException e) {
        }

        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, InstitutionalPdfHeader.TEXT_BLACK)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPadding(3);
        innerTable.addCell(labelCell);

        float percentage = (float) (grade / maxGrade);
        PdfPCell barCell = createGradeBarCell(grade, percentage, color);
        innerTable.addCell(barCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(String.format("%.2f", grade),
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, color)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setPadding(3);
        innerTable.addCell(valueCell);

        containerCell.addElement(innerTable);
        table.addCell(containerCell);
    }

    private PdfPCell createGradeBarCell(double grade, float percentage, BaseColor color) {
        PdfPTable barContainer = new PdfPTable(2);
        float barWidth = Math.max(percentage * 100, 3);
        float emptyWidth = 100 - barWidth;

        try {
            barContainer.setWidths(new float[]{barWidth, emptyWidth});
        } catch (DocumentException e) {
            try {
                barContainer.setWidths(new float[]{50, 50});
            } catch (DocumentException ex) {
            }
        }
        barContainer.setWidthPercentage(100);

        PdfPCell filledCell = new PdfPCell(new Phrase(String.format("%.2f", grade),
                new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, InstitutionalPdfHeader.WHITE)));
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(5);
        filledCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        barContainer.addCell(filledCell);

        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
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

    private void addMonthlyEvolutionChart(Document document, List<MonthlyDefenseAnalysisDTO> monthlyData)
            throws DocumentException {

        if (monthlyData.isEmpty()) return;

        InstitutionalPdfHeader.addSubsectionTitle(document, "Evoluci\u00f3n de Sustentaciones por Mes");

        int maxCompleted = monthlyData.stream()
                .mapToInt(MonthlyDefenseAnalysisDTO::getCompleted)
                .max()
                .orElse(1);

        PdfPTable chartTable = new PdfPTable(1);
        chartTable.setWidthPercentage(100);
        chartTable.setSpacingBefore(10);
        chartTable.setSpacingAfter(20);

        for (MonthlyDefenseAnalysisDTO month : monthlyData) {
            addMonthEvolutionBar(chartTable, month, maxCompleted);
        }

        document.add(chartTable);
    }

    private void addMonthEvolutionBar(PdfPTable table, MonthlyDefenseAnalysisDTO month, int maxValue) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(3);
        containerCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{1.2f, 4.5f, 1.8f});
        } catch (DocumentException e) {
        }

        PdfPCell periodCell = new PdfPCell(new Phrase(month.getPeriodLabel(),
                new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, InstitutionalPdfHeader.TEXT_BLACK)));
        periodCell.setBorder(Rectangle.NO_BORDER);
        periodCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        periodCell.setPadding(3);
        innerTable.addCell(periodCell);

        float percentage = maxValue > 0 ? (float) month.getCompleted() / maxValue : 0;
        BaseColor barColor = month.getSuccessRate() >= 70 ? InstitutionalPdfHeader.INST_GOLD : InstitutionalPdfHeader.INST_RED;
        PdfPCell barCell = createDistributionBarCell(month.getCompleted(), percentage, barColor);
        innerTable.addCell(barCell);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        infoCell.setPadding(3);

        Paragraph infoContent = new Paragraph();
        infoContent.add(new Chunk(month.getCompleted() + " completadas",
                new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, barColor)));
        infoContent.add(new Chunk(" | " + String.format("%.1f%%", month.getSuccessRate()),
                new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, InstitutionalPdfHeader.TEXT_GRAY)));
        infoCell.addElement(infoContent);
        innerTable.addCell(infoCell);

        containerCell.addElement(innerTable);
        table.addCell(containerCell);
    }
}
