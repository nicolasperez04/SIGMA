package com.SIGMA.USCO.report.controller;

import com.SIGMA.USCO.report.dto.AvailableModalityTypesDTO;
import com.SIGMA.USCO.report.dto.CompletedModalitiesFilterDTO;
import com.SIGMA.USCO.report.dto.CompletedModalitiesReportDTO;
import com.SIGMA.USCO.report.dto.DefenseCalendarReportDTO;
import com.SIGMA.USCO.report.dto.DirectorAssignedModalitiesReportDTO;
import com.SIGMA.USCO.report.dto.DirectorReportFilterDTO;
import com.SIGMA.USCO.report.dto.DirectorsByModalityReportDTO;
import com.SIGMA.USCO.report.dto.GlobalModalityReportDTO;
import com.SIGMA.USCO.report.dto.ModalityComparisonFilterDTO;
import com.SIGMA.USCO.report.dto.ModalityHistoricalReportDTO;
import com.SIGMA.USCO.report.dto.ModalityReportFilterDTO;
import com.SIGMA.USCO.report.dto.ModalityTraceabilityReportDTO;
import com.SIGMA.USCO.report.dto.ModalityTypeComparisonReportDTO;
import com.SIGMA.USCO.report.dto.ReportResponse;
import com.SIGMA.USCO.report.dto.StudentListingFilterDTO;
import com.SIGMA.USCO.report.dto.StudentListingReportDTO;
import com.SIGMA.USCO.report.dto.StudentsByModalityReportDTO;
import com.SIGMA.USCO.report.dto.StudentsBySemesterReportDTO;
import com.SIGMA.USCO.report.enums.ReportType;
import com.SIGMA.USCO.report.service.CompletedModalitiesPdfGenerator;
import com.SIGMA.USCO.report.service.CompletedModalitiesReportService;
import com.SIGMA.USCO.report.service.ComparisonReportService;
import com.SIGMA.USCO.report.service.DefenseCalendarPdfGenerator;
import com.SIGMA.USCO.report.service.DefenseCalendarReportService;
import com.SIGMA.USCO.report.service.DirectorAssignedModalitiesPdfGenerator;
import com.SIGMA.USCO.report.service.DirectorAssignedModalitiesReportService;
import com.SIGMA.USCO.report.service.DirectorReportService;
import com.SIGMA.USCO.report.service.GlobalReportService;
import com.SIGMA.USCO.report.service.HistoricalReportService;
import com.SIGMA.USCO.report.service.ModalityComparisonPdfGenerator;
import com.SIGMA.USCO.report.service.ModalityHistoricalPdfGenerator;
import com.SIGMA.USCO.report.service.ModalityTraceabilityPdfGenerator;
import com.SIGMA.USCO.report.service.ModalityTraceabilityReportService;
import com.SIGMA.USCO.report.service.PdfReport;
import com.SIGMA.USCO.report.service.StudentListingPdfGenerator;
import com.SIGMA.USCO.report.service.StudentListingReportService;
import com.SIGMA.USCO.report.service.StudentReportService;
import com.SIGMA.USCO.common.security.Permissions;
import com.SIGMA.USCO.common.exception.BusinessException;
import com.SIGMA.USCO.security.SecurityUtils;
import com.SIGMA.USCO.Users.entity.User;
import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


@Tag(name = "Reportes", description = "Generación de reportes institucionales: modalidades, estudiantes, directores, análisis histórico, trazabilidad y calendarios")
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class GlobalModalityReportController {

    private final CompletedModalitiesReportService completedModalitiesReportService;
    private final StudentListingReportService studentListingReportService;
    private final HistoricalReportService historicalReportService;
    private final GlobalReportService globalReportService;
    private final ComparisonReportService comparisonReportService;
    private final StudentReportService studentReportService;
    private final DirectorReportService directorReportService;
    private final DirectorAssignedModalitiesReportService directorAssignedModalitiesReportService;
    private final ModalityTraceabilityReportService modalityTraceabilityReportService;
    private final DefenseCalendarReportService defenseCalendarReportService;
    private final PdfReport pdfGeneratorService;
    private final DirectorAssignedModalitiesPdfGenerator directorPdfGenerator;
    private final ModalityComparisonPdfGenerator comparisonPdfGenerator;
    private final ModalityHistoricalPdfGenerator modalityHistoricalPdfGenerator;
    private final StudentListingPdfGenerator studentListingPdfGenerator;
    private final CompletedModalitiesPdfGenerator completedModalitiesPdfGenerator;
    private final ModalityTraceabilityPdfGenerator modalityTraceabilityPdfGenerator;
    private final DefenseCalendarPdfGenerator defenseCalendarPdfGenerator;

    private User currentUser() {
        return SecurityUtils.getCurrentUser();
    }

    @GetMapping("/global/modalities")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    @Operation(summary = "Obtener reporte global de modalidades", description = "Retorna el reporte completo de todas las modalidades activas en el sistema con estadísticas y análisis.")
    @ApiResponse(responseCode = "200", description = "Reporte global generado exitosamente")
    public ResponseEntity<ReportResponse<GlobalModalityReportDTO>> getGlobalModalitiesReport() {
        try {
            GlobalModalityReportDTO report = globalReportService.generateGlobalReport(currentUser().getEmail());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte generado exitosamente",
                            ReportType.GLOBAL_ACTIVE_MODALITIES.name(),
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte");
        }
    }


    @GetMapping("/global/modalities/pdf")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    @Operation(summary = "Descargar reporte global en PDF", description = "Exporta el reporte global de modalidades en formato PDF. Incluye toda la información del reporte JSON en un documento profesional descargable.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF generado exitosamente y descargable"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado: necesita PERM_VIEW_REPORT"),
            @ApiResponse(responseCode = "500", description = "Error al generar el PDF")
    })
    public ResponseEntity<Resource> exportGlobalModalityReportToPDF() {
        try {
            GlobalModalityReportDTO report = globalReportService.generateGlobalReport(currentUser().getEmail());
            ByteArrayOutputStream pdfStream = pdfGeneratorService.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Global_Modalidades");

            return buildPdfResponse(resource, fileName, report.getMetadata().getTotalRecords());

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado");
        }
    }




    @GetMapping("/students/by-modality")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<StudentsByModalityReportDTO>> getStudentsByModalityReport(@RequestParam String modalityType) {
        // ========================================
        // SECCIÓN 3: REPORTES DE ESTUDIANTES
        // ========================================
        // Análisis de estudiantes por tipo de modalidad
        // Incluye: distribución, estados, avances y estadísticas por modalidad
        try {
            StudentsByModalityReportDTO report = studentReportService
                    .generateStudentsByModalityReport(modalityType);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte de estudiantes generado exitosamente",
                            ReportType.STUDENTS_BY_MODALITY.name(),
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte");
        }
    }


    @GetMapping("/students/by-semester")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<StudentsBySemesterReportDTO>> getStudentsBySemesterReport(@RequestParam Integer year, @RequestParam Integer semester) {
        try {
            StudentsBySemesterReportDTO report = studentReportService
                    .generateStudentsBySemesterReport(year, semester);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte de estudiantes por semestre generado exitosamente",
                            ReportType.STUDENTS_BY_SEMESTER.name(),
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte");
        }
    }

    // ==================== REPORTES DE DIRECTORES ====================

    @GetMapping("/directors/by-modality")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<DirectorsByModalityReportDTO>> getDirectorsByModalityReport(
            @RequestParam String modalityType
    ) {
        try {
            DirectorsByModalityReportDTO report = directorReportService
                    .generateDirectorsByModalityReport(modalityType);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte de directores generado exitosamente",
                            ReportType.DIRECTORS_BY_MODALITY.name(),
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte");
        }
    }

    // ==================== RF-49: REPORTES POR DIRECTOR ASIGNADO ====================

    /**
     * Genera un reporte de modalidades por director asignado (JSON)
     * RF-49 - Generación de Reportes por Director Asignado
     */
    @PostMapping("/directors/assigned-modalities")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<DirectorAssignedModalitiesReportDTO>> getDirectorAssignedModalitiesReport(
            @RequestBody(required = false) DirectorReportFilterDTO filters
    ) {
        try {
            DirectorAssignedModalitiesReportDTO report = directorAssignedModalitiesReportService.generateDirectorAssignedModalitiesReport(filters, currentUser().getEmail());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte de directores generado exitosamente",
                            ReportType.DIRECTOR_ASSIGNED_MODALITIES.name(),
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte de directores");
        }
    }

    /**
     * Exporta a PDF un reporte de modalidades por director asignado
     * RF-49 - Generación de Reportes por Director Asignado
     */
    @PostMapping("/directors/assigned-modalities/pdf")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<Resource> exportDirectorAssignedModalitiesReportToPDF(@RequestBody(required = false) DirectorReportFilterDTO filters) {
        try {
            DirectorAssignedModalitiesReportDTO report = directorAssignedModalitiesReportService.generateDirectorAssignedModalitiesReport(filters, currentUser().getEmail());
            ByteArrayOutputStream pdfStream = directorPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Directores_Modalidades");

            return buildPdfResponse(resource, fileName, report.getMetadata().getTotalRecords());

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado");
        }
    }

    /**
     * Obtiene reporte de un director específico (JSON)
     * RF-49 - Generación de Reportes por Director Asignado
     */
    @GetMapping("/directors/{directorId}/modalities")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<DirectorAssignedModalitiesReportDTO>> getSpecificDirectorReport(
            @PathVariable Long directorId
    ) {
        try {
            DirectorReportFilterDTO filters = DirectorReportFilterDTO.builder()
                    .directorId(directorId)
                    .includeWorkloadAnalysis(false)
                    .build();

            DirectorAssignedModalitiesReportDTO report = directorAssignedModalitiesReportService.generateDirectorAssignedModalitiesReport(filters, currentUser().getEmail());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte del director generado exitosamente",
                            ReportType.DIRECTOR_ASSIGNED_MODALITIES.name(),
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte del director");
        }
    }

    /**
     * Exporta a PDF el reporte de un director específico
     * RF-49 - Generación de Reportes por Director Asignado
     */
    @GetMapping("/directors/{directorId}/modalities/pdf")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<Resource> exportSpecificDirectorReportToPDF(@PathVariable Long directorId) {
        try {
            DirectorReportFilterDTO filters = DirectorReportFilterDTO.builder()
                    .directorId(directorId)
                    .includeWorkloadAnalysis(false)
                    .build();

            DirectorAssignedModalitiesReportDTO report = directorAssignedModalitiesReportService.generateDirectorAssignedModalitiesReport(filters, currentUser().getEmail());
            ByteArrayOutputStream pdfStream = directorPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Director_" + directorId);

            return buildPdfResponse(resource, fileName, report.getMetadata().getTotalRecords());

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado");
        }
    }

    // ==================== UTILIDADES Y METADATOS ====================

    @GetMapping("/health")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    @Schema(description = "Diccionario dinámico de estado del servicio (claves: status, service, timestamp, version)")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "SIGMA Report Service",
                "timestamp", LocalDateTime.now(),
                "version", "2.0"
        ));
    }


    @GetMapping("/available")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    @Operation(summary = "Catálogo de reportes disponibles", description = "Retorna un listado completo con todos los reportes disponibles en el sistema. Incluye: tipos de reporte, formatos soportados (JSON/PDF), rutas de acceso y descripción de cada uno.")
    @ApiResponse(responseCode = "200", description = "Catálogo de reportes obtenido exitosamente")
    @Schema(description = "Diccionario dinámico: catálogo de reportes disponibles (claves: success, availableReports, timestamp)")
    public ResponseEntity<Map<String, Object>> getAvailableReportsCatalog() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "availableReports", Map.of(
                        "globalModalities", buildReportInfo(
                                ReportType.GLOBAL_ACTIVE_MODALITIES,
                                "Reporte completo de todas las modalidades activas en el sistema",
                                new String[]{"JSON", "PDF"},
                                Map.of(
                                        "json", "/api/reports/global/modalities",
                                        "pdf", "/api/reports/global/modalities/pdf"
                                )
                        ),
                        "studentsByModality", buildReportInfo(
                                ReportType.STUDENTS_BY_MODALITY,
                                "Estudiantes asociados a una modalidad específica",
                                new String[]{"JSON"},
                                Map.of("json", "/api/reports/students/by-modality?modalityType={type}")
                        ),
                        "studentsBySemester", buildReportInfo(
                                ReportType.STUDENTS_BY_SEMESTER,
                                "Estudiantes por período académico",
                                new String[]{"JSON"},
                                Map.of("json", "/api/reports/students/by-semester?year={year}&semester={semester}")
                        ),
                        "directorsByModality", buildReportInfo(
                                ReportType.DIRECTORS_BY_MODALITY,
                                "Directores y su carga de trabajo por modalidad",
                                new String[]{"JSON"},
                                Map.of("json", "/api/reports/directors/by-modality?modalityType={type}")
                        ),
                        "directorsAssignedModalities", buildReportInfo(
                                ReportType.DIRECTOR_ASSIGNED_MODALITIES,
                                "Modalidades asignadas a directores específicos",
                                new String[]{"JSON", "PDF"},
                                Map.of(
                                        "json", "/api/reports/directors/assigned-modalities",
                                        "pdf", "/api/reports/directors/assigned-modalities/pdf"
                                )
                        )
                ),
                "timestamp", LocalDateTime.now()
        ));
    }


    @GetMapping("/modalities/types")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<AvailableModalityTypesDTO>> getAvailableModalityTypes() {
        try {
            AvailableModalityTypesDTO types = globalReportService.getAvailableModalityTypes();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success("Tipos de modalidad obtenidos exitosamente", types));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener tipos de modalidad");
        }
    }


    @PostMapping("/modalities/filtered")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<GlobalModalityReportDTO>> getFilteredModalityReport(@RequestBody ModalityReportFilterDTO filters) {
        try {
            GlobalModalityReportDTO report = globalReportService.generateFilteredReport(filters, currentUser().getEmail());

            // Construir información de filtros aplicados
            Map<String, Object> filterInfo = new java.util.HashMap<>();
            if (filters.getDegreeModalityIds() != null && !filters.getDegreeModalityIds().isEmpty()) {
                filterInfo.put("modalityIds", filters.getDegreeModalityIds());
            }
            if (filters.getDegreeModalityNames() != null && !filters.getDegreeModalityNames().isEmpty()) {
                filterInfo.put("modalityNames", filters.getDegreeModalityNames());
            }
            if (filters.getProcessStatuses() != null && !filters.getProcessStatuses().isEmpty()) {
                filterInfo.put("processStatuses", filters.getProcessStatuses());
            }
            if (filters.getStartDate() != null) {
                filterInfo.put("startDate", filters.getStartDate());
            }
            if (filters.getEndDate() != null) {
                filterInfo.put("endDate", filters.getEndDate());
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte filtrado generado exitosamente",
                            ReportType.FILTERED_MODALITIES.name(),
                            filterInfo,
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte filtrado");
        }
    }

    /**
     * Exporta a PDF un reporte filtrado por tipo de modalidad
     * RF-46 - Filtrado por Tipo de Modalidad
     */
    @PostMapping("/modalities/filtered/pdf")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<Resource> exportFilteredModalityReportToPDF(@RequestBody ModalityReportFilterDTO filters) {
        try {
            GlobalModalityReportDTO report = globalReportService.generateFilteredReport(filters, currentUser().getEmail());
            ByteArrayOutputStream pdfStream = pdfGeneratorService.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Modalidades_Filtrado");

            return buildPdfResponse(resource, fileName, report.getMetadata().getTotalRecords());

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado");
        }
    }

    // ==================== RF-48: COMPARATIVA DE MODALIDADES POR TIPO ====================

    /**
     * Genera un reporte comparativo de modalidades por tipo de grado
     * RF-48 - Comparativa de Modalidades por Tipo de Grado
     */
    @PostMapping("/modalities/comparison")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<ModalityTypeComparisonReportDTO>> getModalityTypeComparison(
            @RequestBody(required = false) ModalityComparisonFilterDTO filters
    ) {
        try {
            ModalityTypeComparisonReportDTO report = comparisonReportService.generateModalityTypeComparison(filters, currentUser().getEmail());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte comparativo generado exitosamente",
                            ReportType.MODALITY_TYPE_COMPARISON.name(),
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte comparativo");
        }
    }

    /**
     * Exporta a PDF un reporte comparativo de modalidades por tipo
     * RF-48 - Comparativa de Modalidades por Tipo de Grado
     */
    @PostMapping("/modalities/comparison/pdf")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<Resource> exportModalityTypeComparisonToPDF(@RequestBody(required = false) ModalityComparisonFilterDTO filters) {
        try {
            ModalityTypeComparisonReportDTO report = comparisonReportService.generateModalityTypeComparison(filters, currentUser().getEmail());
            ByteArrayOutputStream pdfStream = comparisonPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Comparativa_Modalidades");

            return buildPdfResponse(resource, fileName, report.getMetadata().getTotalRecords());

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado");
        }
    }

    // ==================== MÉTODOS HELPER ====================

    /**
     * Genera un reporte histórico completo de una modalidad específica
     * Análisis temporal de evolución, tendencias y estadísticas
     */
    @GetMapping("/modalities/{modalityTypeId}/historical")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<ModalityHistoricalReportDTO>> getModalityHistoricalReport(
            @PathVariable Long modalityTypeId,
            @RequestParam(required = false, defaultValue = "8") Integer periods
    ) {
        try {
            ModalityHistoricalReportDTO report = historicalReportService.generateModalityHistoricalReport(modalityTypeId, periods, currentUser().getEmail());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte histórico generado exitosamente",
                            "MODALITY_HISTORICAL_ANALYSIS",
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte histórico");
        }
    }

    /**
     * Exporta el reporte histórico de modalidad a PDF
     * Reporte completo con análisis temporal, tendencias y proyecciones
     */
    @GetMapping("/modalities/{modalityTypeId}/historical/pdf")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<Resource> exportModalityHistoricalReportToPDF(@PathVariable Long modalityTypeId, @RequestParam(required = false, defaultValue = "8") Integer periods) {
        try {
            ModalityHistoricalReportDTO report = historicalReportService.generateModalityHistoricalReport(modalityTypeId, periods, currentUser().getEmail());
            ByteArrayOutputStream pdfStream = modalityHistoricalPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String modalityName = report.getModalityInfo() != null ?
                report.getModalityInfo().getModalityName().replaceAll("[^a-zA-Z0-9]", "_") :
                "Modalidad";
            String fileName = generateFileName("Reporte_Historico_" + modalityName);

            return buildPdfResponse(resource, fileName,
                report.getHistoricalAnalysis() != null ? report.getHistoricalAnalysis().size() : 0);

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado");
        }
    }

    // ==================== REPORTE DE LISTADO DE ESTUDIANTES CON FILTROS ====================

    /**
     * Genera reporte de listado de estudiantes con filtros múltiples (JSON)
     * Permite filtrar por estados, modalidades y semestres
     */
    @PostMapping("/students/listing")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<StudentListingReportDTO>> getStudentListingReport(
            @RequestBody(required = false) StudentListingFilterDTO filters
    ) {
        try {
            StudentListingReportDTO report = studentListingReportService.generateStudentListingReport(filters, currentUser().getEmail());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte de listado de estudiantes generado exitosamente",
                            "STUDENT_LISTING_FILTERED",
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte");
        }
    }

    /**
     * Exporta el reporte de listado de estudiantes a PDF
     * Diseño profesional con múltiples secciones de análisis
     */
    @PostMapping("/students/listing/pdf")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<Resource> exportStudentListingReportToPDF(@RequestBody(required = false) StudentListingFilterDTO filters) {
        try {
            StudentListingReportDTO report = studentListingReportService.generateStudentListingReport(filters, currentUser().getEmail());
            ByteArrayOutputStream pdfStream = studentListingPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Listado_Estudiantes");

            return buildPdfResponse(resource, fileName,
                report.getStudents() != null ? report.getStudents().size() : 0);

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado");
        }
    }

    // ==================== REPORTE DE MODALIDADES COMPLETADAS ====================

    /**
     * Genera reporte de modalidades completadas (exitosas y fallidas) en JSON
     * Incluye análisis completo de resultados, tiempos, calificaciones y distinciones
     */
    @PostMapping("/modalities/completed")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<CompletedModalitiesReportDTO>> getCompletedModalitiesReport(
            @RequestBody(required = false) CompletedModalitiesFilterDTO filters
    ) {
        try {
            CompletedModalitiesReportDTO report = completedModalitiesReportService.generateCompletedModalitiesReport(filters, currentUser().getEmail());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte de modalidades completadas generado exitosamente",
                            "COMPLETED_MODALITIES_REPORT",
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte");
        }
    }

    /**
     * Exporta el reporte de modalidades completadas a PDF
     * Diseño profesional con análisis completo de resultados, distinciones y desempeño
     */
    @PostMapping("/modalities/completed/pdf")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<Resource> exportCompletedModalitiesReportToPDF(@RequestBody(required = false) CompletedModalitiesFilterDTO filters) {
        try {
            CompletedModalitiesReportDTO report = completedModalitiesReportService.generateCompletedModalitiesReport(filters, currentUser().getEmail());
            ByteArrayOutputStream pdfStream = completedModalitiesPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Modalidades_Completadas");

            return buildPdfResponse(resource, fileName,
                report.getCompletedModalities() != null ? report.getCompletedModalities().size() : 0);

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado");
        }
    }

    // ==================== MÉTODOS HELPER ====================
    // Funciones auxiliares para construir respuestas estandarizadas y generar nombres de archivo

    private Map<String, Object> buildReportInfo(
            ReportType reportType,
            String description,
            String[] formats,
            Map<String, String> endpoints
    ) {
        // Construye información metadatos sobre un reporte específico
        // Retorna: nombre, descripción, RF (requisito funcional), formatos disponibles, endpoints y actores
        return Map.of(
                "name", reportType.getDisplayName(),
                "description", description,
                "rfNumber", reportType.getRequirementCode(),
                "formats", formats,
                "endpoints", endpoints,
                "requiredPermissions", new String[]{
                        "PERM_VIEW_ALL_MODALITIES",
                        "PERM_GENERATE_REPORTS"
                },
                "actors", new String[]{"Secretaría", "Consejo", "Comité de Programa"}
        );
    }

    private String generateFileName(String baseName) {
        return String.format(
                "%s_%s.pdf",
                baseName,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))
        );
    }

    // ==================== REPORTE DE TRAZABILIDAD DE MODALIDAD ====================
    // Permite seguimiento completo del estado de cada modalidad en tiempo real
    // Incluye: historial de cambios, documentos, evaluaciones, aprobaciones y alertas

    /**
     * Endpoint JSON: trazabilidad completa de una modalidad por su ID directo.
     * Uso: el comité consulta el estado en tiempo real de cualquier modalidad.
     */
    @GetMapping("/modality-traceability/{studentModalityId}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<ModalityTraceabilityReportDTO>> getModalityTraceabilityReport(
            @PathVariable Long studentModalityId) {
        try {
            ModalityTraceabilityReportDTO report =
                    modalityTraceabilityReportService.generateReport(studentModalityId);

            return ResponseEntity.ok(ReportResponse.success(
                    "Reporte de trazabilidad generado exitosamente",
                    "MODALITY_TRACEABILITY",
                    report));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte");
        }
    }

    /**
     * Endpoint JSON: trazabilidad completa de la modalidad activa de un estudiante por su ID.
     * Uso: el comité selecciona al estudiante de la lista y obtiene su modalidad automáticamente.
     */
    @GetMapping("/modality-traceability/by-student/{studentId}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<ModalityTraceabilityReportDTO>> getModalityTraceabilityReportByStudent(
            @PathVariable Long studentId) {
        try {
            ModalityTraceabilityReportDTO report =
                    modalityTraceabilityReportService.generateReportByStudentId(studentId);

            return ResponseEntity.ok(ReportResponse.success(
                    "Reporte de trazabilidad generado exitosamente",
                    "MODALITY_TRACEABILITY",
                    studentId,
                    report));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte");
        }
    }

    /**
     * Endpoint PDF: exporta el reporte de trazabilidad de una modalidad por su ID.
     */
    @GetMapping("/modality-traceability/{studentModalityId}/pdf")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<Resource> exportModalityTraceabilityToPdf(
            @PathVariable Long studentModalityId) {
        try {
            ModalityTraceabilityReportDTO report =
                    modalityTraceabilityReportService.generateReport(studentModalityId);

            byte[] pdfBytes = modalityTraceabilityPdfGenerator.generatePdf(report);
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            String fileName = generateFileName("Trazabilidad_Modalidad_" + studentModalityId);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);
            headers.add("X-Report-Generated-At", LocalDateTime.now().toString());
            headers.add("X-Report-Type", "MODALITY_TRACEABILITY");
            headers.add("X-Modality-Id", String.valueOf(studentModalityId));

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            return buildErrorResponse("No se pudo generar el reporte");
        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF");
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado");
        }
    }

    /**
     * Endpoint PDF: exporta el reporte de trazabilidad buscando por ID de estudiante.
     */
    @GetMapping("/modality-traceability/by-student/{studentId}/pdf")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<Resource> exportModalityTraceabilityByStudentToPdf(
            @PathVariable Long studentId) {
        try {
            ModalityTraceabilityReportDTO report =
                    modalityTraceabilityReportService.generateReportByStudentId(studentId);

            byte[] pdfBytes = modalityTraceabilityPdfGenerator.generatePdf(report);
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            String fileName = generateFileName("Trazabilidad_Estudiante_" + studentId);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);
            headers.add("X-Report-Generated-At", LocalDateTime.now().toString());
            headers.add("X-Report-Type", "MODALITY_TRACEABILITY");
            headers.add("X-Student-Id", String.valueOf(studentId));

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            return buildErrorResponse("No se pudo generar el reporte");
        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF");
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado");
        }
    }

    private ResponseEntity<Resource> buildPdfResponse(
            ByteArrayResource resource,
            String fileName,
            Integer totalRecords
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);
        headers.add("X-Report-Generated-At", LocalDateTime.now().toString());
        headers.add("X-Total-Records", String.valueOf(totalRecords));

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    private ResponseEntity<Resource> buildErrorResponse(String errorMessage) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ByteArrayResource(
                        String.format(
                                "{\"success\": false, \"message\": \"%s\", \"error\": \"%s\", \"data\": null, \"timestamp\": \"%s\"}",
                                errorMessage,
                                errorMessage,
                                LocalDateTime.now()
                        ).getBytes()
                ));
    }

    private <T> ResponseEntity<ReportResponse<T>> jsonError(HttpStatus status, String errorMessage) {
        return ResponseEntity.status(status).body(ReportResponse.error(errorMessage));
    }

    // ==================== REPORTE DE CALENDARIO DE SUSTENTACIONES ====================

    /**
     * Endpoint para obtener el reporte de calendario de sustentaciones en JSON
     * Incluye sustentaciones próximas, en progreso, completadas, estadísticas y alertas
     */
    @GetMapping("/defense-calendar")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<ReportResponse<DefenseCalendarReportDTO>> getDefenseCalendarReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "false") Boolean includeCompleted
    ) {
        try {
            DefenseCalendarReportDTO report = defenseCalendarReportService
                    .generateDefenseCalendarReport(startDate, endDate, includeCompleted, currentUser());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ReportResponse.success(
                            "Reporte de calendario de sustentaciones generado exitosamente",
                            "DEFENSE_CALENDAR",
                            report));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte");
        }
    }

    /**
     * Endpoint para exportar el reporte de calendario de sustentaciones a PDF
     * Diseño profesional e institucional con análisis completo
     */
    @GetMapping("/defense-calendar/pdf")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_REPORT + "')")
    public ResponseEntity<Resource> exportDefenseCalendarToPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "false") Boolean includeCompleted
    ) {
        try {
            DefenseCalendarReportDTO report = defenseCalendarReportService
                    .generateDefenseCalendarReport(startDate, endDate, includeCompleted, currentUser());

            byte[] pdfBytes = defenseCalendarPdfGenerator.generatePdf(report);
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            String fileName = generateFileName("Calendario_Sustentaciones");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);
            headers.add("X-Report-Generated-At", LocalDateTime.now().toString());
            headers.add("X-Report-Type", "DEFENSE_CALENDAR");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado");
        }
    }
}
