package com.SIGMA.USCO.report.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    public static final BaseColor INST_RED     = new BaseColor(143, 30, 30);
    public static final BaseColor INST_GOLD    = new BaseColor(213, 203, 160);
    public static final BaseColor LIGHT_GOLD   = new BaseColor(245, 242, 235);
    public static final BaseColor TEXT_BLACK   = BaseColor.BLACK;
    public static final BaseColor TEXT_GRAY    = new BaseColor(80, 80, 80);
    public static final BaseColor WHITE        = BaseColor.WHITE;
    public static final BaseColor LIGHT_GRAY   = new BaseColor(240, 240, 240);

    // ── Fuentes compartidas ──────────────────────────────────────────────────
    public static final Font FONT_UNIV        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, INST_RED);
    public static final Font FONT_FAC         = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, new BaseColor(60, 60, 60));
    public static final Font FONT_PROG        = FontFactory.getFont(FontFactory.HELVETICA,       10f, new BaseColor(60, 60, 60));
    public static final Font FONT_SLOGAN      = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8f, new BaseColor(110, 110, 110));

    public static final Font TITLE_FONT       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, INST_RED);
    public static final Font SECTION_FONT     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, INST_RED);
    public static final Font BOLD_FONT        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_BLACK);
    public static final Font NORMAL_FONT      = FontFactory.getFont(FontFactory.HELVETICA,       10, TEXT_BLACK);
    public static final Font SMALL_FONT       = FontFactory.getFont(FontFactory.HELVETICA,        9, TEXT_GRAY);
    public static final Font TINY_FONT        = FontFactory.getFont(FontFactory.HELVETICA,        8, TEXT_GRAY);
    public static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, WHITE);
    public static final Font INFO_LABEL_FONT  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, TEXT_GRAY);
    public static final Font INFO_VALUE_FONT  = FontFactory.getFont(FontFactory.HELVETICA,         9, TEXT_BLACK);
    public static final Font COVER_LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, TEXT_GRAY);
    public static final Font COVER_VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA,        10, TEXT_BLACK);
    public static final Font FOOTER_FONT      = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY);

    // ── Formatos de fecha ─────────────────────────────────────────────────────
    public static final DateTimeFormatter DATE_FULL    = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter DATE_SHORT   = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private InstitutionalPdfHeader() {
        // Clase utilitaria - no instanciar
    }

    /**
     * Agrega el encabezado institucional completo al documento.
     * Incluye: logo + membrete + línea roja + línea dorada.
     */
    public static void addHeader(Document document,
                                  String facultyName,
                                  String programName,
                                  String reportSubtitle)
            throws DocumentException {

        PdfPTable headerTable = new PdfPTable(new float[]{1.5f, 5f});
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(4f);

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

        addRedLine(document);
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

    // ── Líneas institucionales ────────────────────────────────────────────────

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

    // ── Espaciado ─────────────────────────────────────────────────────────────

    public static void addSpacing(Document document, float height) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(height / 2f);
        spacer.setSpacingAfter(height / 2f);
        document.add(spacer);
    }

    public static void addSpacingParagraph(Document document, float height) throws DocumentException {
        addSpacing(document, height);
    }

    // ── Celda de encabezado de sección ────────────────────────────────────────

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

    // =========================================================================
    //  MÉTODOS COMPARTIDOS — Fase 0 refactorización
    // =========================================================================

    // ── Encabezado interno (páginas interiores) ───────────────────────────────

    /**
     * Encabezado compacto para páginas interiores con borde inferior.
     * Izquierda: "UNIVERSIDAD SURCOLOMBIANA — SIGMA" (rojo)
     * Derecha: texto específico del reporte (gris)
     */
    public static void addInternalHeader(Document document, String rightText)
            throws DocumentException {
        addInternalHeader(document, rightText, false);
    }

    /**
     * Versión "light": sin borde inferior en las celdas, agrega línea dorada después.
     */
    public static void addInternalHeaderLight(Document document, String rightText)
            throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setSpacingAfter(8f);
        try { header.setWidths(new float[]{65f, 35f}); } catch (DocumentException ignored) {}

        PdfPCell leftCell = new PdfPCell(new Phrase(
                "UNIVERSIDAD SURCOLOMBIANA — SIGMA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INST_RED)));
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        leftCell.setPaddingBottom(4f);
        header.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell(new Phrase(rightText,
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY)));
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        rightCell.setPaddingBottom(4f);
        header.addCell(rightCell);

        document.add(header);
        addGoldLine(document);
        addSpacing(document, 12f);
    }

    private static void addInternalHeader(Document document, String rightText, boolean unused)
            throws DocumentException {
        PdfPTable strip = new PdfPTable(2);
        strip.setWidthPercentage(100);
        strip.setSpacingAfter(8f);
        try { strip.setWidths(new float[]{65f, 35f}); } catch (DocumentException ignored) {}

        PdfPCell leftCell = new PdfPCell(new Phrase(
                "UNIVERSIDAD SURCOLOMBIANA — SIGMA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, INST_RED)));
        leftCell.setBorder(Rectangle.BOTTOM);
        leftCell.setBorderColorBottom(INST_RED);
        leftCell.setBorderWidthBottom(1.5f);
        leftCell.setPadding(4f);
        strip.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell(new Phrase(rightText,
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY)));
        rightCell.setBorder(Rectangle.BOTTOM);
        rightCell.setBorderColorBottom(INST_GOLD);
        rightCell.setBorderWidthBottom(1.5f);
        rightCell.setPadding(4f);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        strip.addCell(rightCell);

        document.add(strip);
    }

    // ── Fila de portada ───────────────────────────────────────────────────────

    /**
     * Fila label/valor para la tabla informativa de la portada.
     */
    public static void addCoverInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, COVER_LABEL_FONT));
        labelCell.setBackgroundColor(LIGHT_GOLD);
        labelCell.setPadding(8f);
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderColor(INST_GOLD);
        labelCell.setBorderWidth(0.8f);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "\u2014", COVER_VALUE_FONT));
        valueCell.setPadding(8f);
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(INST_GOLD);
        valueCell.setBorderWidth(0.8f);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(valueCell);
    }

    // ── Encabezado de tabla ───────────────────────────────────────────────────

    /**
     * Celda de encabezado de tabla: fondo rojo, texto blanco.
     */
    public static void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(INST_RED);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(INST_GOLD);
        table.addCell(cell);
    }

    // ── Título de sección ─────────────────────────────────────────────────────

    /**
     * Título de sección con línea dorada inferior.
     */
    public static void addSectionTitle(Document document, String title) throws DocumentException {
        addSectionTitle(document, title, 10f);
    }

    /**
     * Título de sección con línea dorada inferior y spacingBefore configurable.
     */
    public static void addSectionTitle(Document document, String title, float spacingBefore)
            throws DocumentException {
        Paragraph p = new Paragraph(title, SECTION_FONT);
        p.setSpacingBefore(spacingBefore);
        p.setSpacingAfter(4f);
        document.add(p);
        addGoldLine(document);
        addSpacing(document, 6f);
    }

    // ── Pie institucional ─────────────────────────────────────────────────────

    /**
     * Pie de cierre del reporte: líneas + nota + texto de cierre.
     */
    public static void addFooterSection(Document document, String noteText, String closingText)
            throws DocumentException {
        addSpacingParagraph(document, 20f);
        addRedLine(document);
        addGoldLine(document);
        addSpacingParagraph(document, 8f);

        PdfPTable noteTable = new PdfPTable(1);
        noteTable.setWidthPercentage(100);

        PdfPCell noteCell = new PdfPCell();
        noteCell.setBackgroundColor(LIGHT_GOLD);
        noteCell.setPadding(12f);
        noteCell.setBorder(Rectangle.NO_BORDER);

        Paragraph note = new Paragraph(noteText, FOOTER_FONT);
        note.setAlignment(Element.ALIGN_JUSTIFIED);
        noteCell.addElement(note);
        noteTable.addCell(noteCell);
        document.add(noteTable);

        addSpacingParagraph(document, 10f);
        Paragraph closing = new Paragraph(closingText, FOOTER_FONT);
        closing.setAlignment(Element.ALIGN_CENTER);
        document.add(closing);
    }

    // ── Barra de progreso ─────────────────────────────────────────────────────

    /**
     * Barra de progreso visual (relleno rojo/dorado según umbral).
     */
    public static PdfPCell createProgressBar(double percentage) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5f);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(INST_GOLD);

        PdfPTable barTable = new PdfPTable(2);
        barTable.setWidthPercentage(100);

        try {
            int filledWidth = Math.max(1, (int) percentage);
            int emptyWidth  = Math.max(1, 100 - filledWidth);
            barTable.setWidths(new int[]{filledWidth, emptyWidth});
        } catch (DocumentException ignored) {}

        PdfPCell filledCell = new PdfPCell();
        filledCell.setBackgroundColor(percentage > 50 ? INST_RED : INST_GOLD);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setMinimumHeight(12f);
        barTable.addCell(filledCell);

        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(LIGHT_GRAY);
        emptyCell.setBorder(Rectangle.NO_BORDER);
        emptyCell.setMinimumHeight(12f);
        barTable.addCell(emptyCell);

        cell.addElement(barTable);
        return cell;
    }

    /**
     * Barra con valor numérico dentro del relleno.
     */
    public static PdfPCell createValueBar(String text, float percentage, BaseColor color) {
        PdfPTable barContainer = new PdfPTable(2);
        float barWidth = Math.max(Math.min(percentage * 100, 100), 5);
        float emptyWidth = Math.max(100 - barWidth, 0.1f);

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

        PdfPCell filledCell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, WHITE)));
        filledCell.setBackgroundColor(color);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setPadding(4);
        filledCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        filledCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        barContainer.addCell(filledCell);

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

    // ── Fila de barra y tarjeta de métrica ───────────────────────────────────

    /**
     * Fila de gráfico de barras: etiqueta | barra | valor.
     * percentage es fracción 0-1 (misma convención que createValueBar).
     */
    public static void addBarRow(PdfPTable table, String label, String barText, String valueText,
                                 float percentage, BaseColor color) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setPadding(3);
        containerCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable innerTable = new PdfPTable(3);
        try {
            innerTable.setWidths(new float[]{1.5f, 4f, 1.5f});
        } catch (DocumentException e) {
            // Ignorar
        }

        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_BLACK)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setPadding(3);
        innerTable.addCell(labelCell);

        innerTable.addCell(createValueBar(barText, percentage, color));

        PdfPCell valueCell = new PdfPCell(new Phrase(valueText,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, color)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setPadding(3);
        innerTable.addCell(valueCell);

        containerCell.addElement(innerTable);
        table.addCell(containerCell);
    }

    /**
     * Variante de barra donde el text dentro del relleno coincide con el valor.
     */
    public static void addBarRow(PdfPTable table, String label, String text,
                                 float percentage, BaseColor color) {
        addBarRow(table, label, text, text, percentage, color);
    }

    /**
     * Tarjeta de métrica rellena con el color institucional.
     */
    public static void addMetricCard(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(color);
        cell.setPadding(10);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(60);

        Paragraph content = new Paragraph();
        content.add(new Chunk(value + "\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, WHITE)));
        content.add(new Chunk(label,
                FontFactory.getFont(FontFactory.HELVETICA, 8, LIGHT_GRAY)));
        content.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(content);
        table.addCell(cell);
    }

    // ── Portada helpers ───────────────────────────────────────────────────────

    /**
     * Caja de título roja para portada.
     * Las líneas de subtexto se muestran en dorado dentro de la caja.
     */
    public static PdfPTable createRedTitleBox(String title, List<String> boxLines) {
        PdfPTable titleBox = new PdfPTable(1);
        titleBox.setWidthPercentage(100);
        titleBox.setSpacingAfter(18);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(INST_RED);
        titleCell.setPadding(18);
        titleCell.setBorder(Rectangle.NO_BORDER);

        Paragraph titlePara = new Paragraph(title,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, WHITE));
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(titlePara);

        if (boxLines != null) {
            for (String line : boxLines) {
                if (line == null || line.isBlank()) continue;
                Paragraph subPara = new Paragraph(line,
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, INST_GOLD));
                subPara.setAlignment(Element.ALIGN_CENTER);
                subPara.setSpacingBefore(6);
                titleCell.addElement(subPara);
            }
        }

        titleBox.addCell(titleCell);
        return titleBox;
    }

    /**
     * Crea tabla informativa de 2 columnas para portada con filas label/valor.
     * Cada elemento es un arreglo String[]{label, value}.
     */
    public static PdfPTable createInfoTable(List<String[]> rows) {
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(85);
        infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        infoTable.setSpacingBefore(18);
        infoTable.setSpacingAfter(22);
        try { infoTable.setWidths(new float[]{42f, 58f}); } catch (DocumentException ignored) {}

        for (String[] row : rows) {
            addCoverInfoRow(infoTable, row[0], row.length > 1 ? row[1] : null);
        }
        return infoTable;
    }

    /**
     * Texto informativo en portada (disclaimer itálico centrado).
     */
    public static void addCoverDisclaimer(Document document, String text) throws DocumentException {
        addSpacingParagraph(document, 10f);
        Paragraph disclaimer = new Paragraph(text,
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, TEXT_GRAY));
        disclaimer.setAlignment(Element.ALIGN_JUSTIFIED);
        disclaimer.setIndentationLeft(40);
        disclaimer.setIndentationRight(40);
        document.add(disclaimer);
    }

    /**
     * Texto de cierre de portada.
     */
    public static void addCoverClosing(Document document) throws DocumentException {
        addSpacingParagraph(document, 14f);
        Paragraph closing = new Paragraph(
                "Sistema Integral de Gestión de Modalidades de Grado \u2014 SIGMA\n" +
                "Universidad Surcolombiana | Facultad de Ingeniería | Neiva \u2013 Huila",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY));
        closing.setAlignment(Element.ALIGN_CENTER);
        document.add(closing);
    }

    /**
     * Portada institucional completa para reportes SIGMA.
     * Compone: encabezado con logo, caja de título roja, tabla informativa,
     * líneas de cierre, disclaimer opcional y pie de portada.
     *
     * @param programLine    línea de programa para el encabezado (nombre + código)
     * @param reportSubtitle subtítulo del encabezado (nombre del reporte)
     * @param title          título principal dentro de la caja roja
     * @param boxLines       líneas doradas opcionales dentro de la caja roja
     * @param infoRows       filas label/valor para la tabla informativa
     * @param disclaimer     texto informativo opcional (null para omitir)
     */
    public static void addCoverPage(Document document, String programLine, String reportSubtitle,
            String title, List<String> boxLines, List<String[]> infoRows, String disclaimer)
            throws DocumentException, IOException {

        addHeader(document, "Facultad de Ingeniería", programLine, reportSubtitle);

        addSpacingParagraph(document, 10f);
        document.add(createRedTitleBox(title, boxLines));
        addGoldLine(document);
        document.add(createInfoTable(infoRows));

        addRedLine(document);
        addGoldLine(document);

        if (disclaimer != null && !disclaimer.isBlank()) {
            addCoverDisclaimer(document, disclaimer);
        }
        addCoverClosing(document);
    }

    // =========================================================================
    //  MÉTODOS COMPARTIDOS — Fase 1 refactorización
    // =========================================================================

    // ── Celda TABLE_FONT ─────────────────────────────────────────────────────

    /** Fuente para celdas de tabla (7pt, negro). */
    public static final Font TABLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_BLACK);

    // ── Título de subsección ─────────────────────────────────────────────────

    /**
     * Título de subsección sin línea dorada.
     */
    public static void addSubsectionTitle(Document document, String title) throws DocumentException {
        addSubsectionTitle(document, title, 10f, 8f);
    }

    /**
     * Título de subsección con spacing configurable.
     */
    public static void addSubsectionTitle(Document document, String title, float spacingBefore, float spacingAfter)
            throws DocumentException {
        Paragraph p = new Paragraph(title, SECTION_FONT);
        p.setSpacingBefore(spacingBefore);
        p.setSpacingAfter(spacingAfter);
        document.add(p);
    }

    // ── Filas de información ─────────────────────────────────────────────────

    /**
     * Fila label/valor sin bordes, fondo LIGHT_GOLD en label, WHITE en valor.
     */
    public static void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setPadding(8);
        labelCell.setBackgroundColor(LIGHT_GOLD);
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "\u2014", NORMAL_FONT));
        valueCell.setPadding(8);
        valueCell.setBackgroundColor(WHITE);
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }

    /**
     * Fila label/valor con borde inferior LIGHT_GOLD.
     */
    public static void addDetailRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setPadding(8);
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(LIGHT_GOLD);
        labelCell.setBorderWidth(0.5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "\u2014", NORMAL_FONT));
        valueCell.setPadding(8);
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(LIGHT_GOLD);
        valueCell.setBorderWidth(0.5f);
        table.addCell(valueCell);
    }

    /**
     * Fila estadística: label normal izquierda, valor bold centrado con LIGHT_GOLD.
     */
    public static void addStatRow(PdfPTable table, String label, String value) {
        addStatRow(table, label, value, BOLD_FONT);
    }

    /**
     * Fila estadística con fuente personalizada para el valor.
     */
    public static void addStatRow(PdfPTable table, String label, String value, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setPadding(8);
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "\u2014", valueFont));
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setBackgroundColor(LIGHT_GOLD);
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }

    // ── Celda de tabla ───────────────────────────────────────────────────────

    /**
     * Celda de datos para tabla con color alternado de fondo.
     */
    public static void addTableCell(PdfPTable table, String text, boolean alternate) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", TABLE_FONT));
        cell.setPadding(5);
        cell.setBackgroundColor(alternate ? LIGHT_GOLD : WHITE);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(LIGHT_GOLD);
        cell.setBorderWidth(0.3f);
        table.addCell(cell);
    }

    // ── Utilidades de texto ───────────────────────────────────────────────────

    /**
     * Trunca texto con elipsis si excede maxLength.
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

    /**
     * Retorna el texto o un guión si es nulo o vacío.
     */
    public static String nvl(String s) {
        return s != null && !s.isBlank() ? s : "\u2014";
    }
}
