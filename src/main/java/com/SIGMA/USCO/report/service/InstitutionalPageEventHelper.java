package com.SIGMA.USCO.report.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.*;

/**
 * Page event helper institucional para pie de página en reportes PDF.
 * <p>
 * Dibuja en cada página:
 * <ul>
 *   <li>Línea dorada superior del pie</li>
 *   <li>"SIGMA — Universidad Surcolombiana" (izquierda)</li>
 *   <li>Nombre del programa / centro (centro)</li>
 *   <li>"Pág. X" (derecha)</li>
 * </ul>
 */
public class InstitutionalPageEventHelper extends PdfPageEventHelper {

    private final String programName;
    private final String footerCenterText;

    public InstitutionalPageEventHelper(String programName) {
        this(programName, programName);
    }

    public InstitutionalPageEventHelper(String programName, String footerCenterText) {
        this.programName = programName;
        this.footerCenterText = footerCenterText;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();

        float left   = document.leftMargin();
        float right  = document.right();
        float bottom = document.bottom() - 15f;

        cb.saveState();
        cb.setLineWidth(1f);
        cb.setColorStroke(InstitutionalPdfHeader.INST_GOLD);
        cb.moveTo(left, bottom + 10f);
        cb.lineTo(right, bottom + 10f);
        cb.stroke();
        cb.restoreState();

        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase("SIGMA \u2014 Universidad Surcolombiana",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, InstitutionalPdfHeader.INST_RED)),
                left, bottom, 0);

        String centerText = footerCenterText != null ? footerCenterText
                : (programName != null ? programName : "");
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase(centerText,
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, InstitutionalPdfHeader.TEXT_GRAY)),
                (left + right) / 2f, bottom, 0);

        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                new Phrase("P\u00e1g. " + writer.getPageNumber(),
                        FontFactory.getFont(FontFactory.HELVETICA, 8, InstitutionalPdfHeader.TEXT_GRAY)),
                right, bottom, 0);
    }
}
