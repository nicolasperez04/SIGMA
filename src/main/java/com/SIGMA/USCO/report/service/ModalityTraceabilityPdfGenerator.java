package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.report.dto.ModalityTraceabilityReportDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generador de PDF institucional para el reporte de trazabilidad completa de una modalidad.
 * Diseñado para que el Comité de Currículo tenga visibilidad total en tiempo real.
 */
@Service
public class ModalityTraceabilityPdfGenerator {

    // ── Paleta institucional ──────────────────────────────────────────────────
    private static final BaseColor INST_RED       = new BaseColor(143, 30, 30);
    private static final BaseColor INST_GOLD      = new BaseColor(213, 203, 160);
    private static final BaseColor LIGHT_GOLD     = new BaseColor(245, 242, 235);
    private static final BaseColor ROW_ALT        = new BaseColor(238, 235, 228);
    private static final BaseColor WHITE          = BaseColor.WHITE;
    private static final BaseColor TEXT_BLACK     = BaseColor.BLACK;
    private static final BaseColor TEXT_GRAY      = new BaseColor(80, 80, 80);
    private static final BaseColor TEXT_GRAY_LIGHT = new BaseColor(130, 130, 130);
    private static final BaseColor GREEN_DARK     = new BaseColor(30, 100, 30);
    private static final BaseColor RED_SOFT       = new BaseColor(180, 40, 40);
    private static final BaseColor AMBER          = new BaseColor(180, 120, 20);
    private static final BaseColor BLUE_DARK      = new BaseColor(30, 60, 130);

    // ── Fuentes ───────────────────────────────────────────────────────────────
    private static final Font FONT_REPORT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f, INST_RED);
    private static final Font FONT_SECTION      = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, WHITE);
    private static final Font FONT_SUBSECTION   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, INST_RED);
    private static final Font FONT_LABEL        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, TEXT_GRAY);
    private static final Font FONT_VALUE        = FontFactory.getFont(FontFactory.HELVETICA, 8f, TEXT_BLACK);
    private static final Font FONT_VALUE_GREEN  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, GREEN_DARK);
    private static final Font FONT_VALUE_RED    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, RED_SOFT);
    private static final Font FONT_VALUE_AMBER  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, AMBER);
    private static final Font FONT_TABLE_HDR    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, WHITE);
    private static final Font FONT_TABLE_CELL   = FontFactory.getFont(FontFactory.HELVETICA, 8f, TEXT_BLACK);
    private static final Font FONT_FOOTER       = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7f, TEXT_GRAY_LIGHT);
    private static final Font FONT_COVER_TITLE  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f, INST_RED);
    private static final Font FONT_COVER_SUB    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, TEXT_GRAY);
    private static final Font FONT_COVER_SMALL  = FontFactory.getFont(FontFactory.HELVETICA, 9f, TEXT_GRAY_LIGHT);
    private static final Font FONT_UNIV         = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, INST_RED);
    private static final Font FONT_FAC          = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, TEXT_GRAY);
    private static final Font FONT_PROG         = FontFactory.getFont(FontFactory.HELVETICA, 10f, TEXT_GRAY);
    private static final Font FONT_SLOGAN       = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8f, TEXT_GRAY_LIGHT);
    private static final Font FONT_BADGE        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, WHITE);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_SHORT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─────────────────────────────────────────────────────────────────────────
    // Punto de entrada principal
    // ─────────────────────────────────────────────────────────────────────────

    public byte[] generatePdf(ModalityTraceabilityReportDTO report)
            throws DocumentException, IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 45, 45, 40, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, out);

        // Pie de página en cada hoja
        writer.setPageEvent(new FooterEvent(report));

        doc.open();

        // ── 1. Portada ─────────────────────────────────────────────────────
        addCoverPage(doc, report);

        // ── 2. Resumen ejecutivo ───────────────────────────────────────────
        doc.newPage();
        addInstitutionalHeader(doc, report);
        addSummarySection(doc, report);

        // ── 3. Información general de la modalidad ─────────────────────────
        doc.newPage();
        addInstitutionalHeader(doc, report);
        addGeneralInfoSection(doc, report);

        // ── 4. Integrantes ─────────────────────────────────────────────────
        addInstitutionalHeader(doc, report);
        addMembersSection(doc, report);

        // ── 5. Director y Jurados ──────────────────────────────────────────
        addDirectorAndExaminersSection(doc, report);

        // ── 6. Documentos ─────────────────────────────────────────────────
        doc.newPage();
        addInstitutionalHeader(doc, report);
        addDocumentsSection(doc, report);

        // ── 7. Historial de trazabilidad ───────────────────────────────────
        doc.newPage();
        addInstitutionalHeader(doc, report);
        addStatusHistorySection(doc, report);

        // ── 8. Sustentación y resultado final ──────────────────────────────
        addDefenseAndResultSection(doc, report);

        doc.close();
        return out.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. PORTADA
    // ─────────────────────────────────────────────────────────────────────────

    private void addCoverPage(Document doc, ModalityTraceabilityReportDTO r)
            throws DocumentException, IOException {

        // Banda roja superior
        PdfPTable topBand = new PdfPTable(1);
        topBand.setWidthPercentage(100);
        PdfPCell topCell = new PdfPCell();
        topCell.setBackgroundColor(INST_RED);
        topCell.setFixedHeight(12f);
        topCell.setBorder(Rectangle.NO_BORDER);
        topBand.addCell(topCell);
        doc.add(topBand);
        addSpacing(doc, 30f);

        // Encabezado institucional en portada
        addInstitutionalHeader(doc, r);

        addSpacing(doc, 30f);
        addGoldLine(doc);
        addSpacing(doc, 20f);

        // Título principal
        Paragraph title = new Paragraph("REPORTE DE LA MODALIDAD DEL ESTUDIANTE", FONT_COVER_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph sub = new Paragraph("Modalidad de Grado — Seguimiento Integral para Comité de Currículo", FONT_COVER_SUB);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingBefore(6f);
        doc.add(sub);

        addSpacing(doc, 20f);
        addGoldLine(doc);
        addSpacing(doc, 20f);

        // Tarjeta de datos de la modalidad en portada
        PdfPTable card = new PdfPTable(new float[]{2f, 4f});
        card.setWidthPercentage(80);
        card.setHorizontalAlignment(Element.ALIGN_CENTER);
        card.setSpacingBefore(10f);
        card.setSpacingAfter(10f);

        addCardRow(card, "ID de Modalidad:", "#" + r.getStudentModalityId(), false);
        addCardRow(card, "Modalidad:", r.getModalityName(), true);
        addCardRow(card, "Tipo de la modalidad:", r.getModalityType().equals("GROUP") ? "Grupal" : "Individual", false);
        addCardRow(card, "Programa Académico:", r.getAcademicProgramName(), true);
        addCardRow(card, "Facultad:", r.getFacultyName(), false);
        addCardRow(card, "Estado Actual:", r.getCurrentStatusLabel(), true);
        if (r.getSelectionDate() != null)
            addCardRow(card, "Fecha de Inicio:", r.getSelectionDate().format(DATE_SHORT), false);
        addCardRow(card, "Días en Proceso:", r.getTotalDaysInProcess() + " días", true);
        doc.add(card);

        addSpacing(doc, 30f);

        // Fecha de generación
        Paragraph gen = new Paragraph(
                "Reporte generado el " + (r.getGeneratedAt() != null ? r.getGeneratedAt().format(DATE_FMT) : ""),
                FONT_COVER_SMALL);
        gen.setAlignment(Element.ALIGN_CENTER);
        doc.add(gen);

        Paragraph genBy = new Paragraph(r.getGeneratedBy(), FONT_COVER_SMALL);
        genBy.setAlignment(Element.ALIGN_CENTER);
        genBy.setSpacingBefore(4f);
        doc.add(genBy);

        // Banda roja inferior
        addSpacing(doc, 20f);
        addRedLine(doc);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. ENCABEZADO INSTITUCIONAL (en cada página interior)
    // ─────────────────────────────────────────────────────────────────────────

    private void addInstitutionalHeader(Document doc, ModalityTraceabilityReportDTO r)
            throws DocumentException, IOException {

        PdfPTable headerTable = new PdfPTable(new float[]{1.5f, 5f});
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(2f);

        // Logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(2f);
        try {
            org.springframework.core.io.ClassPathResource logoRes =
                    new org.springframework.core.io.ClassPathResource("templates/logo ingenieria.png");
            try (java.io.InputStream is = logoRes.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                Image logo = Image.getInstance(bytes);
                logo.scaleToFit(80f, 60f);
                logo.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {
            logoCell.addElement(new Paragraph(" ", FONT_PROG));
        }
        headerTable.addCell(logoCell);

        // Texto institucional
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        textCell.setPaddingLeft(10f);

        Paragraph univ = new Paragraph("UNIVERSIDAD SURCOLOMBIANA", FONT_UNIV);
        univ.setSpacingAfter(2f);
        textCell.addElement(univ);

        Paragraph fac = new Paragraph(r.getFacultyName() != null ? r.getFacultyName().toUpperCase() : "", FONT_FAC);
        fac.setSpacingAfter(2f);
        textCell.addElement(fac);

        Paragraph prog = new Paragraph(r.getAcademicProgramName() != null ? r.getAcademicProgramName() : "", FONT_PROG);
        prog.setSpacingAfter(2f);
        textCell.addElement(prog);

        textCell.addElement(new Paragraph(
                "Reporte de Trazabilidad — Modalidad #" + r.getStudentModalityId(), FONT_SLOGAN));
        textCell.addElement(new Paragraph(
                "Sistema de Información y Gestión Académica — SIGMA", FONT_SLOGAN));

        headerTable.addCell(textCell);
        doc.add(headerTable);

        addRedLine(doc);
        addGoldLine(doc);
        addSpacing(doc, 6f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. RESUMEN EJECUTIVO
    // ─────────────────────────────────────────────────────────────────────────

    private void addSummarySection(Document doc, ModalityTraceabilityReportDTO r)
            throws DocumentException {

        addSectionHeader(doc, "I. RESUMEN EJECUTIVO");
        addSpacing(doc, 6f);

        ModalityTraceabilityReportDTO.TraceabilitySummaryDTO s = r.getSummary();
        if (s == null) return;

        // Tarjetas de métricas clave en 4 columnas
        PdfPTable metrics = new PdfPTable(4);
        metrics.setWidthPercentage(100);
        metrics.setSpacingBefore(4f);
        metrics.setSpacingAfter(10f);

        addMetricCard(metrics, "Días en Proceso", String.valueOf(s.getTotalDaysInProcess()), INST_RED);
        addMetricCard(metrics, "Cambios de Estado", String.valueOf(s.getTotalStatusChanges()), BLUE_DARK);
        addMetricCard(metrics, "Documentos Subidos", String.valueOf(s.getTotalDocumentsUploaded()), TEXT_GRAY);
        addMetricCard(metrics, "Docs. Aprobados", String.valueOf(s.getApprovedDocuments()), GREEN_DARK);

        doc.add(metrics);

        // Segunda fila de métricas
        PdfPTable metrics2 = new PdfPTable(4);
        metrics2.setWidthPercentage(100);
        metrics2.setSpacingAfter(10f);

        addMetricCard(metrics2, "Docs. Pendientes", String.valueOf(s.getPendingDocuments()),
                s.getPendingDocuments() > 0 ? AMBER : GREEN_DARK);
        addMetricCard(metrics2, "Docs. Rechazados", String.valueOf(s.getRejectedDocuments()),
                s.getRejectedDocuments() > 0 ? RED_SOFT : GREEN_DARK);
        addMetricCard(metrics2, "Jurados Asignados", String.valueOf(s.getTotalExaminers()),
                s.isExaminersAssigned() ? GREEN_DARK : AMBER);
        addMetricCard(metrics2, "Estado Final",
                Boolean.TRUE.equals(s.getFinalResultAvailable()) ? "Disponible" : "Pendiente",
                Boolean.TRUE.equals(s.getFinalResultAvailable()) ? GREEN_DARK : AMBER);

        doc.add(metrics2);

        // Indicadores de estado rápidos
        addSubSectionHeader(doc, "Indicadores de Estado");
        PdfPTable indicators = new PdfPTable(new float[]{3f, 1.5f, 3f, 1.5f});
        indicators.setWidthPercentage(100);
        indicators.setSpacingBefore(4f);
        indicators.setSpacingAfter(8f);

        addIndicatorRow(indicators, "Director asignado:", s.isDirectorAssigned() ? "✔ Sí" : "✘ No",
                "Sustentación completada:", s.isDefenseCompleted() ? "✔ Sí" : "✘ No",
                s.isDirectorAssigned(), s.isDefenseCompleted());

        addIndicatorRow(indicators, "Jurados asignados:", s.isExaminersAssigned() ? "✔ Sí" : "✘ No",
                "Resultado disponible:", Boolean.TRUE.equals(s.getFinalResultAvailable()) ? "✔ Sí" : "Pendiente",
                s.isExaminersAssigned(), Boolean.TRUE.equals(s.getFinalResultAvailable()));

        doc.add(indicators);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. INFORMACIÓN GENERAL
    // ─────────────────────────────────────────────────────────────────────────

    private void addGeneralInfoSection(Document doc, ModalityTraceabilityReportDTO r)
            throws DocumentException {

        addSectionHeader(doc, "II. INFORMACIÓN GENERAL DE LA MODALIDAD");
        addSpacing(doc, 6f);

        PdfPTable table = new PdfPTable(new float[]{2.5f, 4f, 2.5f, 4f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(10f);

        boolean alt = false;
        addDoubleRow(table, "ID Modalidad:", "#" + r.getStudentModalityId(),
                "Modalidad:", r.getModalityName(), alt = !alt);
        addDoubleRow(table, "Tipo de modalidad:", r.getModalityType().equals("GROUP") ? "Grupal" : "Individual",
                "Programa Académico:", r.getAcademicProgramName(), alt = !alt);
        addDoubleRow(table, "Facultad:", r.getFacultyName(),
                "Estado Actual:", r.getCurrentStatusLabel(), alt = !alt);
        addDoubleRow(table, "Fecha de Inicio:",
                r.getSelectionDate() != null ? r.getSelectionDate().format(DATE_SHORT) : "—",
                "Última Actualización:",
                r.getLastUpdated() != null ? r.getLastUpdated().format(DATE_FMT) : "—", alt = !alt);
        addDoubleRow(table, "Total Días en Proceso:", r.getTotalDaysInProcess() + " días",
                "Reporte Generado:", r.getGeneratedAt() != null ? r.getGeneratedAt().format(DATE_FMT) : "—",
                alt = !alt);

        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. INTEGRANTES
    // ─────────────────────────────────────────────────────────────────────────

    private void addMembersSection(Document doc, ModalityTraceabilityReportDTO r)
            throws DocumentException {

        addSpacing(doc, 6f);
        addSectionHeader(doc, "III. INTEGRANTES DE LA MODALIDAD");
        addSpacing(doc, 6f);

        List<ModalityTraceabilityReportDTO.MemberDetailDTO> members = r.getMembers();
        if (members == null || members.isEmpty()) {
            doc.add(infoNote("No se encontraron integrantes registrados."));
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{3f, 2f, 2f, 1.2f, 1.2f, 1.8f, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(10f);

        // Encabezados
        String[] headers = {"Nombre Completo", "Código", "Correo", "Semestre", "Promedio", "Rol", "Estado"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, FONT_TABLE_HDR));
            hCell.setBackgroundColor(INST_RED);
            hCell.setBorder(Rectangle.NO_BORDER);
            hCell.setPadding(6f);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hCell);
        }

        boolean alt = false;
        for (ModalityTraceabilityReportDTO.MemberDetailDTO m : members) {
            BaseColor bg = alt ? ROW_ALT : LIGHT_GOLD;
            alt = !alt;
            addTableCell(table, m.getFullName(), bg, false);
            addTableCell(table, nvl(m.getStudentCode()), bg, true);
            addTableCell(table, nvl(m.getEmail()), bg, false);
            addTableCell(table, m.getSemester() != null ? m.getSemester().toString() : "—", bg, true);
            addTableCell(table, m.getGpa() != null ? String.format("%.2f", m.getGpa()) : "—", bg, false);

            // Rol con badge
            PdfPCell roleCell = new PdfPCell();
            roleCell.setBorder(Rectangle.NO_BORDER);
            roleCell.setBackgroundColor(bg);
            roleCell.setPadding(5f);
            roleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            roleCell.addElement(buildBadge(
                    Boolean.TRUE.equals(m.getIsLeader()) ? "LÍDER" : "Miembro",
                    Boolean.TRUE.equals(m.getIsLeader()) ? INST_RED : BLUE_DARK));
            table.addCell(roleCell);

            addTableCell(table, nvl(m.getMemberStatus()), bg, true);
        }
        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. DIRECTOR Y JURADOS
    // ─────────────────────────────────────────────────────────────────────────

    private void addDirectorAndExaminersSection(Document doc, ModalityTraceabilityReportDTO r)
            throws DocumentException {

        addSpacing(doc, 6f);
        addSectionHeader(doc, "IV. DIRECTOR Y JURADOS");
        addSpacing(doc, 6f);

        // Director
        addSubSectionHeader(doc, "Director del Proyecto");
        ModalityTraceabilityReportDTO.DirectorDetailDTO d = r.getDirector();
        if (d == null || !Boolean.TRUE.equals(d.getAssigned())) {
            doc.add(infoNote("⚠  Director no asignado aún."));
        } else {
            PdfPTable dt = new PdfPTable(new float[]{2.5f, 4f, 2.5f, 4f});
            dt.setWidthPercentage(100);
            dt.setSpacingBefore(4f);
            dt.setSpacingAfter(8f);
            addDoubleRow(dt, "Nombre:", d.getFullName(), "Correo:", nvl(d.getEmail()), false);
            doc.add(dt);
        }

        // Jurados
        addSpacing(doc, 4f);
        addSubSectionHeader(doc, "Jurados Evaluadores");
        List<ModalityTraceabilityReportDTO.ExaminerDetailDTO> examiners = r.getExaminers();
        if (examiners == null || examiners.isEmpty()) {
            doc.add(infoNote("⚠  Jurados no asignados aún."));
            addSpacing(doc, 6f);
            return;
        }

        PdfPTable et = new PdfPTable(new float[]{3f, 2.5f, 3f, 2f});
        et.setWidthPercentage(100);
        et.setSpacingBefore(4f);
        et.setSpacingAfter(10f);

        String[] eHeaders = {"Nombre", "Tipo de Jurado", "Correo", "Fecha Asignación"};
        for (String h : eHeaders) {
            PdfPCell hc = new PdfPCell(new Phrase(h, FONT_TABLE_HDR));
            hc.setBackgroundColor(INST_RED);
            hc.setBorder(Rectangle.NO_BORDER);
            hc.setPadding(6f);
            hc.setHorizontalAlignment(Element.ALIGN_CENTER);
            et.addCell(hc);
        }

        boolean alt = false;
        for (ModalityTraceabilityReportDTO.ExaminerDetailDTO e : examiners) {
            BaseColor bg = alt ? ROW_ALT : LIGHT_GOLD;
            alt = !alt;
            addTableCell(et, nvl(e.getFullName()), bg, false);
            addTableCell(et, nvl(e.getExaminerTypeLabel()), bg, true);
            addTableCell(et, nvl(e.getEmail()), bg, false);
            addTableCell(et, e.getAssignmentDate() != null ? e.getAssignmentDate().format(DATE_SHORT) : "—", bg, true);
        }
        doc.add(et);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. DOCUMENTOS
    // ─────────────────────────────────────────────────────────────────────────

    private void addDocumentsSection(Document doc, ModalityTraceabilityReportDTO r)
            throws DocumentException {

        addSectionHeader(doc, "V. DOCUMENTOS SUBIDOS");
        addSpacing(doc, 6f);

        List<ModalityTraceabilityReportDTO.DocumentDetailDTO> docs = r.getDocuments();
        if (docs == null || docs.isEmpty()) {
            doc.add(infoNote("No se han subido documentos aún."));
            return;
        }

        // Documentos OBLIGATORIOS
        List<ModalityTraceabilityReportDTO.DocumentDetailDTO> mandatory = docs.stream()
                .filter(d -> "MANDATORY".equals(d.getDocumentType()))
                .toList();
        List<ModalityTraceabilityReportDTO.DocumentDetailDTO> secondary = docs.stream()
                .filter(d -> "SECONDARY".equals(d.getDocumentType()))
                .toList();

        if (!mandatory.isEmpty()) {
            addSubSectionHeader(doc, "Documentos Iniciales");
            addSpacing(doc, 4f);
            addDocumentTable(doc, mandatory);
        }
        if (!secondary.isEmpty()) {
            addSpacing(doc, 6f);
            addSubSectionHeader(doc, "Documentos Secundarios");
            addSpacing(doc, 4f);
            addDocumentTable(doc, secondary);
        }
    }

    private void addDocumentTable(Document doc,
                                  List<ModalityTraceabilityReportDTO.DocumentDetailDTO> docs)
            throws DocumentException {

        PdfPTable table = new PdfPTable(new float[]{3f, 2f, 2.5f, 2f, 2.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(8f);

        String[] headers = {"Nombre del Documento", "Tipo", "Estado", "Fecha Subida", "Notas"};
        for (String h : headers) {
            PdfPCell hc = new PdfPCell(new Phrase(h, FONT_TABLE_HDR));
            hc.setBackgroundColor(INST_RED);
            hc.setBorder(Rectangle.NO_BORDER);
            hc.setPadding(6f);
            hc.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hc);
        }

        boolean alt = false;
        for (ModalityTraceabilityReportDTO.DocumentDetailDTO d : docs) {
            BaseColor bg = alt ? ROW_ALT : LIGHT_GOLD;
            alt = !alt;

            addTableCell(table, nvl(d.getDocumentName()), bg, false);
            addTableCell(table, nvl(d.getDocumentTypeLabel()), bg, true);

            // Estado con color
            PdfPCell statusCell = new PdfPCell();
            statusCell.setBorder(Rectangle.NO_BORDER);
            statusCell.setBackgroundColor(bg);
            statusCell.setPadding(5f);
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Font statusFont = getDocumentStatusFont(d.getCurrentStatus());
            statusCell.addElement(new Phrase(nvl(d.getCurrentStatusLabel()), statusFont));
            table.addCell(statusCell);

            addTableCell(table, d.getUploadDate() != null ? d.getUploadDate().format(DATE_SHORT) : "—", bg, false);
            addTableCell(table, d.getNotes() != null ? truncate(d.getNotes(), 60) : "—", bg, true);
        }
        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. HISTORIAL DE TRAZABILIDAD
    // ─────────────────────────────────────────────────────────────────────────

    private void addStatusHistorySection(Document doc, ModalityTraceabilityReportDTO r)
            throws DocumentException {

        addSectionHeader(doc, "VI. HISTORIAL COMPLETO DE TRAZABILIDAD");
        addSpacing(doc, 6f);

        List<ModalityTraceabilityReportDTO.StatusHistoryEntryDTO> history = r.getStatusHistory();
        if (history == null || history.isEmpty()) {
            doc.add(infoNote("Sin historial de estados registrado."));
            return;
        }

        Paragraph intro = new Paragraph(
                "A continuación se presenta el historial cronológico completo de todos los cambios de " +
                "estado de la modalidad, incluyendo el responsable de cada transición y las observaciones registradas.",
                FONT_TABLE_CELL);
        intro.setAlignment(Element.ALIGN_JUSTIFIED);
        intro.setLeading(0f, 1.4f);
        intro.setSpacingAfter(8f);
        doc.add(intro);

        PdfPTable table = new PdfPTable(new float[]{0.5f, 2f, 2.5f, 2f, 1f, 3.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(10f);

        String[] headers = {"#", "Fecha", "Estado", "Responsable", "Días", "Observaciones"};
        for (String h : headers) {
            PdfPCell hc = new PdfPCell(new Phrase(h, FONT_TABLE_HDR));
            hc.setBackgroundColor(INST_RED);
            hc.setBorder(Rectangle.NO_BORDER);
            hc.setPadding(6f);
            hc.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hc);
        }

        boolean alt = false;
        int idx = 1;
        for (ModalityTraceabilityReportDTO.StatusHistoryEntryDTO entry : history) {
            BaseColor bg = alt ? ROW_ALT : LIGHT_GOLD;
            alt = !alt;

            addTableCell(table, String.valueOf(idx++), bg, true);
            addTableCell(table,
                    entry.getChangeDate() != null ? entry.getChangeDate().format(DATE_FMT) : "—", bg, false);
            addTableCell(table, nvl(entry.getStatusLabel()), bg, false);
            addTableCell(table, nvl(entry.getResponsibleName()), bg, false);
            addTableCell(table, entry.getDaysInThisStatus() != null
                    ? entry.getDaysInThisStatus() + "d" : "—", bg, true);
            addTableCell(table,
                    entry.getObservations() != null ? truncate(entry.getObservations(), 120) : "—", bg, false);
        }
        doc.add(table);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. SUSTENTACIÓN Y RESULTADO FINAL
    // ─────────────────────────────────────────────────────────────────────────

    private void addDefenseAndResultSection(Document doc, ModalityTraceabilityReportDTO r)
            throws DocumentException {

        addSpacing(doc, 6f);
        addSectionHeader(doc, "VII. SUSTENTACIÓN Y RESULTADO FINAL");
        addSpacing(doc, 6f);

        // Sustentación
        addSubSectionHeader(doc, "Información de Sustentación");
        ModalityTraceabilityReportDTO.DefenseInfoDTO def = r.getDefenseInfo();

        PdfPTable defTable = new PdfPTable(new float[]{2.5f, 4f, 2.5f, 4f});
        defTable.setWidthPercentage(100);
        defTable.setSpacingBefore(4f);
        defTable.setSpacingAfter(8f);

        addDoubleRow(defTable, "Fecha de Sustentación:",
                def != null && def.getDefenseDate() != null ? def.getDefenseDate().format(DATE_FMT) : "No programada",
                "Lugar de Sustentación:",
                def != null && def.getDefenseLocation() != null ? def.getDefenseLocation() : "No especificado",
                false);
        addDoubleRow(defTable, "¿Programada?:",
                def != null && Boolean.TRUE.equals(def.getDefenseScheduled()) ? "✔ Sí" : "✘ No",
                "¿Completada?:",
                def != null && Boolean.TRUE.equals(def.getDefenseCompleted()) ? "✔ Sí" : "✘ No",
                true);
        doc.add(defTable);

        // Resultado final
        addSpacing(doc, 4f);
        addSubSectionHeader(doc, "Resultado Final de la Evaluación");
        ModalityTraceabilityReportDTO.FinalResultDTO res = r.getFinalResult();

        if (res == null || !Boolean.TRUE.equals(res.getHasResult())) {
            doc.add(infoNote("⏳  El resultado final aún no está disponible. La modalidad continúa en proceso."));
            addSpacing(doc, 6f);
            return;
        }

        PdfPTable resTable = new PdfPTable(new float[]{2.5f, 4f, 2.5f, 4f});
        resTable.setWidthPercentage(100);
        resTable.setSpacingBefore(4f);
        resTable.setSpacingAfter(8f);

        // Calificación
        boolean approved = Boolean.TRUE.equals(res.getApproved());
        String gradeStr = res.getFinalGrade() != null
                ? String.format("%.1f / 5.0", res.getFinalGrade()) : "No registrada";

        addDoubleRow(resTable, "Calificación Final:", gradeStr,
                "Distinción Académica:", nvl(res.getAcademicDistinctionLabel()), false);
        // Estado Distinción: mostrar SIEMPRE en español
        addDoubleRow(resTable, "Resultado:", approved ? "✔ APROBADO" : "✘ NO APROBADO",
                "Estado Distinción:", nvl(res.getAcademicDistinctionLabel()), true);
        doc.add(resTable);

        // Recuadro de resultado destacado
        PdfPTable highlight = new PdfPTable(1);
        highlight.setWidthPercentage(60);
        highlight.setHorizontalAlignment(Element.ALIGN_CENTER);
        highlight.setSpacingBefore(8f);
        highlight.setSpacingAfter(10f);
        PdfPCell hCell = new PdfPCell();
        // Usar colores institucionales: rojo para aprobado, dorado para no aprobado
        hCell.setBackgroundColor(approved ? INST_RED : INST_GOLD);
        hCell.setBorder(Rectangle.NO_BORDER);
        hCell.setPadding(12f);
        hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Font bigResultFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, WHITE);
        hCell.addElement(new Phrase(approved
                ? "MODALIDAD APROBADA — " + nvl(res.getAcademicDistinctionLabel())
                : "MODALIDAD NO APROBADA", bigResultFont));
        highlight.addCell(hCell);
        doc.add(highlight);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de construcción de celdas y tablas
    // ─────────────────────────────────────────────────────────────────────────

    private void addSectionHeader(Document doc, String title) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4f);
        t.setSpacingAfter(0f);
        PdfPCell cell = new PdfPCell(new Phrase(title, FONT_SECTION));
        cell.setBackgroundColor(INST_RED);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8f);
        t.addCell(cell);
        doc.add(t);
    }

    private void addSubSectionHeader(Document doc, String title) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(6f);
        t.setSpacingAfter(0f);
        PdfPCell cell = new PdfPCell(new Phrase(title, FONT_SUBSECTION));
        cell.setBorder(Rectangle.LEFT);
        cell.setBorderColorLeft(INST_RED);
        cell.setBorderWidthLeft(3f);
        cell.setBackgroundColor(LIGHT_GOLD);
        cell.setPadding(6f);
        t.addCell(cell);
        doc.add(t);
    }

    private void addDoubleRow(PdfPTable table, String lbl1, String val1,
                              String lbl2, String val2, boolean alt) {
        BaseColor bg = alt ? ROW_ALT : LIGHT_GOLD;
        PdfPCell l1 = new PdfPCell(new Phrase(lbl1, FONT_LABEL));
        l1.setBorder(Rectangle.NO_BORDER);
        l1.setBackgroundColor(bg);
        l1.setPadding(6f);
        table.addCell(l1);
        PdfPCell v1 = new PdfPCell(new Phrase(nvl(val1), FONT_VALUE));
        v1.setBorder(Rectangle.NO_BORDER);
        v1.setBackgroundColor(bg);
        v1.setPadding(6f);
        table.addCell(v1);
        PdfPCell l2 = new PdfPCell(new Phrase(lbl2, FONT_LABEL));
        l2.setBorder(Rectangle.NO_BORDER);
        l2.setBackgroundColor(bg);
        l2.setPadding(6f);
        table.addCell(l2);
        PdfPCell v2 = new PdfPCell(new Phrase(nvl(val2), FONT_VALUE));
        v2.setBorder(Rectangle.NO_BORDER);
        v2.setBackgroundColor(bg);
        v2.setPadding(6f);
        table.addCell(v2);
    }

    private void addCardRow(PdfPTable table, String label, String value, boolean alt) {
        BaseColor bg = alt ? ROW_ALT : LIGHT_GOLD;
        PdfPCell lbl = new PdfPCell(new Phrase(label, FONT_LABEL));
        lbl.setBorder(Rectangle.NO_BORDER);
        lbl.setBackgroundColor(bg);
        lbl.setPadding(7f);
        table.addCell(lbl);
        PdfPCell val = new PdfPCell(new Phrase(nvl(value), FONT_VALUE));
        val.setBorder(Rectangle.NO_BORDER);
        val.setBackgroundColor(bg);
        val.setPadding(7f);
        table.addCell(val);
    }

    private void addMetricCard(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(color);
        cell.setBorderWidth(2f);
        cell.setBackgroundColor(LIGHT_GOLD);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f, color);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 7f, TEXT_GRAY);

        Paragraph valPara = new Paragraph(value, valueFont);
        valPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valPara);

        Paragraph lblPara = new Paragraph(label, labelFont);
        lblPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(lblPara);

        table.addCell(cell);
    }

    private void addIndicatorRow(PdfPTable table,
                                  String lbl1, String val1, String lbl2, String val2,
                                  boolean ok1, boolean ok2) {
        PdfPCell l1 = new PdfPCell(new Phrase(lbl1, FONT_LABEL));
        l1.setBorder(Rectangle.NO_BORDER);
        l1.setBackgroundColor(LIGHT_GOLD);
        l1.setPadding(5f);
        table.addCell(l1);
        Font f1 = ok1 ? FONT_VALUE_GREEN : FONT_VALUE_AMBER;
        PdfPCell v1 = new PdfPCell(new Phrase(val1, f1));
        v1.setBorder(Rectangle.NO_BORDER);
        v1.setBackgroundColor(LIGHT_GOLD);
        v1.setPadding(5f);
        table.addCell(v1);
        PdfPCell l2 = new PdfPCell(new Phrase(lbl2, FONT_LABEL));
        l2.setBorder(Rectangle.NO_BORDER);
        l2.setBackgroundColor(ROW_ALT);
        l2.setPadding(5f);
        table.addCell(l2);
        Font f2 = ok2 ? FONT_VALUE_GREEN : FONT_VALUE_AMBER;
        PdfPCell v2 = new PdfPCell(new Phrase(val2, f2));
        v2.setBorder(Rectangle.NO_BORDER);
        v2.setBackgroundColor(ROW_ALT);
        v2.setPadding(5f);
        table.addCell(v2);
    }

    private void addTableCell(PdfPTable table, String value, BaseColor bg, boolean center) {
        PdfPCell cell = new PdfPCell(new Phrase(nvl(value), FONT_TABLE_CELL));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        if (center) cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private Paragraph buildBadge(String text, BaseColor color) {
        PdfPTable badge = new PdfPTable(1);
        badge.setWidthPercentage(80);
        badge.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell bc = new PdfPCell(new Phrase(text, FONT_BADGE));
        bc.setBackgroundColor(color);
        bc.setBorder(Rectangle.NO_BORDER);
        bc.setPadding(3f);
        bc.setHorizontalAlignment(Element.ALIGN_CENTER);
        badge.addCell(bc);
        return new Paragraph(text, FONT_BADGE);
    }

    private Paragraph infoNote(String message) {
        Paragraph p = new Paragraph(message, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8f, AMBER));
        p.setSpacingBefore(4f);
        p.setSpacingAfter(4f);
        return p;
    }

    private Font getDocumentStatusFont(String status) {
        if (status == null) return FONT_TABLE_CELL;
        if (status.contains("ACCEPTED") || status.contains("APPROVED")) return FONT_VALUE_GREEN;
        if (status.contains("REJECTED")) return FONT_VALUE_RED;
        if (status.contains("CORRECTION") || status.contains("PENDING")) return FONT_VALUE_AMBER;
        return FONT_TABLE_CELL;
    }

    private void addRedLine(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(INST_RED);
        c.setFixedHeight(3f);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
        doc.add(t);
    }

    private void addGoldLine(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(INST_GOLD);
        c.setFixedHeight(2f);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
        doc.add(t);
    }

    private void addSpacing(Document doc, float height) throws DocumentException {
        Paragraph sp = new Paragraph(" ");
        sp.setSpacingBefore(height / 2f);
        sp.setSpacingAfter(height / 2f);
        doc.add(sp);
    }

    private String nvl(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "—";
        return s.length() > maxLen ? s.substring(0, maxLen) + "…" : s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pie de página en cada hoja
    // ─────────────────────────────────────────────────────────────────────────

    private static class FooterEvent extends PdfPageEventHelper {
        private final ModalityTraceabilityReportDTO report;

        FooterEvent(ModalityTraceabilityReportDTO report) {
            this.report = report;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte cb = writer.getDirectContent();
                Rectangle pageSize = document.getPageSize();
                float y = document.bottomMargin() - 10f;

                // Línea dorada
                cb.setColorStroke(new BaseColor(213, 203, 160));
                cb.setLineWidth(1f);
                cb.moveTo(document.leftMargin(), y + 12f);
                cb.lineTo(pageSize.getWidth() - document.rightMargin(), y + 12f);
                cb.stroke();

                // Texto del pie
                Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7f, new BaseColor(130, 130, 130));
                String leftText = "SIGMA — Reporte de Trazabilidad | Modalidad #"
                        + (report.getStudentModalityId() != null ? report.getStudentModalityId() : "")
                        + " | " + report.getAcademicProgramName();
                String rightText = "Página " + writer.getPageNumber()
                        + " | Universidad Surcolombiana";

                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                        new Phrase(leftText, footerFont),
                        document.leftMargin(), y, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                        new Phrase(rightText, footerFont),
                        pageSize.getWidth() - document.rightMargin(), y, 0);
            } catch (Exception ignored) {}
        }
    }
}

