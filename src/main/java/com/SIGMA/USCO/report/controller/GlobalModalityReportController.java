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
import com.SIGMA.USCO.report.dto.StudentListingFilterDTO;
import com.SIGMA.USCO.report.dto.StudentListingReportDTO;
import com.SIGMA.USCO.report.dto.StudentsByModalityReportDTO;
import com.SIGMA.USCO.report.dto.StudentsBySemesterReportDTO;
import com.SIGMA.USCO.report.enums.ReportType;
import com.SIGMA.USCO.report.service.CompletedModalitiesPdfGenerator;
import com.SIGMA.USCO.report.service.DefenseCalendarPdfGenerator;
import com.SIGMA.USCO.report.service.DefenseCalendarReportService;
import com.SIGMA.USCO.report.service.DirectorAssignedModalitiesPdfGenerator;
import com.SIGMA.USCO.report.service.DirectorReportService;
import com.SIGMA.USCO.report.service.ModalityComparisonPdfGenerator;
import com.SIGMA.USCO.report.service.ModalityHistoricalPdfGenerator;
import com.SIGMA.USCO.report.service.ModalityTraceabilityPdfGenerator;
import com.SIGMA.USCO.report.service.ModalityTraceabilityReportService;
import com.SIGMA.USCO.report.service.PdfReport;
import com.SIGMA.USCO.report.service.ReportService;
import com.SIGMA.USCO.report.service.StudentListingPdfGenerator;
import com.SIGMA.USCO.report.service.StudentReportService;
import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GlobalModalityReportController {

    private final ReportService reportService;
    private final StudentReportService studentReportService;
    private final DirectorReportService directorReportService;
    private final PdfReport pdfGeneratorService;
    private final ModalityComparisonPdfGenerator comparisonPdfGenerator;
    private final DirectorAssignedModalitiesPdfGenerator directorPdfGenerator;
    private final ModalityHistoricalPdfGenerator modalityHistoricalPdfGenerator;
    private final StudentListingPdfGenerator studentListingPdfGenerator;
    private final CompletedModalitiesPdfGenerator completedModalitiesPdfGenerator;
    private final DefenseCalendarReportService defenseCalendarReportService;
    private final DefenseCalendarPdfGenerator defenseCalendarPdfGenerator;
    private final ModalityTraceabilityReportService modalityTraceabilityReportService;
    private final ModalityTraceabilityPdfGenerator modalityTraceabilityPdfGenerator;


    @GetMapping("/global/modalities")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getGlobalModalityReport() {
        try {
            GlobalModalityReportDTO report = reportService.generateGlobalReport();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte generado exitosamente",
                            "reportType", ReportType.GLOBAL_ACTIVE_MODALITIES.name(),
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }


    @GetMapping("/global/modalities/pdf")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<Resource> exportGlobalModalityReportToPDF() {
        try {
            GlobalModalityReportDTO report = reportService.generateGlobalReport();
            ByteArrayOutputStream pdfStream = pdfGeneratorService.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Global_Modalidades");

            return buildPdfResponse(resource, fileName, report.getMetadata().getTotalRecords());

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado: " + e.getMessage());
        }
    }



    @GetMapping("/students/by-modality")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getStudentsByModalityReport(@RequestParam String modalityType) {
        try {
            StudentsByModalityReportDTO report = studentReportService
                    .generateStudentsByModalityReport(modalityType);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte de estudiantes generado exitosamente",
                            "reportType", ReportType.STUDENTS_BY_MODALITY.name(),
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }


    @GetMapping("/students/by-semester")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getStudentsBySemesterReport(@RequestParam Integer year, @RequestParam Integer semester) {
        try {
            StudentsBySemesterReportDTO report = studentReportService
                    .generateStudentsBySemesterReport(year, semester);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte de estudiantes por semestre generado exitosamente",
                            "reportType", ReportType.STUDENTS_BY_SEMESTER.name(),
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    // ==================== REPORTES DE DIRECTORES ====================

    @GetMapping("/directors/by-modality")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getDirectorsByModalityReport(
            @RequestParam String modalityType
    ) {
        try {
            DirectorsByModalityReportDTO report = directorReportService
                    .generateDirectorsByModalityReport(modalityType);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte de directores generado exitosamente",
                            "reportType", ReportType.DIRECTORS_BY_MODALITY.name(),
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    // ==================== RF-49: REPORTES POR DIRECTOR ASIGNADO ====================

    /**
     * Genera un reporte de modalidades por director asignado (JSON)
     * RF-49 - Generación de Reportes por Director Asignado
     */
    @PostMapping("/directors/assigned-modalities")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getDirectorAssignedModalitiesReport(
            @RequestBody(required = false) DirectorReportFilterDTO filters
    ) {
        try {
            DirectorAssignedModalitiesReportDTO report = reportService.generateDirectorAssignedModalitiesReport(filters);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte de directores generado exitosamente",
                            "reportType", ReportType.DIRECTOR_ASSIGNED_MODALITIES.name(),
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte de directores: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Exporta a PDF un reporte de modalidades por director asignado
     * RF-49 - Generación de Reportes por Director Asignado
     */
    @PostMapping("/directors/assigned-modalities/pdf")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<Resource> exportDirectorAssignedModalitiesReportToPDF(@RequestBody(required = false) DirectorReportFilterDTO filters) {
        try {
            DirectorAssignedModalitiesReportDTO report = reportService.generateDirectorAssignedModalitiesReport(filters);
            ByteArrayOutputStream pdfStream = directorPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Directores_Modalidades");

            return buildPdfResponse(resource, fileName, report.getMetadata().getTotalRecords());

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Obtiene reporte de un director específico (JSON)
     * RF-49 - Generación de Reportes por Director Asignado
     */
    @GetMapping("/directors/{directorId}/modalities")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getSpecificDirectorReport(
            @PathVariable Long directorId
    ) {
        try {
            DirectorReportFilterDTO filters = DirectorReportFilterDTO.builder()
                    .directorId(directorId)
                    .includeWorkloadAnalysis(false)
                    .build();

            DirectorAssignedModalitiesReportDTO report = reportService.generateDirectorAssignedModalitiesReport(filters);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte del director generado exitosamente",
                            "reportType", ReportType.DIRECTOR_ASSIGNED_MODALITIES.name(),
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte del director: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Exporta a PDF el reporte de un director específico
     * RF-49 - Generación de Reportes por Director Asignado
     */
    @GetMapping("/directors/{directorId}/modalities/pdf")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<Resource> exportSpecificDirectorReportToPDF(@PathVariable Long directorId) {
        try {
            DirectorReportFilterDTO filters = DirectorReportFilterDTO.builder()
                    .directorId(directorId)
                    .includeWorkloadAnalysis(false)
                    .build();

            DirectorAssignedModalitiesReportDTO report = reportService.generateDirectorAssignedModalitiesReport(filters);
            ByteArrayOutputStream pdfStream = directorPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Director_" + directorId);

            return buildPdfResponse(resource, fileName, report.getMetadata().getTotalRecords());

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado: " + e.getMessage());
        }
    }

    // ==================== UTILIDADES Y METADATOS ====================

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "SIGMA Report Service",
                "timestamp", LocalDateTime.now(),
                "version", "2.0"
        ));
    }


    @GetMapping("/available")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getAvailableReports() {
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
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getAvailableModalityTypes() {
        try {
            AvailableModalityTypesDTO types = reportService.getAvailableModalityTypes();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Tipos de modalidad obtenidos exitosamente",
                            "data", types,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al obtener tipos de modalidad: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }


    @PostMapping("/modalities/filtered")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getFilteredModalityReport(@RequestBody ModalityReportFilterDTO filters) {
        try {
            GlobalModalityReportDTO report = reportService.generateFilteredReport(filters);

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

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte filtrado generado exitosamente",
                            "reportType", ReportType.FILTERED_MODALITIES.name(),
                            "filtersApplied", filterInfo,
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte filtrado: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Exporta a PDF un reporte filtrado por tipo de modalidad
     * RF-46 - Filtrado por Tipo de Modalidad
     */
    @PostMapping("/modalities/filtered/pdf")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<Resource> exportFilteredModalityReportToPDF(@RequestBody ModalityReportFilterDTO filters) {
        try {
            GlobalModalityReportDTO report = reportService.generateFilteredReport(filters);
            ByteArrayOutputStream pdfStream = pdfGeneratorService.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Modalidades_Filtrado");

            return buildPdfResponse(resource, fileName, report.getMetadata().getTotalRecords());

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado: " + e.getMessage());
        }
    }

    // ==================== RF-48: COMPARATIVA DE MODALIDADES POR TIPO ====================

    /**
     * Genera un reporte comparativo de modalidades por tipo de grado
     * RF-48 - Comparativa de Modalidades por Tipo de Grado
     */
    @PostMapping("/modalities/comparison")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getModalityTypeComparison(
            @RequestBody(required = false) ModalityComparisonFilterDTO filters
    ) {
        try {
            ModalityTypeComparisonReportDTO report = reportService.generateModalityTypeComparison(filters);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte comparativo generado exitosamente",
                            "reportType", ReportType.MODALITY_TYPE_COMPARISON.name(),
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte comparativo: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Exporta a PDF un reporte comparativo de modalidades por tipo
     * RF-48 - Comparativa de Modalidades por Tipo de Grado
     */
    @PostMapping("/modalities/comparison/pdf")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<Resource> exportModalityTypeComparisonToPDF(@RequestBody(required = false) ModalityComparisonFilterDTO filters) {
        try {
            ModalityTypeComparisonReportDTO report = reportService.generateModalityTypeComparison(filters);
            ByteArrayOutputStream pdfStream = comparisonPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Comparativa_Modalidades");

            return buildPdfResponse(resource, fileName, report.getMetadata().getTotalRecords());

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado: " + e.getMessage());
        }
    }

    // ==================== MÉTODOS HELPER ====================

    /**
     * Genera un reporte histórico completo de una modalidad específica
     * Análisis temporal de evolución, tendencias y estadísticas
     */
    @GetMapping("/modalities/{modalityTypeId}/historical")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getModalityHistoricalReport(
            @PathVariable Long modalityTypeId,
            @RequestParam(required = false, defaultValue = "8") Integer periods
    ) {
        try {
            ModalityHistoricalReportDTO report = reportService.generateModalityHistoricalReport(modalityTypeId, periods);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte histórico generado exitosamente",
                            "reportType", "MODALITY_HISTORICAL_ANALYSIS",
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte histórico: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Exporta el reporte histórico de modalidad a PDF
     * Reporte completo con análisis temporal, tendencias y proyecciones
     */
    @GetMapping("/modalities/{modalityTypeId}/historical/pdf")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<Resource> exportModalityHistoricalReportToPDF(@PathVariable Long modalityTypeId, @RequestParam(required = false, defaultValue = "8") Integer periods) {
        try {
            ModalityHistoricalReportDTO report = reportService.generateModalityHistoricalReport(modalityTypeId, periods);
            ByteArrayOutputStream pdfStream = modalityHistoricalPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String modalityName = report.getModalityInfo() != null ?
                report.getModalityInfo().getModalityName().replaceAll("[^a-zA-Z0-9]", "_") :
                "Modalidad";
            String fileName = generateFileName("Reporte_Historico_" + modalityName);

            return buildPdfResponse(resource, fileName,
                report.getHistoricalAnalysis() != null ? report.getHistoricalAnalysis().size() : 0);

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("Datos inválidos: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado: " + e.getMessage());
        }
    }

    // ==================== REPORTE DE LISTADO DE ESTUDIANTES CON FILTROS ====================

    /**
     * Genera reporte de listado de estudiantes con filtros múltiples (JSON)
     * Permite filtrar por estados, modalidades y semestres
     */
    @PostMapping("/students/listing")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getStudentListingReport(
            @RequestBody(required = false) StudentListingFilterDTO filters
    ) {
        try {
            StudentListingReportDTO report = reportService.generateStudentListingReport(filters);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte de listado de estudiantes generado exitosamente",
                            "reportType", "STUDENT_LISTING_FILTERED",
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Exporta el reporte de listado de estudiantes a PDF
     * Diseño profesional con múltiples secciones de análisis
     */
    @PostMapping("/students/listing/pdf")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<Resource> exportStudentListingReportToPDF(@RequestBody(required = false) StudentListingFilterDTO filters) {
        try {
            StudentListingReportDTO report = reportService.generateStudentListingReport(filters);
            ByteArrayOutputStream pdfStream = studentListingPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Listado_Estudiantes");

            return buildPdfResponse(resource, fileName,
                report.getStudents() != null ? report.getStudents().size() : 0);

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("Datos inválidos: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado: " + e.getMessage());
        }
    }

    // ==================== REPORTE DE MODALIDADES COMPLETADAS ====================

    /**
     * Genera reporte de modalidades completadas (exitosas y fallidas) en JSON
     * Incluye análisis completo de resultados, tiempos, calificaciones y distinciones
     */
    @PostMapping("/modalities/completed")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getCompletedModalitiesReport(
            @RequestBody(required = false) CompletedModalitiesFilterDTO filters
    ) {
        try {
            CompletedModalitiesReportDTO report = reportService.generateCompletedModalitiesReport(filters);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte de modalidades completadas generado exitosamente",
                            "reportType", "COMPLETED_MODALITIES_REPORT",
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Exporta el reporte de modalidades completadas a PDF
     * Diseño profesional con análisis completo de resultados, distinciones y desempeño
     */
    @PostMapping("/modalities/completed/pdf")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<Resource> exportCompletedModalitiesReportToPDF(@RequestBody(required = false) CompletedModalitiesFilterDTO filters) {
        try {
            CompletedModalitiesReportDTO report = reportService.generateCompletedModalitiesReport(filters);
            ByteArrayOutputStream pdfStream = completedModalitiesPdfGenerator.generatePDF(report);
            ByteArrayResource resource = new ByteArrayResource(pdfStream.toByteArray());

            String fileName = generateFileName("Reporte_Modalidades_Completadas");

            return buildPdfResponse(resource, fileName,
                report.getCompletedModalities() != null ? report.getCompletedModalities().size() : 0);

        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("Datos inválidos: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado: " + e.getMessage());
        }
    }

    // ==================== MÉTODOS HELPER ====================

    private Map<String, Object> buildReportInfo(
            ReportType reportType,
            String description,
            String[] formats,
            Map<String, String> endpoints
    ) {
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

    /**
     * Endpoint JSON: trazabilidad completa de una modalidad por su ID directo.
     * Uso: el comité consulta el estado en tiempo real de cualquier modalidad.
     */
    @GetMapping("/modality-traceability/{studentModalityId}")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getModalityTraceabilityReport(
            @PathVariable Long studentModalityId) {
        try {
            ModalityTraceabilityReportDTO report =
                    modalityTraceabilityReportService.generateReport(studentModalityId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Reporte de trazabilidad generado exitosamente",
                    "reportType", "MODALITY_TRACEABILITY",
                    "data", report,
                    "timestamp", LocalDateTime.now()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error al generar el reporte: " + e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    /**
     * Endpoint JSON: trazabilidad completa de la modalidad activa de un estudiante por su ID.
     * Uso: el comité selecciona al estudiante de la lista y obtiene su modalidad automáticamente.
     */
    @GetMapping("/modality-traceability/by-student/{studentId}")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getModalityTraceabilityReportByStudent(
            @PathVariable Long studentId) {
        try {
            ModalityTraceabilityReportDTO report =
                    modalityTraceabilityReportService.generateReportByStudentId(studentId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Reporte de trazabilidad generado exitosamente",
                    "reportType", "MODALITY_TRACEABILITY",
                    "studentId", studentId,
                    "data", report,
                    "timestamp", LocalDateTime.now()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Error al generar el reporte: " + e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    /**
     * Endpoint PDF: exporta el reporte de trazabilidad de una modalidad por su ID.
     */
    @GetMapping("/modality-traceability/{studentModalityId}/pdf")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
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

        } catch (RuntimeException e) {
            return buildErrorResponse("No se pudo generar el reporte: " + e.getMessage());
        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Endpoint PDF: exporta el reporte de trazabilidad buscando por ID de estudiante.
     */
    @GetMapping("/modality-traceability/by-student/{studentId}/pdf")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
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

        } catch (RuntimeException e) {
            return buildErrorResponse("No se pudo generar el reporte: " + e.getMessage());
        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado: " + e.getMessage());
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
                                "{\"success\": false, \"error\": \"%s\", \"timestamp\": \"%s\"}",
                                errorMessage,
                                LocalDateTime.now()
                        ).getBytes()
                ));
    }

    // ==================== REPORTE DE CALENDARIO DE SUSTENTACIONES ====================

    /**
     * Endpoint para obtener el reporte de calendario de sustentaciones en JSON
     * Incluye sustentaciones próximas, en progreso, completadas, estadísticas y alertas
     */
    @GetMapping("/defense-calendar")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<?> getDefenseCalendarReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "false") Boolean includeCompleted
    ) {
        try {
            LocalDateTime start = startDate != null
                    ? LocalDateTime.parse(startDate)
                    : null;
            LocalDateTime end = endDate != null
                    ? LocalDateTime.parse(endDate)
                    : null;

            DefenseCalendarReportDTO report = defenseCalendarReportService
                    .generateDefenseCalendarReport(start, end, includeCompleted);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "success", true,
                            "message", "Reporte de calendario de sustentaciones generado exitosamente",
                            "reportType", "DEFENSE_CALENDAR",
                            "data", report,
                            "timestamp", LocalDateTime.now()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", "Parámetros inválidos: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Error al generar el reporte: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Endpoint para exportar el reporte de calendario de sustentaciones a PDF
     * Diseño profesional e institucional con análisis completo
     */
    @GetMapping("/defense-calendar/pdf")
    @PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")
    public ResponseEntity<Resource> exportDefenseCalendarToPdf(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "false") Boolean includeCompleted
    ) {
        try {
            LocalDateTime start = startDate != null
                    ? LocalDateTime.parse(startDate)
                    : null;
            LocalDateTime end = endDate != null
                    ? LocalDateTime.parse(endDate)
                    : null;

            DefenseCalendarReportDTO report = defenseCalendarReportService
                    .generateDefenseCalendarReport(start, end, includeCompleted);

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

        } catch (IllegalArgumentException e) {
            return buildErrorResponse("Parámetros inválidos: " + e.getMessage());
        } catch (DocumentException | IOException e) {
            return buildErrorResponse("Error al generar el PDF: " + e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse("Error inesperado: " + e.getMessage());
        }
    }
}
