package com.SIGMA.USCO.common.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

/**
 * Capa defensiva de MIME sobre la validación de extensión. El content-type es
 * declarativo (falsificable), así que NUNCA sustituye al check de extensión:
 * solo lo refuerza. Se omite el check cuando el cliente envía octet-stream o
 * no declara content-type (no se puede verificar, se deja pasar).
 */
public final class MimeTypeGuard {

    private static final Map<String, Set<String>> EXTENSION_MIME = Map.ofEntries(
            Map.entry("PDF", Set.of("application/pdf")),
            Map.entry("DOC", Set.of("application/msword")),
            Map.entry("DOCX", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry("XLS", Set.of("application/vnd.ms-excel")),
            Map.entry("XLSX", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry("PPT", Set.of("application/vnd.ms-powerpoint")),
            Map.entry("PPTX", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
            Map.entry("PNG", Set.of("image/png")),
            Map.entry("JPG", Set.of("image/jpeg")),
            Map.entry("JPEG", Set.of("image/jpeg")),
            Map.entry("GIF", Set.of("image/gif")),
            Map.entry("ZIP", Set.of("application/zip", "application/x-zip-compressed")),
            Map.entry("RAR", Set.of("application/vnd.rar", "application/x-rar-compressed")),
            Map.entry("TXT", Set.of("text/plain"))
    );

    private MimeTypeGuard() {
    }

    /**
     * @return true si el content-type declarado es compatible con la extensión,
     * o si no se puede verificar (null / octet-stream / extensión sin mapa).
     */
    public static boolean isMimeAllowed(MultipartFile file, String extension) {
        Set<String> expected = EXTENSION_MIME.get(extension.toUpperCase());
        if (expected == null) {
            return true;
        }
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank() || contentType.contains("octet-stream")) {
            return true;
        }
        return expected.contains(contentType.toLowerCase());
    }
}
