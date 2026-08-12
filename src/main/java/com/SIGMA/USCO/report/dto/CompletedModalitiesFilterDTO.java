package com.SIGMA.USCO.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para filtros del reporte de modalidades finalizadas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletedModalitiesFilterDTO {

    /**
     * Filtrar por tipos de modalidad
     */
    private List<String> modalityTypes;

    /**
     * Filtrar por resultado
     * Valores: SUCCESS, FAILED
     */
    private List<String> results;

    /**
     * Filtrar por año
     */
    private Integer year;

    /**
     * Filtrar por semestre
     */
    private Integer semester;

    /**
     * Incluir solo con distinción académica
     */
    private Boolean onlyWithDistinction;

    /**
     * Tipo de distinción específica
     * Valores: MERITORIOUS, LAUREATE
     */
    private String distinctionType;

    /**
     * Filtrar por director específico
     */
    private Long directorId;

    /**
     * Filtrar por rango de calificación
     */
    private Double minGrade;
    private Double maxGrade;

    /**
     * Filtrar por tipo (individual/grupal)
     */
    private String modalityTypeFilter; // INDIVIDUAL, GROUP

    /**
     * Ordenar por
     * Valores: DATE, GRADE, TYPE, DURATION
     */
    private String sortBy;

    /**
     * Dirección de orden
     */
    private String sortDirection; // ASC, DESC
}

