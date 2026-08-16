package com.SIGMA.USCO.report.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;

/**
 * Base común de los generadores de reportes PDF.
 * Centraliza la apertura del documento y la creación de páginas con encabezado.
 * No define el flujo de generación: cada subclase conserva su propio generate().
 */
public abstract class BaseReportPdfGenerator {

    protected record PdfSession(Document document, PdfWriter writer, ByteArrayOutputStream out) {}

    /**
     * Abre el documento. Si programName no es null, registra el page event del pie
     * institucional (footerCenterText opcional; por defecto el nombre del programa).
     */
    protected PdfSession openDocument(Rectangle size, float ml, float mr, float mt, float mb,
                                      String programName, String footerCenterText)
            throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(size, ml, mr, mt, mb);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        if (programName != null) {
            writer.setPageEvent(new InstitutionalPageEventHelper(programName, footerCenterText));
        }
        document.open();
        return new PdfSession(document, writer, out);
    }

    protected void newPageWithHeader(PdfSession session, String title) throws DocumentException {
        session.document().newPage();
        InstitutionalPdfHeader.addInternalHeader(session.document(), title);
    }

    protected void newPageWithLightHeader(PdfSession session, String programName) throws DocumentException {
        session.document().newPage();
        InstitutionalPdfHeader.addInternalHeaderLight(session.document(), programName);
    }

    protected void newPageWithFullHeader(PdfSession session, String faculty, String program, String subtitle)
            throws DocumentException {
        session.document().newPage();
        InstitutionalPdfHeader.addHeader(session.document(), faculty, program, subtitle);
    }

    protected void close(PdfSession session) {
        session.document().close();
    }
}
