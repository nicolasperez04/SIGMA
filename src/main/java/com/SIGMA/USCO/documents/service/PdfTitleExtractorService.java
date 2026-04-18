package com.SIGMA.USCO.documents.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PdfTitleExtractorService {

    /**
     * Extrae el título del proyecto desde un archivo PDF.
     * 
     * Estrategias de extracción:
     * 1. Busca líneas que comiencen con "Título:" o "Title:"
     * 2. Extrae la primera línea significativa después del encabezado
     * 3. Busca patrones comunes en documentos de grado
     *
     * @param filePath Ruta del archivo PDF
     * @return Título del proyecto extraído, o null si no se puede extraer
     */
    public String extractProjectTitle(String filePath) {
        try {
            File pdfFile = new File(filePath);
            if (!pdfFile.exists() || !pdfFile.canRead()) {
                log.warn("No se puede leer el archivo PDF: {}", filePath);
                return null;
            }

            PDDocument document = PDDocument.load(pdfFile);
            
            try {
                String extractedText = extractTextFromPdf(document);
                return parseProjectTitle(extractedText);
            } finally {
                document.close();
            }
            
        } catch (IOException e) {
            log.error("Error extrayendo título del PDF: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Error inesperado extrayendo título del PDF: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extrae todo el texto del PDF
     */
    private String extractTextFromPdf(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(0);
        stripper.setEndPage(1);  // Solo la primera página
        return stripper.getText(document);
    }

    /**
     * Analiza el texto extraído para encontrar el título del proyecto
     */
    private String parseProjectTitle(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String[] lines = text.split("\\n");

        // Estrategia 0: Buscar "TÍTULO DE LA..." (Práctica Profesional, Seminario, etc.)
        // En este caso, capturar MÚLTIPLES líneas hasta "OBJETIVO DE LA..." o similar
        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i].trim().toUpperCase();
            
            // Si encuentra "TÍTULO DE" (variantes de título específico)
            if (line.startsWith("TÍTULO DE") && !line.equals("TÍTULO")) {
                // Recopilar múltiples líneas hasta encontrar "OBJETIVO DE LA..." o similar
                StringBuilder titleBuilder = new StringBuilder();
                boolean foundContent = false;
                
                for (int j = i + 1; j < lines.length; j++) {
                    String currentLine = lines[j].trim();
                    String upperLine = currentLine.toUpperCase();
                    
                    // Si es una línea vacía, continuar (pero no agregar)
                    if (currentLine.isEmpty()) {
                        continue;
                    }
                    
                    // Marcadores específicos de fin para Práctica Profesional, Pasantía, Seminario, etc.
                    // Buscar: OBJETIVO DE LA PRÁCTICA, OBJETIVO DE LA PASANTÍA, OBJETIVO DE LA SEMINARIO, etc.
                    if (isEndMarkerForSpecificTitle(upperLine) && foundContent) {
                        // Encontró la siguiente sección, terminar recolección SIN incluir esta línea
                        break;
                    }
                    
                    // Si no es metadato aislado, agregar a título
                    if (!isMetadata(currentLine)) {
                        if (foundContent) {
                            titleBuilder.append(" "); // Agregar espacio entre líneas
                        }
                        titleBuilder.append(currentLine);
                        foundContent = true;
                    }
                }
                
                if (foundContent) {
                    String title = cleanupTitle(titleBuilder.toString());
                    if (!title.isEmpty()) {
                        log.debug("Título extraído usando patrón '{}' [múltiples líneas]: {}", line, title);
                        return title;
                    }
                }
            }
        }

        // Estrategia 1: PRINCIPAL - Buscar "TÍTULO" y capturar TODAS las líneas del título
        // Formato: TÍTULO\n[contenido línea 1]\n[contenido línea 2]\n...\n[próxima sección]
        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i].trim().toUpperCase();
            
            // Si la línea actual es solo "TÍTULO" (no "TÍTULO DE...")
            if (line.equals("TÍTULO") || line.equals("TITLE")) {
                // Recopilar múltiples líneas hasta encontrar la siguiente sección
                StringBuilder titleBuilder = new StringBuilder();
                boolean foundContent = false;
                
                for (int j = i + 1; j < lines.length; j++) {
                    String currentLine = lines[j].trim();
                    
                    // Si es una línea vacía, continuar
                    if (currentLine.isEmpty()) {
                        continue;
                    }
                    
                    // Patrones que indican el inicio de una nueva sección
                    String upperLine = currentLine.toUpperCase();
                    if (isSectionHeader(upperLine) && foundContent) {
                        // Encontró la siguiente sección, terminar recolección
                        break;
                    }
                    
                    // Si no es metadato aislado, agregar a título
                    if (!isMetadata(currentLine)) {
                        if (foundContent) {
                            titleBuilder.append(" "); // Agregar espacio entre líneas
                        }
                        titleBuilder.append(currentLine);
                        foundContent = true;
                    }
                }
                
                if (foundContent) {
                    String title = cleanupTitle(titleBuilder.toString());
                    if (!title.isEmpty()) {
                        log.debug("Título extraído usando patrón 'TÍTULO\\n[múltiples líneas]': {}", title);
                        return title;
                    }
                }
            }
        }

        // Estrategia 2: Buscar líneas con "Título:" o "Title:" en la misma línea
        String titlePattern2 = "(?i)títu?lo[:\\s]+([^\n]+)";
        Pattern pattern2 = Pattern.compile(titlePattern2);
        Matcher matcher2 = pattern2.matcher(text);
        
        if (matcher2.find()) {
            String title = matcher2.group(1).trim();
            if (!title.isEmpty()) {
                title = cleanupTitle(title);
                log.debug("Título extraído usando patrón 'Título:[contenido]': {}", title);
                return title;
            }
        }

        // Estrategia 3: Buscar "Project:" o similar
        String titlePattern3 = "(?i)project[:\\s]+([^\n]+)";
        Pattern pattern3 = Pattern.compile(titlePattern3);
        Matcher matcher3 = pattern3.matcher(text);
        
        if (matcher3.find()) {
            String title = matcher3.group(1).trim();
            if (!title.isEmpty()) {
                title = cleanupTitle(title);
                log.debug("Título extraído usando patrón 'Project:[contenido]': {}", title);
                return title;
            }
        }

        // Estrategia 4: Buscar la primera línea no vacía después de ciertos marcadores
        String titleCandidate = null;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Ignorar líneas vacías
            if (line.isEmpty()) {
                continue;
            }
            
            // Si encuentra "PROPUESTA", "PROYECTO", etc., el siguiente título está cerca
            if (line.toUpperCase().contains("PROPUESTA") || 
                line.toUpperCase().contains("PROYECTO") ||
                line.toUpperCase().contains("TRABAJO DE GRADO")) {
                
                // Buscar la siguiente línea significativa
                for (int j = i + 1; j < lines.length; j++) {
                    String nextLine = lines[j].trim();
                    if (!nextLine.isEmpty() && !isMetadata(nextLine)) {
                        titleCandidate = nextLine;
                        break;
                    }
                }
                
                if (titleCandidate != null) {
                    titleCandidate = cleanupTitle(titleCandidate);
                    if (!titleCandidate.isEmpty() && titleCandidate.length() < 500) {
                        log.debug("Título extraído como línea siguiente a marcador: {}", titleCandidate);
                        return titleCandidate;
                    }
                }
            }
        }

        // Estrategia 5: Si todo falla, retornar null para que se asigne manualmente
        log.debug("No se pudo extraer título automáticamente del PDF");
        return null;
    }

    /**
     * Determina si una línea es un marcador de fin específico para títulos de modalidades
     * Detecta: OBJETIVO DE LA PRÁCTICA, OBJETIVO DE LA PASANTÍA, OBJETIVO DEL SEMINARIO, etc.
     */
    private boolean isEndMarkerForSpecificTitle(String line) {
        // Buscar patrones como "OBJETIVO DE LA..." que indican el fin del título
        return line.contains("OBJETIVO DE LA") ||
               line.contains("OBJETIVO DE LA PRÁCTICA") ||
               line.contains("OBJETIVO DE LA PASANTÍA") ||
               line.contains("OBJETIVO DEL") ||
               line.contains("OBJETIVO DE") ||
               line.contains("JUSTIFICACIÓN") ||
               line.contains("ALCANCE");
    }

    /**
     * Determina si una línea es un encabezado de sección
     */
    private boolean isSectionHeader(String line) {
        // Palabras clave que indican el inicio de una nueva sección
        return line.contains("PROGRAMA") ||
               line.contains("ÁREA") ||
               line.contains("LÍNEA") ||
               line.contains("INVESTIGACIÓN") ||
               line.contains("PROPONENTES") ||
               line.contains("DIRECTOR") ||
               line.contains("CODIRECTOR") ||
               line.contains("COODIRECTOR") ||
               line.contains("INFORME") ||
               line.contains("FINANCIERO") ||
               line.contains("CÓDIGO") ||
               line.contains("CORREO") ||
               line.contains("JUSTIFICACIÓN") ||
               line.contains("OBJETIVOS") ||
               line.contains("ALCANCE") ||
               line.contains("METODOLOGÍA") ||
               line.contains("PRESUPUESTO");
    }

    /**
     * Limpia el título de caracteres no deseados
     */
    private String cleanupTitle(String title) {
        if (title == null) {
            return null;
        }
        
        // Remover caracteres de control
        title = title.replaceAll("[\\p{Cntrl}\\p{C}]", "").trim();
        
        // Remover números de página, fechas típicas al final
        title = title.replaceAll("\\s*\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}\\s*$", "").trim();
        
        // Remover saltos de línea múltiples
        title = title.replaceAll("\\s+", " ").trim();
        
        // Limitar a 500 caracteres
        if (title.length() > 500) {
            title = title.substring(0, 500).trim();
        }
        
        return title;
    }

    /**
     * Determina si una línea es metadatos (no título)
     */
    private boolean isMetadata(String line) {
        String upper = line.toUpperCase();
        
        // Patrones que indican que NO es el título
        return upper.matches(".*AUTOR.*") ||
               upper.matches(".*FECHA.*") ||
               upper.matches(".*DATE.*") ||
               upper.matches(".*PÁGINA.*") ||
               upper.matches(".*PAGE.*") ||
               upper.matches(".*UNIVERSIDAD.*") ||
               upper.matches(".*UNIVERSITY.*") ||
               upper.matches(".*\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}.*") ||  // Fechas
               upper.matches("^\\d+$") ||  // Solo números
               upper.matches(".*@.*") ||  // Emails
               line.length() < 3;  // Muy corto
    }

}


