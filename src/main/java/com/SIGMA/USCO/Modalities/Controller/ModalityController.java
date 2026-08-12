package com.SIGMA.USCO.Modalities.Controller;

import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.dto.*;
import com.SIGMA.USCO.Modalities.dto.response.FinalDefenseResponse;
import com.SIGMA.USCO.Modalities.dto.response.ProjectDirectorResponse;
import com.SIGMA.USCO.Modalities.dto.response.StudentModalityExaminerDTO;
import com.SIGMA.USCO.Modalities.service.CancellationService;
import com.SIGMA.USCO.Modalities.service.DefenseModalityService;
import com.SIGMA.USCO.Modalities.service.DocumentEditRequestService;
import com.SIGMA.USCO.Modalities.service.DocumentModalityService;
import com.SIGMA.USCO.Modalities.service.ModalityCatalogService;
import com.SIGMA.USCO.Modalities.service.StudentModalityListingService;
import com.SIGMA.USCO.Modalities.service.SeminarModalityService;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.service.DocumentService;
import com.SIGMA.USCO.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

@Tag(name = "Modalidades", description = "Gestión completa de modalidades de grado: creación, documentos, sustentación, cancelaciones y evaluaciones")
@RestController
@RequestMapping("/modalities")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class ModalityController {

    private final ModalityCatalogService modalityCatalogService;
    private final StudentModalityListingService modalityListingService;
    private final SeminarModalityService seminarModalityService;
    private final CancellationService cancellationService;
    private final DefenseModalityService defenseModalityService;
    private final DocumentModalityService documentModalityService;
    private final DocumentEditRequestService documentEditRequestService;
    private final DocumentService documentService;

    @Operation(summary = "Crear modalidad de grado", description = "Crea una nueva modalidad de grado en el sistema. El administrador define el tipo, requisitos y configuración específica de la modalidad.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Modalidad creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: falta información requerida"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado: necesita PERM_CREATE_MODALITY o PERM_UPDATE_MODALITY"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor al crear la modalidad")
    })
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('PERM_CREATE_MODALITY') or hasAuthority('PERM_UPDATE_MODALITY')")
    public ResponseEntity<?> createModality(@RequestBody @Valid ModalityDTO request) {
        modalityCatalogService.createModality(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", " Modalidad creada exitosamente."
                )
        );
    }

    @Operation(summary = "Actualizar configuración de modalidad", description = "Modifica los parámetros de una modalidad existente: requisitos, plazos, condiciones de aprobación, etc.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modalidad actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado")
    })
    @PutMapping("/update/{modalityId}")
    @PreAuthorize("hasAuthority('PERM_CREATE_MODALITY') or hasAuthority('PERM_UPDATE_MODALITY')")
    public ResponseEntity<String> updateModality(@PathVariable Long modalityId, @RequestBody @Valid ModalityDTO request) {
        return ResponseEntity.ok(modalityCatalogService.updateModality(modalityId, request));
    }

    @Operation(summary = "Desactivar modalidad", description = "Desactiva una modalidad de grado (eliminación lógica). La modalidad no aparecerá en nuevas inscripciones pero se mantiene para consulta histórica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modalidad desactivada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado: necesita PERM_DESACTIVE_MODALITY")
    })
    @PutMapping("delete/{modalityId}")
    @PreAuthorize("hasAuthority('PERM_DESACTIVE_MODALITY')")
    public ResponseEntity<String> deactivateModality(@PathVariable Long modalityId) {
        return ResponseEntity.ok(modalityCatalogService.desactiveModality(modalityId));
    }


    @Operation(summary = "Crear requisitos de modalidad", description = "Define los requisitos documentales obligatorios que los estudiantes deben cumplir para completar esta modalidad de grado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Requisitos creados correctamente"),
            @ApiResponse(responseCode = "400", description = "Lista de requisitos inválida"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado")
    })
    @PostMapping("/requirements/create/{modalityId}")
    @PreAuthorize("hasAuthority('PERM_CREATE_MODALITY') or hasAuthority('PERM_UPDATE_MODALITY')")
    public ResponseEntity<?> createModalityRequirements(@PathVariable Long modalityId, @RequestBody @Valid List<@Valid RequirementDTO> requirements) {
        modalityCatalogService.createModalityRequirements(modalityId, requirements);
        return ResponseEntity.ok("Requisitos creados correctamente");
    }

    @Operation(summary = "Actualizar requisito de modalidad", description = "Modifica un requisito específico: nombre, descripción, tipo de documento, plazo de entrega, etc.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Requisito actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos del requisito inválidos"),
            @ApiResponse(responseCode = "404", description = "Requisito o modalidad no encontrados"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado")
    })
    @PutMapping("/requirements/{modalityId}/update/{requirementId}")
    @PreAuthorize("hasAuthority('PERM_CREATE_MODALITY') or hasAuthority('PERM_UPDATE_MODALITY')")
    public ResponseEntity<?> updateRequirement(@PathVariable Long modalityId, @PathVariable Long requirementId, @RequestBody @Valid RequirementDTO request) {
        modalityCatalogService.updateModalityRequirement(modalityId, requirementId, request);
        return ResponseEntity.ok("Requisito actualizado correctamente");
    }


    @Operation(summary = "Obtener requisitos de modalidad", description = "Retorna la lista de requisitos documentales definidos para una modalidad, con opción de filtrar por estado (activo/inactivo).")
    @ApiResponse(responseCode = "200", description = "Lista de requisitos obtenida")
    @GetMapping("/{modalityId}/requirements")
    public ResponseEntity<List<RequirementDTO>> listRequirements(@PathVariable Long modalityId, @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(modalityCatalogService.getModalityRequirements(modalityId, active));
    }

    @Operation(summary = "Eliminar requisito de modalidad", description = "Desactiva un requisito documental de la modalidad (eliminación lógica). No afecta documentos ya entregados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Requisito eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Requisito no encontrado"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado: necesita PERM_DELETE_MODALITY_REQUIREMENT")
    })
    @PutMapping("/requirements/delete/{requirementId}")
    @PreAuthorize("hasAuthority('PERM_DELETE_MODALITY_REQUIREMENT')")
    public ResponseEntity<String> desactiveRequirements(@PathVariable Long requirementId) {
        return ResponseEntity.ok(modalityCatalogService.deleteRequirement(requirementId));
    }

    @PutMapping("/requirements/active/{requirementId}")
    @PreAuthorize("hasAuthority('PERM_DELETE_MODALITY_REQUIREMENT')")
    public ResponseEntity<String> activeRequirements(@PathVariable Long requirementId) {
        return ResponseEntity.ok(modalityCatalogService.activeRequirement(requirementId));
    }


    @Operation(summary = "Obtener todas las modalidades", description = "Retorna una lista completa de todas las modalidades de grado disponibles en el sistema, incluyendo activas e inactivas.")
    @ApiResponse(responseCode = "200", description = "Lista completa de modalidades obtenida")
    @GetMapping
    public ResponseEntity<List<ModalityDTO>> getAllModalities() {
        return ResponseEntity.ok(modalityCatalogService.getAllModalities());
    }

    @Operation(summary = "Obtener detalle de modalidad", description = "Retorna la información completa de una modalidad específica: configuración, requisitos, estudiantes inscritos, estado actual.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalle de modalidad obtenido"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ModalityDTO> getModalityById(@PathVariable Long id) {
        return ResponseEntity.ok(modalityCatalogService.getModalityDetail(id));
    }

    @Operation(summary = "Cargar documento requerido", description = "Permite al estudiante o director cargar un documento obligatorio para la modalidad de grado. Valida tipo de archivo, tamaño y formato.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documento cargado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Archivo inválido: formato no soportado o excede tamaño máximo"),
            @ApiResponse(responseCode = "404", description = "Modalidad o documento requerido no encontrado"),
            @ApiResponse(responseCode = "413", description = "Archivo demasiado grande (máximo 20MB)")
    })
    @PostMapping(
            value = "/{studentModalityId}/documents/{requiredDocumentId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @PathVariable Long studentModalityId,
            @PathVariable Long requiredDocumentId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {

        return ResponseEntity.ok(documentModalityService.uploadRequiredDocument(
                studentModalityId,
                requiredDocumentId,
                file
        ));


    }


    @PostMapping("/{modalityId}/start")
    public ResponseEntity<Map<String, Object>> startModality(@PathVariable Long modalityId) {
        return ResponseEntity.ok(documentModalityService.startStudentModalityIndividual(modalityId));
    }

    @GetMapping("/{id}/validate-documents")
    public ResponseEntity<Map<String, Object>> validateDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(documentModalityService.validateAllDocumentsUploaded(id));
    }

    @GetMapping("/my-available-documents")
    public ResponseEntity<Map<String, Object>> getMyAvailableDocuments() {
        return ResponseEntity.ok(documentModalityService.getAvailableDocumentsForStudent());
    }

    @GetMapping("/{studentModalityId}/documents")
    @PreAuthorize("hasAuthority('PERM_REVIEW_DOCUMENTS')")
    public ResponseEntity<List<Map<String, Object>>> listStudentDocuments(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentModalityService.getStudentDocuments(studentModalityId));
    }

    @GetMapping("/student/{studentDocumentId}/view")
    @PreAuthorize("hasAuthority('PERM_VIEW_DOCUMENTS')")
    public ResponseEntity<Resource> viewStudentDocument(@PathVariable Long studentDocumentId) throws MalformedURLException {
        return ResponseEntity.ok(documentModalityService.viewStudentDocument(studentDocumentId));
    }

    @PutMapping("/documents/{studentDocumentId}/review")
    @PreAuthorize("hasAuthority('PERM_REVIEW_DOCUMENTS')")
    public ResponseEntity<Map<String, Object>> reviewDocument(@PathVariable Long studentDocumentId, @Valid @RequestBody DocumentReviewDTO request) {
        return ResponseEntity.ok(documentModalityService.reviewStudentDocument(studentDocumentId, request));
    }

    @PostMapping("/{studentModalityId}/approve-program-head")
    @PreAuthorize("hasAuthority('PERM_APPROVE_MODALITY')")
    public ResponseEntity<Map<String, Object>> approveByProgramHead(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentModalityService.approveModalityByProgramHead(studentModalityId));
    }

    @PostMapping("/{studentModalityId}/approve-committee")
    @PreAuthorize("hasAuthority('PERM_APPROVE_MODALITY')")
    public ResponseEntity<Map<String, Object>> approveByCommittee(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentModalityService.approveModalityByCommittee(studentModalityId));
    }

    @PostMapping("/{studentModalityId}/approve-examiners")
    @PreAuthorize("hasAuthority('PERM_APPROVE_MODALITY_BY_EXAMINER')")
    public ResponseEntity<Map<String, Object>> approveByExaminer(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentModalityService.approveModalityByExaminers(studentModalityId));
    }

    @PostMapping("/documents/{studentDocumentId}/review-committee")
    @PreAuthorize("hasAuthority('PERM_REVIEW_DOCUMENTS')")
    public ResponseEntity<Map<String, Object>> reviewDocumentCommittee(@PathVariable Long studentDocumentId, @Valid @RequestBody DocumentReviewDTO request) {
        return ResponseEntity.ok(documentModalityService.reviewStudentDocumentByCommittee(studentDocumentId, request));
    }

    @PutMapping("/documents/{studentDocumentId}/review-examiner")
    @PreAuthorize("hasAuthority('PERM_REVIEW_DOCUMENTS')")
    public ResponseEntity<Object> reviewDocumentExaminer(@PathVariable Long studentDocumentId, @Valid @RequestBody DocumentReviewDTO request) {
        return ResponseEntity.ok(documentModalityService.reviewStudentDocumentByExaminer(studentDocumentId, request));
    }

    @PutMapping("/documents/{studentDocumentId}/review-examiner-final-document")
    @PreAuthorize("hasAuthority('PERM_REVIEW_DOCUMENTS')")
    public ResponseEntity<Map<String, Object>> reviewSecondaryDocumentExaminer(@PathVariable Long studentDocumentId, @Valid @RequestBody DocumentReviewDTO request) {
        return ResponseEntity.ok(documentModalityService.reviewFinalDocumentByExaminer(studentDocumentId, request));
    }

    @GetMapping("/students")
    @PreAuthorize("hasAuthority('PERM_VIEW_ALL_MODALITIES')")
    public ResponseEntity<List<ModalityListDTO>> listAllModalitiesForProgramHead(@RequestParam(required = false)
                                                             List<ModalityProcessStatus> statuses, @RequestParam(required = false)
                                                             String name) {
        return ResponseEntity.ok(modalityListingService.getAllStudentModalitiesForProgramHead(statuses, name));
    }

    @GetMapping("/students/committee")
    @PreAuthorize("hasAuthority('PERM_VIEW_ALL_MODALITIES')")
    public ResponseEntity<List<ModalityListDTO>> listAllModalitiesForCommittee(@RequestParam(required = false)
                                                           List<ModalityProcessStatus> statuses, @RequestParam(required = false)
                                                           String name) {
        return ResponseEntity.ok(modalityListingService.getAllStudentModalitiesForProgramCurriculumCommittee(statuses, name));
    }

    @GetMapping("/students/director")
    @PreAuthorize("hasAuthority('PERM_VIEW_MODALITY')")
    public ResponseEntity<List<ModalityListDTO>> listAllModalitiesForProjectDirector(@RequestParam(required = false)
                                                                 List<ModalityProcessStatus> statuses,
                                                                 @RequestParam(required = false) String name) {
        return ResponseEntity.ok(modalityListingService.getAllStudentModalitiesForProjectDirector(statuses, name));
    }

    @GetMapping("/students/examiner")
    @PreAuthorize("hasAuthority('PERM_VIEW_EXAMINER_MODALITIES')")
    public ResponseEntity<List<ModalityListDTO>> listAllModalitiesForExaminer(@RequestParam(required = false)
                                                          List<ModalityProcessStatus> statuses,
                                                          @RequestParam(required = false) String name) {
        return ResponseEntity.ok(modalityListingService.getAllStudentModalitiesForExaminer(statuses, name));
    }

    @GetMapping("/students/{studentModalityId}")
    @PreAuthorize("hasAuthority('PERM_VIEW_ALL_MODALITIES')")
    public ResponseEntity<StudentModalityDTO> getModalityDetailForProgramHead(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(modalityListingService.getStudentModalityDetailForProgramHead(studentModalityId));
    }

    @GetMapping("/students/{studentModalityId}/committee")
    @PreAuthorize("hasAuthority('PERM_VIEW_ALL_MODALITIES')")
    public ResponseEntity<StudentModalityDTO> getModalityDetailForCommittee(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(modalityListingService.getStudentModalityDetailForCommittee(studentModalityId));
    }

    @GetMapping("/students/{studentModalityId}/director")
    @PreAuthorize("hasAuthority('PERM_VIEW_MODALITY')")
    public ResponseEntity<StudentModalityDTO> getModalityDetailForProjectDirector(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(modalityListingService.getStudentModalityDetailForProjectDirector(studentModalityId));
    }

    @GetMapping("/students/{studentModalityId}/examiner")
    @PreAuthorize("hasAuthority('PERM_VIEW_EXAMINER_MODALITIES')")
    public ResponseEntity<StudentModalityExaminerDTO> getModalityDetailForExaminer(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(modalityListingService.getStudentModalityDetailForExaminer(studentModalityId));
    }

    @PostMapping("/{studentModalityId}/cancellation/director/approve")
    @PreAuthorize("hasAuthority('PERM_APPROVE_CANCELLATION_DIRECTOR')")
    public ResponseEntity<Map<String, Object>> approveModalityCancellationByDirector(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(cancellationService.approveModalityCancellationByDirector(studentModalityId));
    }

    @PostMapping("/{studentModalityId}/cancellation/director/reject")
    @PreAuthorize("hasAuthority('PERM_APPROVE_CANCELLATION_DIRECTOR')")
    public ResponseEntity<Map<String, Object>> rejectModalityCancellationByDirector(
            @PathVariable Long studentModalityId,
            @Valid @RequestBody ReasonRequest body
    ) {
        String reason = body.getReason();
        return ResponseEntity.ok(cancellationService.rejectModalityCancellationByDirector(studentModalityId, reason));
    }

    @PostMapping("/{studentModalityId}/cancellation/approve")
    @PreAuthorize("hasAuthority('PERM_APPROVE_CANCELLATION')")
    public ResponseEntity<Map<String, Object>> approveCancellation(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(cancellationService.approveCancellation(studentModalityId));
    }

    @PostMapping("/{studentModalityId}/cancellation/reject")
    @PreAuthorize("hasAuthority('PERM_REJECT_CANCELLATION')")
    public ResponseEntity<Map<String, Object>> rejectCancellation(@PathVariable Long studentModalityId, @Valid @RequestBody ReasonRequest body
    ) {
        return ResponseEntity.ok(cancellationService.rejectCancellation(studentModalityId, body.getReason()));
    }

    @GetMapping("/cancellation-request")
    @PreAuthorize("hasAuthority('PERM_VIEW_CANCELLATIONS')")
    public ResponseEntity<List<CancellationList>> getPendingCancellations() {

        List<CancellationList> cancellations =
                cancellationService.getPendingCancellations();

        return ResponseEntity.ok(cancellations);
    }

    @GetMapping("/cancellation/document/{studentModalityId}")
    @PreAuthorize("hasAuthority('PERM_VIEW_CANCELLATIONS')")
    public ResponseEntity<Resource> getCancellationDocument(@PathVariable Long studentModalityId) throws MalformedURLException {

        StudentDocument document = documentService.getDocumentCancellation(studentModalityId, SecurityUtils.getCurrentUser());

        Resource resource = cancellationService.getCancellationDocumentResource(document);

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + document.getFileName() + "\""
                )
                .body(resource);
    }

    @PostMapping("/{studentModalityId}/assign-director/{directorId}")
    @PreAuthorize("hasAuthority('PERM_ASSIGN_PROJECT_DIRECTOR')")
    public ResponseEntity<Map<String, Object>> assignProjectDirector(@PathVariable Long studentModalityId, @PathVariable Long directorId) {
        return ResponseEntity.ok(cancellationService.assignProjectDirector(studentModalityId, directorId));
    }


    @PutMapping("/{studentModalityId}/change-director")
    @PreAuthorize("hasAuthority('PERM_ASSIGN_PROJECT_DIRECTOR')")
    public ResponseEntity<Map<String, Object>> changeProjectDirector(@PathVariable Long studentModalityId, @RequestBody @Valid ChangeDirectorDTO request) {
        return ResponseEntity.ok(cancellationService.changeProjectDirector(studentModalityId, request.getNewDirectorId(), request.getReason()));
    }

    @PostMapping("/{studentModalityId}/propose-defense-director")
    @PreAuthorize("hasAuthority('PERM_PROPOSE_DEFENSE')")
    public ResponseEntity<Map<String, Object>> proposeDefenseByDirector(@PathVariable Long studentModalityId, @Valid @RequestBody ScheduleDefenseDTO request) {
        return ResponseEntity.ok(defenseModalityService.scheduleDefense(studentModalityId, request));
    }


    @GetMapping("/defense-proposals/pending")
    @PreAuthorize("hasAuthority('PERM_SCHEDULE_DEFENSE')")
    public ResponseEntity<Map<String, Object>> getPendingDefenseProposals() {
        return ResponseEntity.ok(defenseModalityService.getPendingDefenseProposals());
    }

    @PostMapping("/{studentModalityId}/defense-proposals/approve")
    @PreAuthorize("hasAuthority('PERM_SCHEDULE_DEFENSE')")
    public ResponseEntity<Map<String, Object>> approveDefenseProposal(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseModalityService.approveDefenseProposal(studentModalityId));
    }

    @PostMapping("/{studentModalityId}/defense-proposals/reschedule")
    @PreAuthorize("hasAuthority('PERM_SCHEDULE_DEFENSE')")
    public ResponseEntity<Map<String, Object>> rescheduleDefense(@PathVariable Long studentModalityId, @Valid @RequestBody ScheduleDefenseDTO request) {
        return ResponseEntity.ok(defenseModalityService.rescheduleDefense(studentModalityId, request));
    }

    @PostMapping("/{studentModalityId}/examiners/assign")
    @PreAuthorize("hasAuthority('PERM_SCHEDULE_DEFENSE')")
    public ResponseEntity<Map<String, Object>> assignExaminers(@PathVariable Long studentModalityId, @Valid @RequestBody ScheduleDefenseDTO request) {
        return ResponseEntity.ok(defenseModalityService.assignExaminers(studentModalityId, request));
    }

    @PostMapping("/{studentModalityId}/final-evaluation/register")
    @PreAuthorize("hasAuthority('PERM_EVALUATE_DEFENSE')")
    public ResponseEntity<Map<String, Object>> registerFinalDefenseEvaluation(
            @PathVariable Long studentModalityId,
            @Valid @RequestBody ExaminerEvaluationDTO evaluationDTO) {
        return ResponseEntity.ok(defenseModalityService.registerFinalDefenseEvaluation(studentModalityId, evaluationDTO));
    }

    @GetMapping("/project-directors")
    @PreAuthorize("hasAuthority('PERM_VIEW_PROJECT_DIRECTOR')")
    public ResponseEntity<List<ProjectDirectorResponse>> getProjectDirectors() {
        return ResponseEntity.ok(modalityCatalogService.getProjectDirectors());
    }

    @GetMapping("/program-heads")
    @PreAuthorize("hasAuthority('PERM_VIEW_PROGRAM_HEAD')")
    public ResponseEntity<List<ProjectDirectorResponse>> getProgramHeads() {
        return ResponseEntity.ok(modalityCatalogService.getProgramHeads());
    }

    @GetMapping("/committee")
    @PreAuthorize("hasAuthority('PERM_VIEW_COMMITTEE')")
    public ResponseEntity<List<ProjectDirectorResponse>> getProgramCurriculumCommittee(
            @RequestParam(required = false) Long academicProgramId,
            @RequestParam(required = false) Long facultyId
    ) {
        return ResponseEntity.ok(modalityCatalogService.getProgramCurriculumCommittee(academicProgramId, facultyId));
    }

    @GetMapping("/examiners")
    @PreAuthorize("hasAuthority('PERM_VIEW_COMMITTEE')")
    public ResponseEntity<List<ProjectDirectorResponse>> getExaminers(
            @RequestParam(required = false) Long academicProgramId,
            @RequestParam(required = false) Long facultyId
    ) {
        return ResponseEntity.ok(modalityCatalogService.getExaminers(academicProgramId, facultyId));
    }

    @GetMapping("/examiners/for-committee")
    @PreAuthorize("hasAuthority('PERM_VIEW_EXAMINER')")
    public ResponseEntity<List<ProjectDirectorResponse>> getExaminersForCommittee() {
        return ResponseEntity.ok(modalityCatalogService.getExaminersForCommittee());
    }


    @PreAuthorize("hasAuthority('PERM_VIEW_FINAL_DEFENSE_RESULT')")
    @GetMapping("/final-evaluation/{studentModalityId}/result")
    public ResponseEntity<FinalDefenseResponse> getFinalDefenseResult(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseModalityService.getFinalDefenseResult(studentModalityId));
    }

    @GetMapping("/final-evaluation/my-result")
    public ResponseEntity<Object> getMyFinalDefenseResult() {
        return ResponseEntity.ok(defenseModalityService.getMyFinalDefenseResult());
    }


    @PostMapping("/{studentModalityId}/documents/{documentId}/resubmit-correction")
    public ResponseEntity<Map<String, Object>> resubmitCorrectedDocument(
            @PathVariable Long studentModalityId,
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(documentModalityService.resubmitCorrectedDocument(studentModalityId, documentId, file));
    }

    @PostMapping("/documents/{documentId}/approve-correction")
    @PreAuthorize("hasAuthority('PERM_REVIEW_DOCUMENTS')")
    public ResponseEntity<Map<String, Object>> approveCorrectedDocument(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentModalityService.approveCorrectedDocument(documentId));
    }

    @PostMapping("/documents/{documentId}/reject-correction-final")
    @PreAuthorize("hasAuthority('PERM_REVIEW_DOCUMENTS')")
    public ResponseEntity<Map<String, Object>> rejectCorrectedDocumentFinal(
            @PathVariable Long documentId,
            @Valid @RequestBody ReasonRequest request) {
        String reason = request.getReason();
        return ResponseEntity.ok(documentModalityService.rejectCorrectedDocumentFinal(documentId, reason));
    }

    @GetMapping("/{studentModalityId}/correction-deadline-status")
    public ResponseEntity<Map<String, Object>> getCorrectionDeadlineStatus(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentModalityService.getCorrectionDeadlineStatus(studentModalityId));
    }


    @PostMapping("/{studentModalityId}/close-by-committee")
    @PreAuthorize("hasAuthority('PERM_APPROVE_CANCELLATION') or hasAuthority('PERM_REVIEW_DOCUMENT_COMMITTEE')")
    public ResponseEntity<Map<String, Object>> closeModalityByCommittee(
            @PathVariable Long studentModalityId,
            @Valid @RequestBody ReasonRequest request
    ) {
        String reason = request.getReason();
        return ResponseEntity.ok(documentModalityService.closeModalityByCommittee(studentModalityId, reason));
    }


    @PostMapping("/{studentModalityId}/approve-final-by-committee")
    @PreAuthorize("hasAuthority('PERM_APPROVE_MODALITY_BY_COMMITTEE')")
    public ResponseEntity<Map<String, Object>> approveFinalModalityByCommittee(@PathVariable Long studentModalityId, @RequestBody(required = false) Map<String, String> request) {
        String observations = request != null ? request.get("observations") : null;
        return ResponseEntity.ok(documentModalityService.approveFinalModalityByCommittee(studentModalityId, observations));
    }

    @PostMapping("/{studentModalityId}/reject-final-by-committee")
    @PreAuthorize("hasAuthority('PERM_REJECT_MODALITY_BY_COMMITTEE')")
    public ResponseEntity<Map<String, Object>> rejectFinalModalityByCommittee(@PathVariable Long studentModalityId, @Valid @RequestBody ReasonRequest request) {
        String reason = request.getReason();
        return ResponseEntity.ok(documentModalityService.rejectFinalModalityByCommittee(studentModalityId, reason));
    }


    @PostMapping("/seminar/create")
    @PreAuthorize("hasAuthority('PERM_CREATE_SEMINAR')")
    public ResponseEntity<Map<String, Object>> createSeminar(@Valid @RequestBody SeminarDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seminarModalityService.createSeminar(request));
    }

    @GetMapping("/seminar/{seminarId}/detail")
    public ResponseEntity<Map<String, Object>> getSeminarDetail(@PathVariable Long seminarId) {
        return ResponseEntity.ok(seminarModalityService.FgetSeminarDetailForProgramHead(seminarId));
    }

    @GetMapping("/seminar/available")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<Map<String, Object>> listActiveSeminarsWithSeats() {
        return ResponseEntity.ok(seminarModalityService.listActiveSeminarsWithSeats());
    }


    @PostMapping("/seminar/{seminarId}/enroll")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<Map<String, Object>> enrollInSeminar(@PathVariable Long seminarId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seminarModalityService.enrollInSeminar(seminarId));
    }


    @GetMapping("/seminars")
    @PreAuthorize("hasAuthority('PERM_CREATE_SEMINAR')")
    public ResponseEntity<Map<String, Object>> listSeminars(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(seminarModalityService.listSeminarsForProgramHead(status, active));
    }


    @PostMapping("/seminar/{seminarId}/start")
    @PreAuthorize("hasAuthority('PERM_CREATE_SEMINAR')")
    public ResponseEntity<Map<String, Object>> startSeminar(@PathVariable Long seminarId) {
        return ResponseEntity.ok(seminarModalityService.startSeminar(seminarId));
    }

    @PostMapping("/seminar/{seminarId}/cancel")
    @PreAuthorize("hasAuthority('PERM_CREATE_SEMINAR')")
    public ResponseEntity<Map<String, Object>> cancelSeminar(@PathVariable Long seminarId, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(seminarModalityService.cancelSeminar(seminarId, reason));
    }

    @PutMapping("/seminar/{seminarId}")
    @PreAuthorize("hasAuthority('PERM_CREATE_SEMINAR')")
    public ResponseEntity<Map<String, Object>> updateSeminar(@PathVariable Long seminarId, @Valid @RequestBody SeminarDTO request) {
        return ResponseEntity.ok(seminarModalityService.updateSeminar(seminarId, request));
    }

    @PostMapping("/seminar/{seminarId}/close-registrations")
    @PreAuthorize("hasAuthority('PERM_CREATE_SEMINAR')")
    public ResponseEntity<Map<String, Object>> closeRegistrations(@PathVariable Long seminarId) {
        return ResponseEntity.ok(seminarModalityService.closeRegistrations(seminarId));
    }

    @PostMapping("/seminar/{seminarId}/complete")
    @PreAuthorize("hasAuthority('PERM_CREATE_SEMINAR')")
    public ResponseEntity<Map<String, Object>> completeSeminar(@PathVariable Long seminarId) {
        return ResponseEntity.ok(seminarModalityService.completeSeminar(seminarId));
    }

    @PostMapping("/{studentModalityId}/ready-for-defense")
    @PreAuthorize("hasAuthority('PERM_PROPOSE_DEFENSE')")
    public ResponseEntity<Map<String, Object>> modalityReadyForDefenseByDirector(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseModalityService.modalityReadyForDefenseByDirector(studentModalityId));
    }

    @PostMapping("/{studentModalityId}/program-head/approve-final-and-notify-examiners")
    @PreAuthorize("hasAuthority('PERM_APPROVE_MODALITY')")
    public ResponseEntity<Map<String, Object>> programHeadApprovesAndNotifiesExaminers(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseModalityService.programHeadApprovesAndNotifiesExaminers(studentModalityId));
    }

    @PostMapping("/{studentModalityId}/final-review-completed")
    @PreAuthorize("hasAuthority('PERM_APPROVE_MODALITY_BY_EXAMINER')")
    public ResponseEntity<Map<String, Object>> examinerFinalReviewCompleted(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseModalityService.examinerFinalReviewCompleted(studentModalityId));
    }


    @GetMapping("/{studentModalityId}/examiner-evaluation")
    @PreAuthorize("hasAuthority('PERM_VIEW_EXAMINER_MODALITIES')")
    public ResponseEntity<Map<String, Object>> getFinalDefenseEvaluationForExaminer(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseModalityService.getFinalDefenseEvaluationForExaminer(studentModalityId));
    }

    /**
     * Endpoint para que el jurado autenticado obtenga su calendario de próximas sustentaciones.
     * Solo incluye modalidades en estado DEFENSE_SCHEDULED, ordenadas por fecha de defensa ascendente.
     */
    @GetMapping("/examiner/defense-calendar")
    @PreAuthorize("hasAuthority('PERM_VIEW_EXAMINER_MODALITIES')")
    public ResponseEntity<List<ModalityListDTO>> getExaminerDefenseCalendar() {
        return ResponseEntity.ok(defenseModalityService.getExaminerDefenseCalendar());
    }

    @GetMapping("/examiner-type/{studentModalityId}")
    @PreAuthorize("hasAuthority('PERM_VIEW_EXAMINER_MODALITIES')")
    public ResponseEntity<Map<String, Object>> getExaminerTypeForModality(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseModalityService.getExaminerTypeForModality(studentModalityId));
    }

    @GetMapping("/examiner-evaluation/{studentModalityId}")
    @PreAuthorize("hasAuthority('PERM_VIEW_EXAMINER_MODALITIES')")
    public ResponseEntity<Map<String, Object>> getExaminerEvaluationForModality(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseModalityService.getExaminerEvaluationForModality(studentModalityId));
    }

    /**
     * El jurado autenticado obtiene su veredicto sobre documentos MANDATORY de propuesta.
     * Devuelve la decisión individual del jurado, notas y evaluación de propuesta (si aplica).
     * Ruta: GET /modalities/documents/{studentDocumentId}/examiner-proposal-evaluation
     */
    @GetMapping("/documents/{studentDocumentId}/examiner-proposal-evaluation")

    public ResponseEntity<Object> getMyProposalEvaluation(@PathVariable Long studentDocumentId) {
        return ResponseEntity.ok(documentEditRequestService.getMyProposalEvaluation(studentDocumentId));
    }

    @GetMapping("/documents/{studentDocumentId}/examiner-final-evaluation")

    public ResponseEntity<Object> getMyFinalDocumentEvaluation(@PathVariable Long studentDocumentId) {
        return ResponseEntity.ok(documentEditRequestService.getMyFinalDocumentEvaluation(studentDocumentId));
    }

    // =========================================================================
    // SOLICITUD DE EDICIÓN DE PROPUESTA APROBADA
    // =========================================================================

    /**
     * El estudiante solicita editar un documento MANDATORY que ya fue aprobado por los jurados.
     * Body: { "reason": "motivo justificado de la solicitud (mínimo 20 caracteres)" }
     */
    @PostMapping("/documents/{studentDocumentId}/request-edit")
    public ResponseEntity<Map<String, Object>> requestDocumentEdit(
            @PathVariable Long studentDocumentId,
            @Valid @RequestBody com.SIGMA.USCO.documents.dto.DocumentEditRequestDTO request) {
        return ResponseEntity.ok(documentEditRequestService.requestDocumentEdit(studentDocumentId, request));
    }

    /**
     * Un jurado vota sobre una solicitud de edición de documento.
     * Sigue la lógica de consenso: ambos jurados primarios deben votar;
     * si hay desacuerdo, el jurado de desempate decide.
     * Body: { "approved": true|false, "resolutionNotes": "..." }
     */
    @PostMapping("/document-edit-requests/{editRequestId}/resolve")
    @PreAuthorize("hasAuthority('PERM_REVIEW_DOCUMENTS')")
    public ResponseEntity<Map<String, Object>> resolveDocumentEditRequest(
            @PathVariable Long editRequestId,
            @Valid @RequestBody com.SIGMA.USCO.documents.dto.DocumentEditResolutionDTO request) {
        return ResponseEntity.ok(documentEditRequestService.resolveDocumentEditRequest(editRequestId, request));
    }

    /**
     * El jurado autenticado obtiene las solicitudes de edición pendientes de una modalidad.
     * Los jurados primarios ven las PENDING; el jurado de desempate ve las TIEBREAKER_REQUIRED.
     */
    @GetMapping("/{studentModalityId}/document-edit-requests/pending")
    @PreAuthorize("hasAuthority('PERM_VIEW_EXAMINER_MODALITIES')")
    public ResponseEntity<Map<String, Object>> getPendingEditRequestsForExaminer(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentEditRequestService.getPendingEditRequestsForExaminer(studentModalityId));
    }

    /**
     * El jurado autenticado obtiene TODAS las solicitudes de edición de documentos
     * de una modalidad (todos los estados: pendiente, desempate, aprobado, rechazado).
     * Incluye información completa: documento, solicitante, votos de cada jurado,
     * si el jurado autenticado ya votó, si puede votar y el resultado final.
     */
    @GetMapping("/{studentModalityId}/document-edit-requests/all")
    @PreAuthorize("hasAuthority('PERM_VIEW_EXAMINER_MODALITIES')")
    public ResponseEntity<Map<String, Object>> getAllEditRequestsForExaminer(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentEditRequestService.getAllEditRequestsForExaminer(studentModalityId));
    }

    // =========================================================================
    // ENDPOINTS GET PARA EL ESTUDIANTE – SOLICITUDES DE EDICIÓN
    // =========================================================================

    /**
     * El estudiante autenticado obtiene TODAS sus solicitudes de edición de documentos
     * en todas sus modalidades, con el estado de votación de cada una.
     */
    @GetMapping("/my-document-edit-requests")
    public ResponseEntity<Map<String, Object>> getMyDocumentEditRequests() {
        return ResponseEntity.ok(documentEditRequestService.getMyDocumentEditRequests());
    }

    /**
     * El estudiante autenticado obtiene todas las solicitudes de edición asociadas
     * a una modalidad específica (por studentModalityId).
     */
    @GetMapping("/{studentModalityId}/my-document-edit-requests")
    public ResponseEntity<Map<String, Object>> getMyDocumentEditRequestsByModality(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentEditRequestService.getMyDocumentEditRequestsByModality(studentModalityId));
    }

    /**
     * El estudiante autenticado obtiene el detalle de una solicitud de edición específica.
     */
    @GetMapping("/document-edit-requests/{editRequestId}")
    public ResponseEntity<Map<String, Object>> getDocumentEditRequestDetail(@PathVariable Long editRequestId) {
        return ResponseEntity.ok(documentEditRequestService.getDocumentEditRequestDetail(editRequestId));
    }

    /**
     * Obtiene la lista de jurados (examinadores) asociados a una modalidad específica.
     * Retorna información detallada de cada jurado: ID, nombre, email, tipo (primario 1, primario 2, desempate)
     * y fecha de asignación.
     * Ruta: GET /modalities/{studentModalityId}/examiners
     */
    @GetMapping("/{studentModalityId}/examiners")
    @PreAuthorize("hasAuthority('PERM_VIEW_EXAMINER_MODALITIES')")
    public ResponseEntity<Map<String, Object>> getExaminersForModality(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentEditRequestService.getExaminersForModality(studentModalityId));
    }

    /**
     * Retorna la lista completa de todos los estudiantes que pertenecen al
     * programa académico del comité autenticado, con filtro opcional por nombre.
     * <p>
     * GET /modalities/committee/program-students?studentName=raul
     */
    @GetMapping("/committee/program-students")
    @PreAuthorize("hasAuthority('PERM_STUDENT_LIST')")
    public ResponseEntity<Map<String, Object>> getProgramStudentsForCommittee(@RequestParam(required = false) String studentName) {
        return ResponseEntity.ok(modalityListingService.getProgramStudentsForCommittee(studentName));
    }

    // =========================================================================
    // GESTIÓN DE DISTINCIONES HONORÍFICAS PROPUESTAS POR JURADOS
    // =========================================================================

    /**
     * Lista todas las modalidades donde los jurados han propuesto unánimemente
     * una distinción honorífica (Meritoria o Laureada) pendiente de revisión
     * por el Comité de Currículo.
     * <p>
     * Incluye los argumentos de cada jurado para que el comité pueda evaluarlos.
     * <p>
     * GET /modalities/committee/pending-distinction-proposals
     */
    @GetMapping("/committee/pending-distinction-proposals")
    @PreAuthorize("hasAuthority('PERM_APPROVE_MODALITY')")
    public ResponseEntity<Map<String, Object>> getPendingDistinctionProposals() {
        return ResponseEntity.ok(defenseModalityService.getPendingDistinctionProposals());
    }

    /**
     * El Comité de Currículo ACEPTA la distinción honorífica propuesta por los jurados.
     * La modalidad pasa a estado GRADED_APPROVED con la distinción confirmada.
     * <p>
     * Body (opcional): { "notes": "Observaciones del comité al aceptar" }
     * <p>
     * POST /modalities/{studentModalityId}/committee/accept-distinction
     */
    @PostMapping("/{studentModalityId}/committee/accept-distinction")
    @PreAuthorize("hasAuthority('PERM_APPROVE_MODALITY')")
    public ResponseEntity<Map<String, Object>> acceptDistinctionProposal(
            @PathVariable Long studentModalityId,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(defenseModalityService.acceptDistinctionProposal(studentModalityId, notes));
    }

    /**
     * El Comité de Currículo RECHAZA la distinción honorífica propuesta por los jurados.
     * La modalidad pasa a estado GRADED_APPROVED sin mención especial.
     * <p>
     * Body: { "reason": "Razón del rechazo (obligatorio)" }
     * <p>
     * POST /modalities/{studentModalityId}/committee/reject-distinction
     */
    @PostMapping("/{studentModalityId}/committee/reject-distinction")
    @PreAuthorize("hasAuthority('PERM_APPROVE_MODALITY')")
    public ResponseEntity<Map<String, Object>> rejectDistinctionProposal(
            @PathVariable Long studentModalityId,
            @Valid @RequestBody ReasonRequest body) {
        String reason = body.getReason();
        return ResponseEntity.ok(defenseModalityService.rejectDistinctionProposal(studentModalityId, reason));
    }

}