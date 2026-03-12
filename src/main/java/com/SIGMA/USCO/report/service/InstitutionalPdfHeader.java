package com.SIGMA.USCO.report.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

/**
 * Utilidad compartida para generar el encabezado institucional USCO
 * en todos los reportes PDF del sistema SIGMA.
 *
 * Genera un encabezado con:
 *  - Logo de la universidad (izquierda)
 *  - Texto institucional: nombre, facultad, programa (derecha)
 *  - Línea roja institucional inferior
 *  - Línea dorada institucional inferior
 */
public class InstitutionalPdfHeader {

    private static final Logger log = LoggerFactory.getLogger(InstitutionalPdfHeader.class);

    // ── Paleta institucional ──────────────────────────────────────────────────
    static final BaseColor INST_RED  = new BaseColor(143, 30, 30);
    static final BaseColor INST_GOLD = new BaseColor(213, 203, 160);
    static final BaseColor LIGHT_GOLD = new BaseColor(245, 242, 235);
    static final BaseColor TEXT_BLACK = BaseColor.BLACK;
    static final BaseColor TEXT_GRAY  = new BaseColor(80, 80, 80);
    static final BaseColor WHITE      = BaseColor.WHITE;

    // ── Fuentes reutilizables ─────────────────────────────────────────────────
    static final Font FONT_UNIV   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, INST_RED);
    static final Font FONT_FAC    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, new BaseColor(60, 60, 60));
    static final Font FONT_PROG   = FontFactory.getFont(FontFactory.HELVETICA,       10f, new BaseColor(60, 60, 60));
    static final Font FONT_SLOGAN = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8f, new BaseColor(110, 110, 110));

    private InstitutionalPdfHeader() {
        // Clase utilitaria - no instanciar
    }

    /**
     * Agrega el encabezado institucional completo al documento.
     * Incluye: logo + membrete + línea roja + línea dorada.
     *
     * @param document       Documento iText abierto
     * @param facultyName    Nombre de la facultad
     * @param programName    Nombre del programa académico
     * @param reportSubtitle Subtítulo del reporte (puede ser null)
     */
    public static void addHeader(Document document,
                                  String facultyName,
                                  String programName,
                                  String reportSubtitle)
            throws DocumentException {

        // ── 1. Tabla logo + membrete ────────────────────────────────────────
        PdfPTable headerTable = new PdfPTable(new float[]{1.5f, 5f});
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(4f);

        // Celda del logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(2f);

        try {
            ClassPathResource logoResource = new ClassPathResource("templates/logo ingenieria.png");
            try (InputStream logoStream = logoResource.getInputStream()) {
                byte[] logoBytes = logoStream.readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(90f, 70f);
                logo.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo institucional: {}", e.getMessage());
            logoCell.addElement(new Paragraph(" ", FONT_PROG));
        }
        headerTable.addCell(logoCell);

        // Celda del texto institucional
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        textCell.setPaddingLeft(10f);

        Paragraph univName = new Paragraph("UNIVERSIDAD SURCOLOMBIANA", FONT_UNIV);
        univName.setSpacingAfter(2f);
        textCell.addElement(univName);

        if (facultyName != null && !facultyName.isBlank()) {
            Paragraph fac = new Paragraph(facultyName.toUpperCase(), FONT_FAC);
            fac.setSpacingAfter(2f);
            textCell.addElement(fac);
        }

        if (programName != null && !programName.isBlank()) {
            Paragraph prog = new Paragraph(programName, FONT_PROG);
            prog.setSpacingAfter(2f);
            textCell.addElement(prog);
        }

        if (reportSubtitle != null && !reportSubtitle.isBlank()) {
            Paragraph sub = new Paragraph(reportSubtitle, FONT_SLOGAN);
            sub.setSpacingAfter(1f);
            textCell.addElement(sub);
        }

        Paragraph slogan = new Paragraph("Sistema de Información y Gestión Académica — SIGMA", FONT_SLOGAN);
        textCell.addElement(slogan);

        headerTable.addCell(textCell);
        document.add(headerTable);

        // ── 2. Línea roja ───────────────────────────────────────────────────
        addRedLine(document);

        // ── 3. Línea dorada ─────────────────────────────────────────────────
        addGoldLine(document);

        addSpacing(document, 8f);
    }

    /**
     * Versión simplificada sin subtítulo de reporte.
     */
    public static void addHeader(Document document, String facultyName, String programName)
            throws DocumentException {
        addHeader(document, facultyName, programName, null);
    }

    // ── Helpers de líneas y espaciado ────────────────────────────────────────

    public static void addRedLine(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingAfter(0f);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(INST_RED);
        cell.setFixedHeight(3f);
        cell.setBorder(Rectangle.NO_BORDER);
        line.addCell(cell);
        document.add(line);
    }

    public static void addGoldLine(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingBefore(2f);
        line.setSpacingAfter(0f);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(INST_GOLD);
        cell.setFixedHeight(2f);
        cell.setBorder(Rectangle.NO_BORDER);
        line.addCell(cell);
        document.add(line);
    }

    public static void addSpacing(Document document, float height) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(height / 2f);
        spacer.setSpacingAfter(height / 2f);
        document.add(spacer);
    }

    /**
     * Celda de encabezado de sección con fondo rojo y texto blanco.
     * Útil para encabezados de tablas.
     */
    public static PdfPCell createSectionHeaderCell(String text, Font font, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(INST_RED);
        cell.setColspan(colspan);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
}

