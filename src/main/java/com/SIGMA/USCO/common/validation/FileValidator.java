package com.SIGMA.USCO.common.validation;

import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.common.util.MimeTypeGuard;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Validación unificada de archivos subidos. Mensajes canónicos: byte-idénticos
 * a los del flujo de UPLOAD de ModalityDocumentService (contrato del frontend,
 * fijado por ModalityDocumentUploadFormatTest). DocumentService se alineó a estos.
 */
public final class FileValidator {

    private FileValidator() {
    }

    public static void validateNotEmpty(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("Archivo vacío");
        }
    }

    public static void validateExtension(MultipartFile file, String allowedFormats) {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename()).toUpperCase();
        List<String> allowed = Arrays.stream(allowedFormats.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();
        if (!allowed.contains(extension)) {
            throw new ValidationException("Formato de archivo no permitido");
        }
    }

    public static void validateMime(MultipartFile file, String extension) {
        if (!MimeTypeGuard.isMimeAllowed(file, extension)) {
            throw new ValidationException("Formato de archivo no permitido");
        }
    }

    public static void validateSize(MultipartFile file, int maxSizeMb) {
        if (file.getSize() > maxSizeMb * 1024L * 1024L) {
            throw new ValidationException("El archivo supera el tamaño permitido");
        }
    }
}
