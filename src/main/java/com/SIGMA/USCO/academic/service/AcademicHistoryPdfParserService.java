package com.SIGMA.USCO.academic.service;

import com.SIGMA.USCO.academic.dto.AcademicHistoryExtractionResult;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AcademicHistoryPdfParserService {

    private static final Logger logger = LoggerFactory.getLogger(AcademicHistoryPdfParserService.class);

    private static final int MIN_TEXT_LENGTH = 100;
    private static final Pattern PROGRAM_PATTERN = Pattern.compile(
            "(?i)programa\\s*:?\\s*(.+?)(?=registro\\s+snies\\s+programa|plan\\b|cr[eé]ditos\\s+aprobados)");

    private static final Pattern CREDITS_PATTERN = Pattern.compile(
            "(?i)cr(?:e|é)?ditos\\s+aprobados\\s*:?\\s*(\\d+)\\s*de\\s*(\\d+)");

    private static final Pattern GPA_PATTERN = Pattern.compile(
            "(?i)puntaje\\s+cali\\s*fi\\s*cado\\s+ponderado\\s*\\(\\s*con\\s+p[eé]r?didas\\s*\\)\\s*:?\\s*([0-5](?:[.,]\\d+)?)");

    private static final Pattern GPA_PATTERN_ALT_NO_PAREN = Pattern.compile(
            "(?i)puntaje\\s+cali\\s*fi\\s*cado\\s+ponderado\\s+con\\s+p[eé]r?didas\\s*:?\\s*([0-5](?:[.,]\\d+)?)");

    private static final Pattern GPA_PATTERN_ALT_RELAXED = Pattern.compile(
            "(?i)puntaje.{0,180}?([0-5](?:[.,]\\d+)?)");

    public AcademicHistoryExtractionResult extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Debe adjuntar el historial académico en PDF.");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!fileName.endsWith(".pdf")) {
            throw new ValidationException("El archivo debe estar en formato PDF.");
        }

        logger.info("Iniciando extracción de PDF: {}", fileName);
        String firstPageText = extractFirstPageText(file);
        logger.info("Texto extraído: {} caracteres", firstPageText.length());
        return extractFromText(firstPageText);
    }

    AcademicHistoryExtractionResult extractFromText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new ValidationException("No se encontró contenido legible en el documento.");
        }

        String normalized = normalizeForSearch(rawText);

        String programName = matchProgramName(normalized);
        long approvedCredits = matchLong(normalized, CREDITS_PATTERN, 1,
                "No se encontró 'Créditos aprobados' en el PDF.");
        long totalCredits = matchLong(normalized, CREDITS_PATTERN, 2,
                "No se encontró el total de créditos en el PDF.");
        double gpa = matchGpa(normalized,
                "No se encontró 'Puntaje calificado ponderado (con pérdidas)' en el PDF.");

        return AcademicHistoryExtractionResult.builder()
                .programName(programName)
                .approvedCredits(approvedCredits)
                .totalCreditsInPdf(totalCredits)
                .gpa(gpa)
                .build();
    }

    private String extractFirstPageText(MultipartFile file) {
        try {
            // 1) Intenta con iText en todo el documento
            logger.debug("Intentando extracción con iText...");
            String text = extractWithItextFallbacks(file);

            if (text != null && !text.isBlank()) {
                if (text.length() >= MIN_TEXT_LENGTH) {
                    logger.info("iText extrajo contenido suficiente ({} caracteres)", text.length());
                } else {
                    logger.info("iText extrajo contenido corto pero util ({} caracteres).", text.length());
                }
                return text;
            }

            // 2) Fallback de texto con PDFBox en todo el documento
            logger.warn("iText no extrajo texto. Intentando extracción con PDFBox...");
            text = extractWithPdfBoxText(file);
            if (text != null && !text.isBlank()) {
                logger.info("PDFBox extrajo texto ({} caracteres).", text.length());
                return text;
            }

            throw new ValidationException(
                    "No se pudo extraer texto del PDF. Sube un PDF original con texto seleccionable (no escaneado)."
            );

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error crítico en extracción de PDF: {}", e.getMessage());
            throw new ValidationException(
                    "No se pudo extraer texto del PDF. Sube un PDF original con texto seleccionable (no escaneado)."
            );
        }
    }

    private String extractWithPdfBoxText(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            if (document.getNumberOfPages() < 1) {
                return "";
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            String text = stripper.getText(document);
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            logger.warn("Error extrayendo texto con PDFBox: {}", e.getMessage());
            return "";
        }
    }

    private String extractWithItextFallbacks(MultipartFile file) throws IOException {
        try {
            PdfReader reader = new PdfReader(file.getBytes());
            if (reader.getNumberOfPages() < 1) {
                logger.warn("PDF vacío: 0 páginas");
                reader.close();
                return "";
            }

            logger.debug("PDF válido: {} páginas", reader.getNumberOfPages());
            StringBuilder textBuilder = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                String pageText = extractWithFallbackStrategies(reader, page);
                if (pageText != null && !pageText.isBlank()) {
                    textBuilder.append(pageText).append(' ');
                }
            }
            reader.close();

            return textBuilder.toString().trim();
        } catch (Exception e) {
            logger.warn("⚠️ Error en iText: {}", e.getMessage());
            return "";
        }
    }

    private String extractWithFallbackStrategies(PdfReader reader, int pageNumber) throws IOException {
        // Estrategia 1: LocationTextExtractionStrategy
        logger.debug("📍 Intentando estrategia 1: LocationTextExtractionStrategy...");
        String text = PdfTextExtractor.getTextFromPage(reader, pageNumber, new LocationTextExtractionStrategy());
        if (text != null && !text.isBlank()) {
            logger.debug("✅ Estrategia 1 exitosa: {} caracteres", text.length());
            return text;
        }

        // Estrategia 2: SimpleTextExtractionStrategy
        logger.debug("📋 Intentando estrategia 2: SimpleTextExtractionStrategy...");
        text = PdfTextExtractor.getTextFromPage(reader, pageNumber, new SimpleTextExtractionStrategy());
        if (text != null && !text.isBlank()) {
            logger.debug("✅ Estrategia 2 exitosa: {} caracteres", text.length());
            return text;
        }

        // Estrategia 3: Default
        logger.debug("🔄 Intentando estrategia 3: Default...");
        text = PdfTextExtractor.getTextFromPage(reader, pageNumber);
        if (text != null && !text.isBlank()) {
            logger.debug("✅ Estrategia 3 exitosa: {} caracteres", text.length());
            return text;
        }
        
        logger.warn("⚠️ Ninguna estrategia de iText extrajo contenido significativo");
        return "";
    }

    private String normalizeForSearch(String input) {
        return input
                .replace('\u00A0', ' ')
                .replace('|', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String matchProgramName(String text) {
        Matcher matcher = PROGRAM_PATTERN.matcher(text);
        if (!matcher.find()) {
            throw new ValidationException("No se encontró el campo 'Programa' en el PDF.");
        }
        String program = matcher.group(1).trim();
        if (program.isBlank()) {
            throw new ValidationException("El campo 'Programa' está vacío en el PDF.");
        }
        return program;
    }

    private long matchLong(String text, Pattern pattern, int group, String errorMessage) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new ValidationException(errorMessage);
        }
        try {
            return Long.parseLong(matcher.group(group));
        } catch (NumberFormatException ex) {
            throw new ValidationException(errorMessage);
        }
    }

    private double matchGpa(String text, String errorMessage) {
        Pattern[] candidates = new Pattern[]{
                GPA_PATTERN,
                GPA_PATTERN_ALT_NO_PAREN,
                GPA_PATTERN_ALT_RELAXED
        };

        for (Pattern candidate : candidates) {
            Matcher matcher = candidate.matcher(text);
            if (!matcher.find()) {
                continue;
            }
            String value = matcher.group(1).replace(',', '.');
            try {
                double parsed = Double.parseDouble(value);
                if (parsed >= 0.0 && parsed <= 5.0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // Try next pattern
            }
        }

        throw new ValidationException(errorMessage);
    }
}










