package com.SIGMA.USCO.Modalities.dto.response;

/**
 * Respuesta de uploadRequiredDocument: claves exactas del Map anterior
 * {message, path, documentStatus, modalityStatus}.
 * OJO: el Map NO incluía "success". "path" es la ruta absoluta del filesystem
 * (pendiente de decisión de negocio, el contrato se preserva en esta fase).
 */
public record UploadDocumentResponse(
        String message,
        String path,
        String documentStatus,
        String modalityStatus) {
}