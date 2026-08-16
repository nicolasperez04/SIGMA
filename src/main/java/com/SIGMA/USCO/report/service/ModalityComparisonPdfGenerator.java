package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.report.dto.ModalityTypeComparisonReportDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ModalityComparisonPdfGenerator extends BaseReportPdfGenerator {

    public ByteArrayOutputStream generatePDF(ModalityTypeComparisonReportDTO report)
            throws DocumentException, IOException {

        PdfSession session = openDocument(PageSize.A4, 50, 50, 50, 50, report.getAcademicProgramName(), "Reporte Comparativo de Modalidades");

        // 1. Portada institucional
        addCoverPage(session.document(), report);

        // 2. Resumen ejecutivo
        newPageWithHeader(session, "Reporte Comparativo \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addExecutiveSummary(session.document(), report);

        // 3. Análisis visual comparativo
        newPageWithHeader(session, "Reporte Comparativo \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addVisualComparison(session.document(), report);

        // 4. Estadísticas detalladas por tipo
        newPageWithHeader(session, "Reporte Comparativo \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addDetailedStatistics(session.document(), report);

        // 5. Distribución de estudiantes (opcional)
        if (report.getStudentDistributionByType() != null
                && !report.getStudentDistributionByType().isEmpty()) {
            newPageWithHeader(session, "Reporte Comparativo \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
            addStudentDistribution(session.document(), report);
        }

        // 6. Análisis de eficiencia
        newPageWithHeader(session, "Reporte Comparativo \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addEfficiencyAnalysis(session.document(), report);

        // 7. Comparación histórica (opcional)
        if (report.getHistoricalComparison() != null && !report.getHistoricalComparison().isEmpty()) {
            newPageWithHeader(session, "Reporte Comparativo \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
            addHistoricalComparison(session.document(), report);
        }

        // 8. Análisis de tendencias (opcional)
        if (report.getTrendsAnalysis() != null) {
            newPageWithHeader(session, "Reporte Comparativo \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
            addTrendsAnalysis(session.document(), report);
        }

        // 9. Conclusiones + pie institucional
        newPageWithHeader(session, "Reporte Comparativo \u2014 " + report.getGeneratedAt().format(InstitutionalPdfHeader.DATE_SHORT));
        addConclusions(session.document(), report);
        InstitutionalPdfHeader.addFooterSection(session.document(),
                "NOTA: Este reporte fue generado automáticamente por el Sistema SIGMA. "
                        + "Los datos corresponden al programa acad\u00e9mico "
                        + report.getAcademicProgramName()
                        + " y est\u00e1n filtrados seg\u00fan los criterios especificados. "
                        + "Para consultas o an\u00e1lisis adicionales, contacte con la coordinaci\u00f3n del programa.",
                "Sistema Integral de Gesti\u00f3n de Modalidades de Grado \u2014 SIGMA\n"
                        + "Universidad Surcolombiana \u00b7 Facultad de Ingenier\u00eda");

        close(session);
        return session.out();
    }

    // =========================================================================
    //  PORTADA INSTITUCIONAL
    // =========================================================================

    private void addCoverPage(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException, IOException {

        List<String> boxLines = new ArrayList<>();
        boxLines.add(report.getAcademicProgramName().toUpperCase());
        if (report.getYear() != null) {
            boxLines.add("Periodo: " + report.getYear()
                    + (report.getSemester() != null ? " \u2014 Semestre " + report.getSemester() : ""));
        }

        List<String[]> infoRows = new ArrayList<>();
        infoRows.add(new String[]{"Programa:", report.getAcademicProgramName()});
        if (report.getAcademicProgramCode() != null) {
            infoRows.add(new String[]{"C\u00f3digo:", report.getAcademicProgramCode()});
        }
        infoRows.add(new String[]{"Fecha de generaci\u00f3n:",
                report.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy, HH:mm"))});
        infoRows.add(new String[]{"Generado por:", report.getGeneratedBy().split(" \\(")[0]});
        infoRows.add(new String[]{"Tipos de modalidad:",
                String.valueOf(report.getSummary().getTotalModalityTypes())});
        infoRows.add(new String[]{"Total de modalidades:",
                String.valueOf(report.getSummary().getTotalModalities())});

        InstitutionalPdfHeader.addCoverPage(
                document,
                report.getAcademicProgramName()
                        + (report.getAcademicProgramCode() != null
                            ? " \u2014 C\u00f3d. " + report.getAcademicProgramCode() : ""),
                "Reporte Comparativo de Modalidades por Tipo de Grado",
                "REPORTE COMPARATIVO DE\nMODALIDADES POR TIPO DE GRADO",
                boxLines,
                infoRows,
                null);

        document.newPage();
    }

    // =========================================================================
    //  SECCIONES DE CONTENIDO
    // =========================================================================

    private void addExecutiveSummary(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "1. RESUMEN EJECUTIVO");

        ModalityTypeComparisonReportDTO.ComparisonSummaryDTO summary = report.getSummary();

        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        try { summaryTable.setWidths(new int[]{25, 25, 25, 25}); } catch (DocumentException ignored) {}
        summaryTable.setSpacingAfter(12f);

        addMetricCard(summaryTable, "Tipos de Modalidad",
                String.valueOf(summary.getTotalModalityTypes()), InstitutionalPdfHeader.INST_GOLD);
        addMetricCard(summaryTable, "Total Modalidades",
                String.valueOf(summary.getTotalModalities()), InstitutionalPdfHeader.INST_RED);
        addMetricCard(summaryTable, "Total Estudiantes",
                String.valueOf(summary.getTotalStudents()), InstitutionalPdfHeader.INST_GOLD);
        addMetricCard(summaryTable, "Prom. por Tipo",
                String.format("%.1f", summary.getAverageModalitiesPerType()), InstitutionalPdfHeader.INST_RED);
        document.add(summaryTable);

        PdfPTable secondRow = new PdfPTable(2);
        secondRow.setWidthPercentage(100);
        secondRow.setSpacingAfter(18f);

        addWideMetricCard(secondRow, "Promedio de Estudiantes por Tipo",
                String.format("%.1f estudiantes", summary.getAverageStudentsPerType()),
                InstitutionalPdfHeader.LIGHT_GOLD, InstitutionalPdfHeader.INST_RED);

        if (summary.getMostPopularType() != null && summary.getTotalModalities() > 0) {
            double pct = (double) summary.getMostPopularTypeCount() / summary.getTotalModalities() * 100;
            addWideMetricCard(secondRow, "Concentraci\u00f3n en Tipo Principal",
                    String.format("%.1f%% en %s", pct, summary.getMostPopularType()),
                    InstitutionalPdfHeader.LIGHT_GOLD, InstitutionalPdfHeader.INST_GOLD);
        }
        document.add(secondRow);

        if (summary.getMostPopularType() != null) {
            addHighlightBox(document,
                    "TIPO M\u00c1S POPULAR: " + summary.getMostPopularType()
                            + " (" + summary.getMostPopularTypeCount() + " modalidades)",
                    InstitutionalPdfHeader.INST_GOLD, InstitutionalPdfHeader.WHITE, 1.5f);
        }

        if (summary.getLeastPopularType() != null && summary.getLeastPopularTypeCount() > 0) {
            addHighlightBox(document,
                    "TIPO MENOS POPULAR: " + summary.getLeastPopularType()
                            + " (" + summary.getLeastPopularTypeCount() + " modalidades)",
                    InstitutionalPdfHeader.LIGHT_GOLD, InstitutionalPdfHeader.INST_RED, 1f);
        }
    }

    private void addVisualComparison(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "2. AN\u00c1LISIS VISUAL COMPARATIVO");

        Paragraph intro = new Paragraph(
                "Comparaci\u00f3n de la distribuci\u00f3n de modalidades y estudiantes por tipo:",
                InstitutionalPdfHeader.NORMAL_FONT);
        intro.setSpacingAfter(14f);
        document.add(intro);

        List<ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO> stats =
                report.getModalityTypeStatistics();

        int totalModalities = stats.stream()
                .mapToInt(ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO::getTotalModalities).sum();
        int totalStudents = stats.stream()
                .mapToInt(ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO::getTotalStudents).sum();

        InstitutionalPdfHeader.addSubsectionTitle(document, "2.1  Modalidades por tipo");
        for (ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO stat : stats) {
            addComparisonBar(document, stat.getModalityTypeName(),
                    stat.getTotalModalities(), totalModalities, "modalidades", InstitutionalPdfHeader.INST_RED);
        }

        InstitutionalPdfHeader.addSpacingParagraph(document, 10f);
        InstitutionalPdfHeader.addSubsectionTitle(document, "2.2  Estudiantes por tipo");
        for (ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO stat : stats) {
            addComparisonBar(document, stat.getModalityTypeName(),
                    stat.getTotalStudents(), totalStudents, "estudiantes", InstitutionalPdfHeader.INST_GOLD);
        }
    }

    private void addDetailedStatistics(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "3. ESTAD\u00cdSTICAS DETALLADAS POR TIPO DE MODALIDAD");

        List<ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO> statistics =
                report.getModalityTypeStatistics();
        int totalModalities = report.getSummary().getTotalModalities();

        for (int i = 0; i < statistics.size(); i++) {
            ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO stat = statistics.get(i);

            PdfPTable typeHeader = new PdfPTable(1);
            typeHeader.setWidthPercentage(100);
            typeHeader.setSpacingBefore(i > 0 ? 18f : 4f);
            typeHeader.setSpacingAfter(4f);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(InstitutionalPdfHeader.INST_RED);
            headerCell.setPadding(9f);
            headerCell.setBorder(Rectangle.NO_BORDER);
            headerCell.addElement(new Paragraph(
                    (i + 1) + ". " + stat.getModalityTypeName(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, InstitutionalPdfHeader.WHITE)));
            typeHeader.addCell(headerCell);
            document.add(typeHeader);

            if (stat.getDescription() != null && !stat.getDescription().isEmpty()) {
                Paragraph desc = new Paragraph(stat.getDescription(), InstitutionalPdfHeader.SMALL_FONT);
                desc.setIndentationLeft(10f);
                desc.setSpacingAfter(8f);
                document.add(desc);
            }

            PdfPTable metricsTable = new PdfPTable(2);
            metricsTable.setWidthPercentage(100);
            metricsTable.setSpacingAfter(8f);
            try { metricsTable.setWidths(new int[]{60, 40}); } catch (DocumentException ignored) {}

            addMetricRowWithBar(metricsTable, "Total Modalidades",
                    stat.getTotalModalities(), totalModalities, "modalidades");
            addMetricRowWithBar(metricsTable, "Total Estudiantes",
                    stat.getTotalStudents(), report.getSummary().getTotalStudents(), "estudiantes");
            document.add(metricsTable);

            PdfPTable statsTable = new PdfPTable(4);
            statsTable.setWidthPercentage(100);
            try { statsTable.setWidths(new int[]{25, 25, 25, 25}); } catch (DocumentException ignored) {}
            statsTable.setSpacingAfter(8f);

            addStatCellEnhanced(statsTable, "Porcentaje Total",
                    String.format("%.1f%%", stat.getPercentageOfTotal()), InstitutionalPdfHeader.INST_GOLD);
            addStatCellEnhanced(statsTable, "Prom. Est./Mod.",
                    String.format("%.2f", stat.getAverageStudentsPerModality()), InstitutionalPdfHeader.LIGHT_GOLD);
            addStatCellEnhanced(statsTable, "Tipo predominante",
                    stat.getIndividualModalities() > stat.getGroupModalities()
                            ? "Individual" : "Grupal", InstitutionalPdfHeader.INST_GOLD);
            addStatCellEnhanced(statsTable, "Director",
                    stat.getRequiresDirector() ? "Requerido" : "No requiere", InstitutionalPdfHeader.LIGHT_GOLD);
            document.add(statsTable);

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
                        withoutDir > 0 ? new BaseColor(255, 152, 0) : InstitutionalPdfHeader.TEXT_GRAY));
                document.add(dirTable);
            }

            if (stat.getDistributionByStatus() != null && !stat.getDistributionByStatus().isEmpty()) {
                Paragraph statusLbl = new Paragraph("Distribuci\u00f3n por Estado:", InstitutionalPdfHeader.BOLD_FONT);
                statusLbl.setSpacingBefore(6f);
                statusLbl.setSpacingAfter(4f);
                document.add(statusLbl);
                addStatusDistributionBars(document, stat.getDistributionByStatus(), stat.getTotalModalities());
            }

            if (stat.getTrend() != null) {
                BaseColor tColor; String tIcon; String tText;
                switch (stat.getTrend()) {
                    case "INCREASING" -> { tColor = new BaseColor(76, 175, 80);   tIcon = "\u2197"; tText = "EN CRECIMIENTO"; }
                    case "DECREASING" -> { tColor = new BaseColor(244, 67, 54);   tIcon = "\u2198"; tText = "EN DECLIVE"; }
                    default           -> { tColor = InstitutionalPdfHeader.INST_GOLD;           tIcon = "\u2192"; tText = "ESTABLE"; }
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

            if (i < statistics.size() - 1) {
                InstitutionalPdfHeader.addGoldLine(document);
            }
        }
    }

    private void addStudentDistribution(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "4. DISTRIBUCI\u00d3N DE ESTUDIANTES POR TIPO");

        Paragraph intro = new Paragraph(
                "Cantidad de estudiantes \u00fanicos por tipo de modalidad:", InstitutionalPdfHeader.NORMAL_FONT);
        intro.setSpacingAfter(14f);
        document.add(intro);

        Map<String, Integer> distribution = report.getStudentDistributionByType();
        int maxStudents   = distribution.values().stream().max(Integer::compare).orElse(1);
        int totalStudents = distribution.values().stream().mapToInt(Integer::intValue).sum();

        List<Map.Entry<String, Integer>> sorted = distribution.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .toList();

        for (Map.Entry<String, Integer> entry : sorted) {
            PdfPTable barContainer = new PdfPTable(1);
            barContainer.setWidthPercentage(100);
            barContainer.setSpacingAfter(10f);

            PdfPCell hdrCell = new PdfPCell();
            hdrCell.setBackgroundColor(InstitutionalPdfHeader.INST_RED);
            hdrCell.setPadding(6f);
            hdrCell.setBorder(Rectangle.NO_BORDER);
            hdrCell.addElement(new Paragraph(entry.getKey(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, InstitutionalPdfHeader.WHITE)));
            barContainer.addCell(hdrCell);

            float barWidth   = (float) entry.getValue() / maxStudents * 85;
            float emptyWidth = 100 - barWidth;

            PdfPTable inner = new PdfPTable(2);
            inner.setWidthPercentage(100);
            try { inner.setWidths(new float[]{Math.max(barWidth, 0.1f), Math.max(emptyWidth, 0.1f)}); }
            catch (DocumentException ignored) {}

            PdfPCell filled = new PdfPCell(new Phrase(
                    entry.getValue() + " estudiantes",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, InstitutionalPdfHeader.WHITE)));
            filled.setBackgroundColor(InstitutionalPdfHeader.INST_GOLD);
            filled.setBorder(Rectangle.NO_BORDER);
            filled.setPadding(8f);
            inner.addCell(filled);

            double pct = (double) entry.getValue() / totalStudents * 100;
            PdfPCell empty = new PdfPCell(new Phrase(
                    String.format("%.1f%% del total", pct),
                    FontFactory.getFont(FontFactory.HELVETICA, 9, InstitutionalPdfHeader.INST_RED)));
            empty.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
            empty.setBorder(Rectangle.NO_BORDER);
            empty.setPadding(8f);
            inner.addCell(empty);

            PdfPCell barCell = new PdfPCell();
            barCell.setPadding(0);
            barCell.setBorder(Rectangle.BOX);
            barCell.setBorderColor(InstitutionalPdfHeader.LIGHT_GOLD);
            barCell.setBorderWidth(0.5f);
            barCell.addElement(inner);
            barContainer.addCell(barCell);

            document.add(barContainer);
        }

        PdfPTable totalBox = new PdfPTable(1);
        totalBox.setWidthPercentage(100);
        totalBox.setSpacingBefore(12f);

        PdfPCell totalCell = new PdfPCell();
        totalCell.setBackgroundColor(InstitutionalPdfHeader.INST_GOLD);
        totalCell.setPadding(10f);
        totalCell.setBorder(Rectangle.NO_BORDER);
        Paragraph totalPara = new Paragraph("TOTAL DE ESTUDIANTES: " + totalStudents,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, InstitutionalPdfHeader.WHITE));
        totalPara.setAlignment(Element.ALIGN_CENTER);
        totalCell.addElement(totalPara);
        totalBox.addCell(totalCell);
        document.add(totalBox);
    }

    private void addEfficiencyAnalysis(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "5. AN\u00c1LISIS DE EFICIENCIA");

        List<ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO> stats =
                report.getModalityTypeStatistics();

        PdfPTable effTable = new PdfPTable(4);
        effTable.setWidthPercentage(100);
        effTable.setSpacingAfter(18f);
        try { effTable.setWidths(new int[]{35, 20, 25, 20}); } catch (DocumentException ignored) {}

        InstitutionalPdfHeader.addTableHeader(effTable, "Tipo de Modalidad");
        InstitutionalPdfHeader.addTableHeader(effTable, "Modalidades");
        InstitutionalPdfHeader.addTableHeader(effTable, "Prom. Est./Mod.");
        InstitutionalPdfHeader.addTableHeader(effTable, "Eficiencia");

        double avgEff = stats.stream()
                .mapToDouble(ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO::getAverageStudentsPerModality)
                .average().orElse(0);

        boolean alternate = false;
        for (ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO stat : stats) {
            BaseColor rowBg = alternate ? InstitutionalPdfHeader.LIGHT_GOLD : InstitutionalPdfHeader.WHITE;

            PdfPCell nameCell = new PdfPCell(new Phrase(stat.getModalityTypeName(), InstitutionalPdfHeader.NORMAL_FONT));
            nameCell.setBackgroundColor(rowBg);
            nameCell.setPadding(8f);
            effTable.addCell(nameCell);

            PdfPCell modCell = new PdfPCell(new Phrase(
                    String.valueOf(stat.getTotalModalities()), InstitutionalPdfHeader.BOLD_FONT));
            modCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            modCell.setBackgroundColor(rowBg);
            modCell.setPadding(8f);
            effTable.addCell(modCell);

            PdfPCell avgCell = new PdfPCell(new Phrase(
                    String.format("%.2f", stat.getAverageStudentsPerModality()), InstitutionalPdfHeader.BOLD_FONT));
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
                effText = "Normal"; effColor = InstitutionalPdfHeader.INST_GOLD;
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

        PdfPTable sumEff = new PdfPTable(2);
        sumEff.setWidthPercentage(80);
        sumEff.setSpacingBefore(8f);
        addSummaryRow(sumEff, "Promedio general de estudiantes por modalidad:",
                String.format("%.2f", avgEff), InstitutionalPdfHeader.BOLD_FONT);

        ModalityTypeComparisonReportDTO.ModalityTypeStatisticsDTO mostEfficient = stats.stream()
                .max((s1, s2) -> Double.compare(
                        s1.getAverageStudentsPerModality(), s2.getAverageStudentsPerModality()))
                .orElse(null);
        if (mostEfficient != null) {
            addSummaryRow(sumEff, "Tipo m\u00e1s eficiente:",
                    mostEfficient.getModalityTypeName()
                            + " (" + String.format("%.2f", mostEfficient.getAverageStudentsPerModality()) + ")",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new BaseColor(76, 175, 80)));
        }
        document.add(sumEff);
    }

    private void addHistoricalComparison(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "6. COMPARACI\u00d3N HIST\u00d3RICA POR PERIODOS");

        List<ModalityTypeComparisonReportDTO.PeriodComparisonDTO> periods = report.getHistoricalComparison();

        PdfPTable compTable = new PdfPTable(periods.size() + 1);
        compTable.setWidthPercentage(100);
        compTable.setSpacingAfter(20f);

        InstitutionalPdfHeader.addTableHeader(compTable, "Tipo de Modalidad");
        for (ModalityTypeComparisonReportDTO.PeriodComparisonDTO p : periods) {
            InstitutionalPdfHeader.addTableHeader(compTable, p.getPeriodLabel());
        }

        for (String typeName : report.getStudentDistributionByType().keySet()) {
            PdfPCell typeCell = new PdfPCell(new Phrase(typeName, InstitutionalPdfHeader.SMALL_FONT));
            typeCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
            typeCell.setPadding(5f);
            compTable.addCell(typeCell);

            for (ModalityTypeComparisonReportDTO.PeriodComparisonDTO p : periods) {
                int cnt = p.getModalitiesByType().getOrDefault(typeName, 0);
                int stu = p.getStudentsByType().getOrDefault(typeName, 0);
                Phrase ph = new Phrase();
                ph.add(new Chunk(cnt + " modalidades\n",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
                ph.add(new Chunk(stu + " estudiantes",
                        FontFactory.getFont(FontFactory.HELVETICA, 8, InstitutionalPdfHeader.TEXT_GRAY)));
                PdfPCell dc = new PdfPCell(ph);
                dc.setHorizontalAlignment(Element.ALIGN_CENTER);
                dc.setPadding(5f);
                compTable.addCell(dc);
            }
        }

        PdfPCell totalLbl = new PdfPCell(new Phrase("TOTALES", InstitutionalPdfHeader.TABLE_HEADER_FONT));
        totalLbl.setBackgroundColor(InstitutionalPdfHeader.INST_RED);
        totalLbl.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalLbl.setPadding(5f);
        compTable.addCell(totalLbl);

        for (ModalityTypeComparisonReportDTO.PeriodComparisonDTO p : periods) {
            Phrase ph = new Phrase();
            ph.add(new Chunk(p.getTotalModalitiesInPeriod() + " modalidades\n",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, InstitutionalPdfHeader.WHITE)));
            ph.add(new Chunk(p.getTotalStudentsInPeriod() + " estudiantes",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, InstitutionalPdfHeader.WHITE)));
            PdfPCell tc = new PdfPCell(ph);
            tc.setBackgroundColor(InstitutionalPdfHeader.INST_RED);
            tc.setHorizontalAlignment(Element.ALIGN_CENTER);
            tc.setPadding(5f);
            compTable.addCell(tc);
        }

        document.add(compTable);
    }

    private void addTrendsAnalysis(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "7. AN\u00c1LISIS DE TENDENCIAS");

        ModalityTypeComparisonReportDTO.TrendsAnalysisDTO trends = report.getTrendsAnalysis();

        BaseColor tColor; String tIcon;
        switch (trends.getOverallTrend()) {
            case "GROWING"   -> { tColor = InstitutionalPdfHeader.INST_GOLD; tIcon = "\u2197"; }
            case "DECLINING" -> { tColor = InstitutionalPdfHeader.INST_RED;  tIcon = "\u2198"; }
            default          -> { tColor = InstitutionalPdfHeader.INST_RED;  tIcon = "\u2192"; }
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
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, InstitutionalPdfHeader.WHITE)));
        tP.add(new Chunk(getTrendLabel(trends.getOverallTrend()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, InstitutionalPdfHeader.WHITE)));
        trendCell.addElement(tP);
        overallBox.addCell(trendCell);
        document.add(overallBox);

        if (trends.getGrowingTypes()  != null && !trends.getGrowingTypes().isEmpty()) {
            addTrendSection(document, "\u2197 TIPOS EN CRECIMIENTO",
                    trends.getGrowingTypes(), trends.getGrowthRateByType(), InstitutionalPdfHeader.INST_GOLD);
        }
        if (trends.getDecliningTypes() != null && !trends.getDecliningTypes().isEmpty()) {
            addTrendSection(document, "\u2198 TIPOS EN DECLIVE",
                    trends.getDecliningTypes(), trends.getGrowthRateByType(), InstitutionalPdfHeader.INST_RED);
        }
        if (trends.getStableTypes() != null && !trends.getStableTypes().isEmpty()) {
            addTrendSection(document, "\u2192 TIPOS ESTABLES",
                    trends.getStableTypes(), trends.getGrowthRateByType(), InstitutionalPdfHeader.TEXT_GRAY);
        }

        if (trends.getMostImprovedType() != null) {
            Double rate = trends.getGrowthRateByType().get(trends.getMostImprovedType());
            addHighlightBox(document,
                    "Mayor Mejora: " + trends.getMostImprovedType()
                            + " (+" + String.format("%.2f", rate) + "%)",
                    InstitutionalPdfHeader.LIGHT_GOLD, InstitutionalPdfHeader.INST_RED, 1f);
        }

        if (trends.getMostDeclinedType() != null) {
            Double rate = trends.getGrowthRateByType().get(trends.getMostDeclinedType());
            addHighlightBox(document,
                    "Mayor Declive: " + trends.getMostDeclinedType()
                            + " (" + String.format("%.2f", rate) + "%)",
                    new BaseColor(255, 230, 230), InstitutionalPdfHeader.INST_RED, 1f);
        }
    }

    private void addConclusions(Document document, ModalityTypeComparisonReportDTO report)
            throws DocumentException {

        InstitutionalPdfHeader.addSectionTitle(document, "8. CONCLUSIONES Y RECOMENDACIONES");

        List<String> conclusions = generateConclusions(report);
        for (int i = 0; i < conclusions.size(); i++) {
            Paragraph p = new Paragraph((i + 1) + ". " + conclusions.get(i), InstitutionalPdfHeader.NORMAL_FONT);
            p.setSpacingAfter(10f);
            p.setIndentationLeft(20f);
            document.add(p);
        }
    }

    // =========================================================================
    //  HELPERS VISUALES
    // =========================================================================

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

    private void addMetricCard(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell card = new PdfPCell();
        card.setPadding(14f);
        card.setBorderColor(color);
        card.setBorderWidth(1.5f);
        card.setBackgroundColor(InstitutionalPdfHeader.WHITE);

        Paragraph valP = new Paragraph(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, color));
        valP.setAlignment(Element.ALIGN_CENTER);
        valP.setSpacingAfter(4f);
        card.addElement(valP);

        Paragraph lblP = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA, 9, InstitutionalPdfHeader.TEXT_GRAY));
        lblP.setAlignment(Element.ALIGN_CENTER);
        card.addElement(lblP);
        table.addCell(card);
    }

    private void addWideMetricCard(PdfPTable table, String label, String value,
            BaseColor bgColor, BaseColor valueColor) {
        PdfPCell card = new PdfPCell();
        card.setPadding(12f);
        card.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        card.setBorderWidth(1.5f);
        card.setBackgroundColor(bgColor);

        Paragraph lblP = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA, 9, InstitutionalPdfHeader.TEXT_GRAY));
        lblP.setAlignment(Element.ALIGN_CENTER);
        lblP.setSpacingAfter(5f);
        card.addElement(lblP);

        Paragraph valP = new Paragraph(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, valueColor));
        valP.setAlignment(Element.ALIGN_CENTER);
        card.addElement(valP);
        table.addCell(card);
    }

    private void addComparisonBar(Document document, String label, int value, int totalValue,
            String unit, BaseColor color) throws DocumentException {

        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(7f);

        PdfPCell mainCell = new PdfPCell();
        mainCell.setPadding(0);
        mainCell.setBorder(Rectangle.BOX);
        mainCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        mainCell.setBorderWidth(0.5f);

        PdfPTable inner = new PdfPTable(2);
        inner.setWidthPercentage(100);
        try { inner.setWidths(new float[]{30f, 70f}); } catch (DocumentException ignored) {}

        PdfPCell lblCell = new PdfPCell(new Phrase(label, InstitutionalPdfHeader.SMALL_FONT));
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
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, InstitutionalPdfHeader.WHITE)));
        filled.setBackgroundColor(color);
        filled.setBorder(Rectangle.NO_BORDER);
        filled.setPadding(5f);
        filled.setHorizontalAlignment(Element.ALIGN_CENTER);
        filled.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bar.addCell(filled);

        PdfPCell empty = new PdfPCell(new Phrase(
                String.format("%.0f%%", percentage),
                FontFactory.getFont(FontFactory.HELVETICA, 8, InstitutionalPdfHeader.TEXT_GRAY)));
        empty.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        empty.setBorder(Rectangle.NO_BORDER);
        empty.setPadding(5f);
        empty.setHorizontalAlignment(Element.ALIGN_LEFT);
        empty.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bar.addCell(empty);

        return bar;
    }

    private void addMetricRowWithBar(PdfPTable table, String label, int value, int total, String unit)
            throws DocumentException {
        PdfPCell lblCell = new PdfPCell(new Phrase(label + ":", InstitutionalPdfHeader.SMALL_FONT));
        lblCell.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        lblCell.setPadding(6f);
        lblCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(lblCell);

        PdfPCell barCell = new PdfPCell();
        barCell.setPadding(2f);
        barCell.setBorder(Rectangle.BOX);
        barCell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        float pct = total > 0 ? (float) value / total * 100 : 0;
        barCell.addElement(createProgressBarTable(pct, value + " " + unit, InstitutionalPdfHeader.INST_RED));
        table.addCell(barCell);
    }

    private void addStatusDistributionBars(Document document,
            Map<String, Integer> distribution, int total) throws DocumentException {

        List<Map.Entry<String, Integer>> sorted = distribution.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .toList();

        PdfPTable statusTable = new PdfPTable(3);
        statusTable.setWidthPercentage(95);
        statusTable.setSpacingAfter(10f);
        try { statusTable.setWidths(new int[]{40, 15, 45}); } catch (DocumentException ignored) {}

        for (Map.Entry<String, Integer> entry : sorted) {
            float pct = total > 0 ? (float) entry.getValue() / total * 100 : 0;

            PdfPCell sCell = new PdfPCell(new Phrase(entry.getKey(), InstitutionalPdfHeader.SMALL_FONT));
            sCell.setPadding(5f);
            sCell.setBorder(Rectangle.NO_BORDER);
            statusTable.addCell(sCell);

            PdfPCell cCell = new PdfPCell(new Phrase(String.valueOf(entry.getValue()), InstitutionalPdfHeader.BOLD_FONT));
            cCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cCell.setPadding(5f);
            cCell.setBorder(Rectangle.NO_BORDER);
            statusTable.addCell(cCell);

            PdfPCell bCell = new PdfPCell();
            bCell.setPadding(2f);
            bCell.setBorder(Rectangle.NO_BORDER);
            bCell.addElement(createMiniProgressBar(pct, InstitutionalPdfHeader.INST_GOLD));
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
                FontFactory.getFont(FontFactory.HELVETICA, 7, InstitutionalPdfHeader.TEXT_GRAY)));
        empty.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GRAY);
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
        cell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
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
        cell.setBorderColor(InstitutionalPdfHeader.INST_GOLD);
        cell.setBorderWidth(0.5f);

        Paragraph lblP = new Paragraph(label,
                FontFactory.getFont(FontFactory.HELVETICA, 8, InstitutionalPdfHeader.TEXT_GRAY));
        lblP.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(lblP);

        Paragraph valP = new Paragraph(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, InstitutionalPdfHeader.INST_RED));
        valP.setAlignment(Element.ALIGN_CENTER);
        valP.setSpacingBefore(2f);
        cell.addElement(valP);
        table.addCell(cell);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font valueFont) {
        PdfPCell lc = new PdfPCell(new Phrase(label, InstitutionalPdfHeader.NORMAL_FONT));
        lc.setBackgroundColor(InstitutionalPdfHeader.LIGHT_GOLD);
        lc.setPadding(8f);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setHorizontalAlignment(Element.ALIGN_CENTER);
        vc.setPadding(8f);
        table.addCell(vc);
    }

    private void addTrendSection(Document document, String title,
            List<String> types, Map<String, Double> rates, BaseColor color) throws DocumentException {

        Paragraph sTitle = new Paragraph(title, InstitutionalPdfHeader.SECTION_FONT);
        sTitle.setSpacingBefore(10f);
        sTitle.setSpacingAfter(5f);
        document.add(sTitle);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(90);
        try { t.setWidths(new int[]{70, 30}); } catch (DocumentException ignored) {}
        t.setSpacingAfter(10f);

        for (String type : types) {
            PdfPCell tc = new PdfPCell(new Phrase(type, InstitutionalPdfHeader.NORMAL_FONT));
            tc.setPadding(5f);
            t.addCell(tc);

            Double rate = rates.get(type);
            String rateStr = rate != null ? String.format("%+.2f%%", rate) : "N/A";
            PdfPCell rc = new PdfPCell(new Phrase(rateStr, InstitutionalPdfHeader.BOLD_FONT));
            rc.setBackgroundColor(color);
            rc.setHorizontalAlignment(Element.ALIGN_CENTER);
            rc.setPadding(5f);
            t.addCell(rc);
        }
        document.add(t);
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
            conclusions.add("El tipo de modalidad m\u00e1s popular es \""
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
                            + trends.getGrowingTypes().size() + " tipos de modalidad en expansi\u00f3n.");
                case "DECLINING" ->
                    conclusions.add("Se observa una tendencia general de declive, "
                            + "sugiriendo la necesidad de revisar la oferta acad\u00e9mica.");
                default ->
                    conclusions.add("El programa muestra una tendencia estable en la distribuci\u00f3n de tipos de modalidad.");
            }
        }

        conclusions.add("Se recomienda continuar monitoreando las preferencias estudiantiles "
                + "y ajustar la oferta de modalidades seg\u00fan la demanda observada.");

        return conclusions;
    }
}
