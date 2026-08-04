package com.SIGMA.USCO.notifications.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Soporte compartido de generación de actas/certificados PDF institucionales USCO.
 * Contiene la paleta de colores, fuentes y helpers de construcción idénticos
 * entre AcademicCertificatePdfService y ExaminerCertificatePdfService.
 */
public final class CertificatePdfSupport {

    private static final Logger log = LoggerFactory.getLogger(CertificatePdfSupport.class);

    private CertificatePdfSupport() {}

    // ── Paleta institucional ──────────────────────────────────────────────────
    public static final BaseColor INST_RED     = new BaseColor(143, 30, 30);
    public static final BaseColor INST_GOLD    = new BaseColor(180, 140, 60);
    public static final BaseColor ROW_BG_LIGHT = new BaseColor(250, 247, 242);
    public static final BaseColor ROW_BG_ALT   = new BaseColor(245, 240, 230);
    public static final BaseColor GRAY_DARK    = new BaseColor(60, 60, 60);
    public static final BaseColor GRAY_MID     = new BaseColor(110, 110, 110);

    // ── Fuentes ───────────────────────────────────────────────────────────────
    public static final Font FONT_UNIV_NAME   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   13f, INST_RED);
    public static final Font FONT_FACULTY     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11f, GRAY_DARK);
    public static final Font FONT_PROGRAM     = FontFactory.getFont(FontFactory.HELVETICA,         10f, GRAY_DARK);
    public static final Font FONT_TITLE       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   15f, INST_RED);
    public static final Font FONT_ACTA_NUM    = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,  9f, GRAY_MID);
    public static final Font FONT_SECTION_HDR = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11f, BaseColor.WHITE);
    public static final Font FONT_LABEL       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    9f, GRAY_DARK);
    public static final Font FONT_VALUE       = FontFactory.getFont(FontFactory.HELVETICA,          9f, BaseColor.BLACK);
    public static final Font FONT_BODY        = FontFactory.getFont(FontFactory.HELVETICA,          9f, GRAY_DARK);
    public static final Font FONT_FOOTER      = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,  7f, GRAY_MID);
    public static final Font FONT_SIGN_NAME   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    8f, BaseColor.BLACK);
    public static final Font FONT_SIGN_ROLE   = FontFactory.getFont(FontFactory.HELVETICA,          7f, GRAY_MID);

    // ── DateTimeFormatter en español ──────────────────────────────────────────
    public static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));
    public static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-CO"));

    /** Cabecera: logo a la izquierda + texto institucional centrado */
    public static void addInstitutionalHeader(Document doc, String facultyName, String programName)
            throws DocumentException, IOException {

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
            // Si no se encuentra el logo, la celda queda vacía
            logoCell.addElement(new Paragraph(" ", FONT_BODY));
        }

        headerTable.addCell(logoCell);

        // Celda del texto institucional
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        textCell.setPaddingLeft(10f);

        Paragraph univName = new Paragraph("UNIVERSIDAD SURCOLOMBIANA", FONT_UNIV_NAME);
        univName.setAlignment(Element.ALIGN_LEFT);
        univName.setSpacingAfter(2f);

        Paragraph fac = new Paragraph(facultyName.toUpperCase(), FONT_FACULTY);
        fac.setAlignment(Element.ALIGN_LEFT);
        fac.setSpacingAfter(2f);

        Paragraph prog = new Paragraph(programName, FONT_PROGRAM);
        prog.setAlignment(Element.ALIGN_LEFT);
        prog.setSpacingAfter(2f);

        Paragraph slogan = new Paragraph("Sistema de Información y Gestión Académica — SIGMA", FONT_SIGN_ROLE);
        slogan.setAlignment(Element.ALIGN_LEFT);

        textCell.addElement(univName);
        textCell.addElement(fac);
        textCell.addElement(prog);
        textCell.addElement(slogan);

        headerTable.addCell(textCell);
        doc.add(headerTable);
    }

    /** Fila de datos de una tabla (etiqueta + valor) */
    public static void addDataRow(PdfPTable table, String label, String value, boolean alt) {
        BaseColor bg = alt ? ROW_BG_ALT : ROW_BG_LIGHT;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(bg);
        labelCell.setPadding(7f);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", FONT_VALUE));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setBackgroundColor(bg);
        valueCell.setPadding(7f);
        table.addCell(valueCell);
    }

    /** Celda de firma (línea + nombre + rol) */
    public static PdfPCell buildSignatureCell(String name, String role) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(10f);
        cell.setPaddingBottom(6f);
        cell.setPaddingLeft(10f);
        cell.setPaddingRight(10f);

        // Línea de firma
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(80);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setFixedHeight(22f);
        lineCell.setBorder(Rectangle.BOTTOM);
        lineCell.setBorderWidthBottom(1f);
        lineCell.setBorderColorBottom(GRAY_DARK);
        lineTable.addCell(lineCell);
        cell.addElement(lineTable);

        // Nombre
        Paragraph namePara = new Paragraph(name, FONT_SIGN_NAME);
        namePara.setAlignment(Element.ALIGN_CENTER);
        namePara.setSpacingBefore(4f);
        cell.addElement(namePara);

        // Rol
        Paragraph rolePara = new Paragraph(role, FONT_SIGN_ROLE);
        rolePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(rolePara);

        return cell;
    }

    /** Encabezado de sección (banda roja con título blanco) */
    public static void addSectionHeader(Document doc, String title) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4f);
        t.setSpacingAfter(0f);
        PdfPCell cell = new PdfPCell(new Phrase(title, FONT_SECTION_HDR));
        cell.setBackgroundColor(INST_RED);
        cell.setPadding(7f);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        doc.add(t);
    }

    /** Línea roja divisora */
    public static void addRedLine(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(3f);
        cell.setBackgroundColor(INST_RED);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        doc.add(t);
    }

    /** Línea dorada divisora */
    public static void addGoldLine(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(2f);
        cell.setBackgroundColor(INST_GOLD);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        doc.add(t);
    }

    /** Espaciado vertical */
    public static void addSpacing(Document doc, float spacingPt) throws DocumentException {
        Paragraph sp = new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, spacingPt));
        sp.setLeading(spacingPt);
        doc.add(sp);
    }

    /** Pie de página con validez oficial y código de verificación */
    public static void addFooter(Document doc, String certNumber, Font verifFont) throws DocumentException {
        Paragraph footer = new Paragraph(
            "Este documento tiene validez oficial en el marco de los procesos académicos " +
            "de la Universidad Surcolombiana, conforme al Acuerdo 071 de 2023.",
            FONT_FOOTER
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingAfter(4f);
        doc.add(footer);

        Paragraph verif = new Paragraph("Código de verificación: " + certNumber, verifFont);
        verif.setAlignment(Element.ALIGN_CENTER);
        doc.add(verif);
    }

    /** Hash SHA-256 del archivo (para trazabilidad) */
    public static String calculateFileHash(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Error calculando hash del archivo: {}", e.getMessage());
            return "";
        }
    }

    /** Busca el siguiente número de acta disponible en el programa para el año */
    public static String generateCertificateNumber(String prefix, Long programId, List<String> existingNumbers) {
        int year = LocalDateTime.now().getYear();
        int nextNumber = 1;
        while (true) {
            String candidate = String.format("%s%d-%d-%04d", prefix, programId, year, nextNumber);
            if (!existingNumbers.contains(candidate)) {
                return candidate;
            }
            nextNumber++;
        }
    }

    /** Traducción del tipo de modalidad (Individual/Grupal) */
    public static String translateModalityType(String type) {
        if (type == null) return "Individual";
        return switch (type.toUpperCase()) {
            case "GROUP" -> "Grupal";
            case "INDIVIDUAL" -> "Individual";
            default -> type;
        };
    }
}
