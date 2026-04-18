package com.SIGMA.USCO.documents.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas del Servicio de Extracción de Títulos de PDF")
class PdfTitleExtractorServiceTest {

    private PdfTitleExtractorService service;

    @BeforeEach
    void setUp() {
        service = new PdfTitleExtractorService();
    }

    @Test
    @DisplayName("Debe retornar null para archivo inexistente")
    void testNonExistentFile() {
        String result = service.extractProjectTitle("/ruta/inexistente/archivo.pdf");
        assertNull(result);
    }

    @Test
    @DisplayName("Debe manejar archivos null gracefully")
    void testNullFilePath() {
        String result = service.extractProjectTitle(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Debe manejar archivos vacíos")
    void testEmptyFile() {
        String result = service.extractProjectTitle("");
        assertNull(result);
    }

    /*
     * Nota: Para pruebas más completas, necesitaríamos:
     * 1. Crear archivos PDF de prueba con diferentes formatos
     * 2. Usar una librería de prueba de PDF o generar PDFs en memoria
     * 3. Probar casos reales con diferentes tipos de documentos
     * 
     * Las pruebas anteriores validan el manejo de errores.
     * Las pruebas de integración real requieren archivos PDF físicos.
     */

}

