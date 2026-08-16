package com.SIGMA.USCO.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para los filtros de reportes de modalidades
 * RF-46 - Filtrado por Tipo de Modalidad
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModalityReportFilterDTO {

    /**
     * Lista de IDs de modalidades de grado para filtrar
     * Ejemplo: [1, 3, 5] para filtrar por Proyecto de Grado, Pasantía, etc.
     */
    private List<Long> degreeModalityIds;

    /**
     * Lista de nombres de modalidades de grado para filtrar
     * Ejemplo: ["PROYECTO DE GRADO", "PASANTIA"]
     */
    private List<String> degreeModalityNames;

    /**
     * Filtrar por estado de proceso
     * Ejemplo: ["APROBADO", "EN_REVISION"]
     */
    private List<String> processStatuses;

    /**
     * Indica si se deben incluir modalidades sin director asignado
     */
    private Boolean includeWithoutDirector;

    /**
     * Indica si se deben incluir solo modalidades con director asignado
     */
    private Boolean onlyWithDirector;

    /**
     * Filtro por rango de fecha de última actualización (inclusive)
     */
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}

