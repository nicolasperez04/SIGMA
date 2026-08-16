package com.SIGMA.USCO.report.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Respuesta estandarizada de los endpoints JSON de reportes.
 * Las claves reportType, error, filtersApplied y studentId solo aparecen
 * cuando aplican al endpoint (JSON idéntico al Map.of() previo, clave por clave).
 */
@Schema(description = "Respuesta estandarizada de los endpoints JSON de reportes. Las claves reportType, error, filtersApplied y studentId solo aparecen cuando aplican al endpoint.")
public record ReportResponse<T>(
        @Schema(description = "Indica si la operación fue exitosa") boolean success,
        @Schema(description = "Mensaje descriptivo de la operación") String message,
        @Schema(description = "Tipo de reporte (solo en respuestas de éxito)") @JsonInclude(JsonInclude.Include.NON_NULL) String reportType,
        @Schema(description = "Mensaje de error (solo en respuestas de error)") @JsonInclude(JsonInclude.Include.NON_NULL) String error,
        @Schema(description = "Filtros aplicados al reporte (solo en reporte filtrado)") @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> filtersApplied,
        @Schema(description = "ID del estudiante (solo en trazabilidad por estudiante)") @JsonInclude(JsonInclude.Include.NON_NULL) Long studentId,
        @Schema(description = "Datos del reporte") T data,
        @Schema(description = "Marca de tiempo de generación") LocalDateTime timestamp
) {

    public static <T> ReportResponse<T> success(String message, String reportType, T data) {
        return new ReportResponse<>(true, message, reportType, null, null, null, data, LocalDateTime.now());
    }

    public static <T> ReportResponse<T> success(String message, T data) {
        return new ReportResponse<>(true, message, null, null, null, null, data, LocalDateTime.now());
    }

    public static <T> ReportResponse<T> success(String message, String reportType, Map<String, Object> filtersApplied, T data) {
        return new ReportResponse<>(true, message, reportType, null, filtersApplied, null, data, LocalDateTime.now());
    }

    public static <T> ReportResponse<T> success(String message, String reportType, Long studentId, T data) {
        return new ReportResponse<>(true, message, reportType, null, null, studentId, data, LocalDateTime.now());
    }

    public static <T> ReportResponse<T> error(String message) {
        return new ReportResponse<>(false, message, null, message, null, null, null, LocalDateTime.now());
    }
}
