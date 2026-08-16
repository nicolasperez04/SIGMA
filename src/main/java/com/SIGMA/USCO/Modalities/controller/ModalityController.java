package com.SIGMA.USCO.Modalities.controller;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.dto.*;
import com.SIGMA.USCO.Modalities.dto.request.DistinctionDecisionRequest;
import com.SIGMA.USCO.Modalities.dto.response.*;
import com.SIGMA.USCO.Modalities.service.CancellationService;
import com.SIGMA.USCO.Modalities.service.DefenseEvaluationService;
import com.SIGMA.USCO.Modalities.service.DefenseWorkflowService;
import com.SIGMA.USCO.Modalities.service.DocumentEditRequestService;
import com.SIGMA.USCO.Modalities.service.DocumentWorkflowService;
import com.SIGMA.USCO.Modalities.service.ModalityCatalogService;
import com.SIGMA.USCO.Modalities.service.ModalityDocumentService;
import com.SIGMA.USCO.Modalities.service.StudentModalityListingService;
import com.SIGMA.USCO.Modalities.service.SeminarModalityService;
import com.SIGMA.USCO.common.security.Permissions;
import com.SIGMA.USCO.common.security.Roles;
import com.SIGMA.USCO.common.web.OperationResultResponse;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.service.DocumentService;
import com.SIGMA.USCO.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
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
    private final DefenseEvaluationService defenseEvaluationService;
    private final DefenseWorkflowService defenseWorkflowService;
    private final DocumentWorkflowService documentWorkflowService;
    private final ModalityDocumentService modalityDocumentService;
    private final DocumentEditRequestService documentEditRequestService;
    private final DocumentService documentService;

    @Operation(summary = "Crear modalidad de grado", description = "Crea una nueva modalidad de grado en el sistema. El administrador define el tipo, requisitos y configuración específica de la modalidad.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Modalidad creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: falta información requerida"),
            @ApiResponse(responseCode = "409", description = "Ya existe una modalidad con ese nombre en esta facultad"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado: necesita PERM_CREATE_MODALITY o PERM_UPDATE_MODALITY"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor al crear la modalidad")
    })
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_MODALITY + "') or hasAuthority('" + Permissions.PERM_UPDATE_MODALITY + "')")
    public ResponseEntity<ModalityDTO> createModality(@RequestBody @Valid ModalityDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(modalityCatalogService.createModality(request));
    }

    @Operation(summary = "Actualizar configuración de modalidad", description = "Modifica los parámetros de una modalidad existente: requisitos, plazos, condiciones de aprobación, etc.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modalidad actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado")
    })
    @PutMapping("/update/{modalityId}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_MODALITY + "') or hasAuthority('" + Permissions.PERM_UPDATE_MODALITY + "')")
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
    @PreAuthorize("hasAuthority('" + Permissions.PERM_DESACTIVE_MODALITY + "')")
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
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_MODALITY + "') or hasAuthority('" + Permissions.PERM_UPDATE_MODALITY + "')")
    public ResponseEntity<String> createModalityRequirements(@PathVariable Long modalityId, @RequestBody @Valid List<@Valid RequirementDTO> requirements) {
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
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_MODALITY + "') or hasAuthority('" + Permissions.PERM_UPDATE_MODALITY + "')")
    public ResponseEntity<String> updateRequirement(@PathVariable Long modalityId, @PathVariable Long requirementId, @RequestBody @Valid RequirementDTO request) {
        modalityCatalogService.updateModalityRequirement(modalityId, requirementId, request);
        return ResponseEntity.ok("Requisito actualizado correctamente");
    }


    @Operation(summary = "Obtener requisitos de modalidad", description = "Retorna la lista de requisitos documentales definidos para una modalidad, con opción de filtrar por estado (activo/inactivo).")
    @ApiResponse(responseCode = "200", description = "Lista de requisitos obtenida")
    @GetMapping("/{modalityId}/requirements")
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("hasAuthority('" + Permissions.PERM_DELETE_MODALITY_REQUIREMENT + "')")
    public ResponseEntity<String> desactiveRequirements(@PathVariable Long requirementId) {
        return ResponseEntity.ok(modalityCatalogService.deleteRequirement(requirementId));
    }

    @PutMapping("/requirements/active/{requirementId}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_DELETE_MODALITY_REQUIREMENT + "')")
    public ResponseEntity<String> activeRequirements(@PathVariable Long requirementId) {
        return ResponseEntity.ok(modalityCatalogService.activeRequirement(requirementId));
    }


    @Operation(summary = "Obtener todas las modalidades", description = "Retorna una lista completa de todas las modalidades de grado disponibles en el sistema, incluyendo activas e inactivas.")
    @ApiResponse(responseCode = "200", description = "Lista completa de modalidades obtenida")
    @GetMapping
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "')")
    public ResponseEntity<List<ModalityDTO>> getAllModalities() {
        return ResponseEntity.ok(modalityCatalogService.getAllModalities(SecurityUtils.getCurrentUser()));
    }

    @Operation(summary = "Obtener detalle de modalidad", description = "Retorna la información completa de una modalidad específica: configuración, requisitos, estudiantes inscritos, estado actual.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalle de modalidad obtenido"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "') or hasAnyRole('" + Roles.ROLE_PROJECT_DIRECTOR + "','" + Roles.ROLE_PROGRAM_HEAD + "','" + Roles.ROLE_PROGRAM_CURRICULUM_COMMITTEE + "','" + Roles.ROLE_EXAMINER + "')")
    public ResponseEntity<ModalityDTO> getModalityById(@PathVariable Long id) {
        return ResponseEntity.ok(modalityCatalogService.getModalityDetail(id, SecurityUtils.getCurrentUser()));
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
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "') or hasRole('" + Roles.ROLE_PROJECT_DIRECTOR + "')")
    public ResponseEntity<UploadDocumentResponse> uploadDocument(
            @PathVariable Long studentModalityId,
            @PathVariable Long requiredDocumentId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {

        return ResponseEntity.ok(modalityDocumentService.uploadRequiredDocument(
                studentModalityId,
                requiredDocumentId,
                file,
                SecurityUtils.getCurrentUser()
        ));


    }


    @PostMapping("/{modalityId}/start")
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "')")
    public ResponseEntity<StartGroupModalityResponse> startModality(@PathVariable Long modalityId) {
        return ResponseEntity.ok(documentWorkflowService.startStudentModalityIndividual(modalityId, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/{id}/validate-documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ValidateAllDocumentsUploadedResponse> validateDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(modalityDocumentService.validateAllDocumentsUploaded(id, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/my-available-documents")
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "')")
    public ResponseEntity<AvailableDocumentsResponse> getMyAvailableDocuments() {
        return ResponseEntity.ok(modalityDocumentService.getAvailableDocumentsForStudent(SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/{studentModalityId}/documents")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_REVIEW_DOCUMENTS + "')")
    public ResponseEntity<List<StudentDocumentResponse>> listStudentDocuments(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(modalityDocumentService.getStudentDocuments(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/student/{studentDocumentId}/view")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_DOCUMENTS + "')")
    public ResponseEntity<Resource> viewStudentDocument(@PathVariable Long studentDocumentId) throws MalformedURLException {
        Resource resource = modalityDocumentService.viewStudentDocument(studentDocumentId, SecurityUtils.getCurrentUser());
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PutMapping("/documents/{studentDocumentId}/review")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_REVIEW_DOCUMENTS + "')")
    public ResponseEntity<ReviewStudentDocumentResponse> reviewDocument(@PathVariable Long studentDocumentId, @Valid @RequestBody DocumentReviewDTO request) {
        return ResponseEntity.ok(documentWorkflowService.reviewStudentDocument(studentDocumentId, request, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/approve-program-head")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_MODALITY + "')")
    public ResponseEntity<ApproveModalityResponse> approveByProgramHead(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentWorkflowService.approveModalityByProgramHead(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/approve-committee")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_MODALITY + "')")
    public ResponseEntity<ApproveModalityResponse> approveByCommittee(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentWorkflowService.approveModalityByCommittee(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/approve-examiners")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_MODALITY_BY_EXAMINER + "')")
    public ResponseEntity<ApproveModalityResponse> approveByExaminer(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentWorkflowService.approveModalityByExaminers(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/documents/{studentDocumentId}/review-committee")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_REVIEW_DOCUMENTS + "')")
    public ResponseEntity<CommitteeDocumentReviewResponse> reviewDocumentCommittee(@PathVariable Long studentDocumentId, @Valid @RequestBody DocumentReviewDTO request) {
        return ResponseEntity.ok(documentWorkflowService.reviewStudentDocumentByCommittee(studentDocumentId, request, SecurityUtils.getCurrentUser()));
    }

    @PutMapping("/documents/{studentDocumentId}/review-examiner")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_REVIEW_DOCUMENTS + "')")
    public ResponseEntity<Object> reviewDocumentExaminer(@PathVariable Long studentDocumentId, @Valid @RequestBody DocumentReviewDTO request) {
        return ResponseEntity.ok(documentWorkflowService.reviewStudentDocumentByExaminer(studentDocumentId, request, SecurityUtils.getCurrentUser()));
    }

    @PutMapping("/documents/{studentDocumentId}/review-examiner-final-document")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_REVIEW_DOCUMENTS + "')")
    // ponytail: diccionario dinámico legítimo — el body es un merge de la respuesta de consenso del
    // jurado (forma variable por rama) + secondaryEvaluation/finalEvaluation/traceability.
    @io.swagger.v3.oas.annotations.media.Schema(description = "Respuesta variable según consenso de jurados: claves del consenso + secondaryEvaluation, finalEvaluation, currentModalityStatus, traceability")
    public ResponseEntity<Map<String, Object>> reviewSecondaryDocumentExaminer(@PathVariable Long studentDocumentId, @Valid @RequestBody DocumentReviewDTO request) {
        return ResponseEntity.ok(documentWorkflowService.reviewFinalDocumentByExaminer(studentDocumentId, request, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/students")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_ALL_MODALITIES + "')")
    public ResponseEntity<List<ModalityListDTO>> listAllModalitiesForProgramHead(@RequestParam(required = false)
                                                             List<ModalityProcessStatus> statuses, @RequestParam(required = false)
                                                             String name) {
        return ResponseEntity.ok(modalityListingService.getAllStudentModalitiesForProgramHead(statuses, name, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/students/committee")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_ALL_MODALITIES + "')")
    public ResponseEntity<List<ModalityListDTO>> listAllModalitiesForCommittee(@RequestParam(required = false)
                                                           List<ModalityProcessStatus> statuses, @RequestParam(required = false)
                                                           String name) {
        return ResponseEntity.ok(modalityListingService.getAllStudentModalitiesForProgramCurriculumCommittee(statuses, name, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/students/director")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_MODALITY + "')")
    public ResponseEntity<List<ModalityListDTO>> listAllModalitiesForProjectDirector(@RequestParam(required = false)
                                                                 List<ModalityProcessStatus> statuses,
                                                                 @RequestParam(required = false) String name) {
        return ResponseEntity.ok(modalityListingService.getAllStudentModalitiesForProjectDirector(statuses, name, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/students/examiner")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_EXAMINER_MODALITIES + "')")
    public ResponseEntity<List<ModalityListDTO>> listAllModalitiesForExaminer(@RequestParam(required = false)
                                                          List<ModalityProcessStatus> statuses,
                                                          @RequestParam(required = false) String name) {
        return ResponseEntity.ok(modalityListingService.getAllStudentModalitiesForExaminer(statuses, name, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/students/{studentModalityId}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_ALL_MODALITIES + "')")
    public ResponseEntity<StudentModalityDTO> getModalityDetailForProgramHead(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(modalityListingService.getStudentModalityDetailForProgramHead(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/students/{studentModalityId}/committee")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_ALL_MODALITIES + "')")
    public ResponseEntity<StudentModalityDTO> getModalityDetailForCommittee(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(modalityListingService.getStudentModalityDetailForCommittee(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/students/{studentModalityId}/director")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_MODALITY + "')")
    public ResponseEntity<StudentModalityDTO> getModalityDetailForProjectDirector(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(modalityListingService.getStudentModalityDetailForProjectDirector(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/students/{studentModalityId}/examiner")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_EXAMINER_MODALITIES + "')")
    public ResponseEntity<StudentModalityExaminerDTO> getModalityDetailForExaminer(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(modalityListingService.getStudentModalityDetailForExaminer(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/cancellation/director/approve")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_CANCELLATION_DIRECTOR + "')")
    public ResponseEntity<OperationResultResponse> approveModalityCancellationByDirector(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(cancellationService.approveModalityCancellationByDirector(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/cancellation/director/reject")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_CANCELLATION_DIRECTOR + "')")
    public ResponseEntity<CancellationRejectedByDirectorResponse> rejectModalityCancellationByDirector(
            @PathVariable Long studentModalityId,
            @Valid @RequestBody ReasonRequest body
    ) {
        String reason = body.getReason();
        return ResponseEntity.ok(cancellationService.rejectModalityCancellationByDirector(studentModalityId, reason, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/cancellation/approve")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_CANCELLATION + "')")
    public ResponseEntity<OperationResultResponse> approveCancellation(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(cancellationService.approveCancellation(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/cancellation/reject")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_REJECT_CANCELLATION + "')")
    public ResponseEntity<CancellationRejectedResponse> rejectCancellation(@PathVariable Long studentModalityId, @Valid @RequestBody ReasonRequest body
    ) {
        return ResponseEntity.ok(cancellationService.rejectCancellation(studentModalityId, body.getReason(), SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/cancellation-request")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_CANCELLATIONS + "')")
    public ResponseEntity<List<CancellationList>> getPendingCancellations() {

        List<CancellationList> cancellations =
                cancellationService.getPendingCancellations(SecurityUtils.getCurrentUser());

        return ResponseEntity.ok(cancellations);
    }

    @GetMapping("/cancellation/document/{studentModalityId}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_CANCELLATIONS + "')")
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
    @PreAuthorize("hasAuthority('" + Permissions.PERM_ASSIGN_PROJECT_DIRECTOR + "')")
    public ResponseEntity<DirectorAssignmentResponse> assignProjectDirector(@PathVariable Long studentModalityId, @PathVariable Long directorId) {
        return ResponseEntity.ok(cancellationService.assignProjectDirector(studentModalityId, directorId, SecurityUtils.getCurrentUser()));
    }


    @PutMapping("/{studentModalityId}/change-director")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_ASSIGN_PROJECT_DIRECTOR + "')")
    public ResponseEntity<DirectorChangeResponse> changeProjectDirector(@PathVariable Long studentModalityId, @RequestBody @Valid ChangeDirectorDTO request) {
        return ResponseEntity.ok(cancellationService.changeProjectDirector(studentModalityId, request.getNewDirectorId(), request.getReason(), SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/propose-defense-director")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_PROPOSE_DEFENSE + "')")
    public ResponseEntity<DefenseScheduleResponse> proposeDefenseByDirector(@PathVariable Long studentModalityId, @Valid @RequestBody ScheduleDefenseRequest request) {
        return ResponseEntity.ok(defenseWorkflowService.scheduleDefense(studentModalityId, request, SecurityUtils.getCurrentUser()));
    }


    @PostMapping("/{studentModalityId}/examiners/assign")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_SCHEDULE_DEFENSE + "')")
    public ResponseEntity<ExaminerAssignmentResponse> assignExaminers(@PathVariable Long studentModalityId, @Valid @RequestBody ScheduleDefenseRequest request) {
        return ResponseEntity.ok(defenseWorkflowService.assignExaminers(studentModalityId, request, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/final-evaluation/register")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_EVALUATE_DEFENSE + "')")
    public ResponseEntity<Object> registerFinalDefenseEvaluation(
            @PathVariable Long studentModalityId,
            @Valid @RequestBody ExaminerEvaluationDTO evaluationDTO) {
        return ResponseEntity.ok(defenseEvaluationService.registerFinalDefenseEvaluation(studentModalityId, evaluationDTO, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/project-directors")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_PROJECT_DIRECTOR + "')")
    public ResponseEntity<List<ProjectDirectorResponse>> getProjectDirectors() {
        return ResponseEntity.ok(modalityCatalogService.getProjectDirectors(SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/program-heads")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_PROGRAM_HEAD + "')")
    public ResponseEntity<List<ProjectDirectorResponse>> getProgramHeads() {
        return ResponseEntity.ok(modalityCatalogService.getProgramHeads(SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/committee")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_COMMITTEE + "')")
    public ResponseEntity<List<ProjectDirectorResponse>> getProgramCurriculumCommittee(
            @RequestParam(required = false) Long academicProgramId,
            @RequestParam(required = false) Long facultyId
    ) {
        return ResponseEntity.ok(modalityCatalogService.getProgramCurriculumCommittee(academicProgramId, facultyId, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/examiners")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_COMMITTEE + "')")
    public ResponseEntity<List<ProjectDirectorResponse>> getExaminers(
            @RequestParam(required = false) Long academicProgramId,
            @RequestParam(required = false) Long facultyId
    ) {
        return ResponseEntity.ok(modalityCatalogService.getExaminers(academicProgramId, facultyId, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/examiners/for-committee")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_EXAMINER + "')")
    public ResponseEntity<List<ProjectDirectorResponse>> getExaminersForCommittee() {
        return ResponseEntity.ok(modalityCatalogService.getExaminersForCommittee(SecurityUtils.getCurrentUser()));
    }


    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_FINAL_DEFENSE_RESULT + "')")
    @GetMapping("/final-evaluation/{studentModalityId}/result")
    public ResponseEntity<FinalDefenseResponse> getFinalDefenseResult(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseEvaluationService.getFinalDefenseResult(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/final-evaluation/my-result")
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "')")
    public ResponseEntity<Object> getMyFinalDefenseResult() {
        return ResponseEntity.ok(defenseEvaluationService.getMyFinalDefenseResult(SecurityUtils.getCurrentUser()));
    }


    @PostMapping("/{studentModalityId}/documents/{documentId}/resubmit-correction")
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "')")
    public ResponseEntity<ResubmitDocumentResponse> resubmitCorrectedDocument(
            @PathVariable Long studentModalityId,
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(modalityDocumentService.resubmitCorrectedDocument(studentModalityId, documentId, file, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/documents/{documentId}/approve-correction")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_REVIEW_DOCUMENTS + "')")
    public ResponseEntity<ApproveCorrectedDocumentResponse> approveCorrectedDocument(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentWorkflowService.approveCorrectedDocument(documentId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/documents/{documentId}/reject-correction-final")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_REVIEW_DOCUMENTS + "')")
    public ResponseEntity<RejectCorrectedDocumentFinalResponse> rejectCorrectedDocumentFinal(
            @PathVariable Long documentId,
            @Valid @RequestBody ReasonRequest request) {
        String reason = request.getReason();
        return ResponseEntity.ok(documentWorkflowService.rejectCorrectedDocumentFinal(documentId, reason, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/{studentModalityId}/correction-deadline-status")
    @PreAuthorize("hasAnyRole('" + Roles.ROLE_STUDENT + "','" + Roles.ROLE_PROGRAM_HEAD + "','" + Roles.ROLE_PROGRAM_CURRICULUM_COMMITTEE + "','" + Roles.ROLE_PROJECT_DIRECTOR + "','" + Roles.ROLE_EXAMINER + "')")
    public ResponseEntity<CorrectionDeadlineStatusResponse> getCorrectionDeadlineStatus(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(modalityDocumentService.getCorrectionDeadlineStatus(studentModalityId, SecurityUtils.getCurrentUser()));
    }


    @PostMapping("/{studentModalityId}/close-by-committee")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_CANCELLATION + "') or hasAuthority('" + Permissions.PERM_REVIEW_DOCUMENT_COMMITTEE + "')")
    public ResponseEntity<CloseModalityResponse> closeModalityByCommittee(
            @PathVariable Long studentModalityId,
            @Valid @RequestBody ReasonRequest request
    ) {
        String reason = request.getReason();
        return ResponseEntity.ok(documentWorkflowService.closeModalityByCommittee(studentModalityId, reason, SecurityUtils.getCurrentUser()));
    }


    @PostMapping("/{studentModalityId}/approve-final-by-committee")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_MODALITY_BY_COMMITTEE + "')")
    public ResponseEntity<ApproveFinalModalityResponse> approveFinalModalityByCommittee(@PathVariable Long studentModalityId, @RequestBody(required = false) Map<String, String> request) {
        String observations = request != null ? request.get("observations") : null;
        return ResponseEntity.ok(documentWorkflowService.approveFinalModalityByCommittee(studentModalityId, observations, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/reject-final-by-committee")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_REJECT_MODALITY_BY_COMMITTEE + "')")
    public ResponseEntity<RejectFinalModalityResponse> rejectFinalModalityByCommittee(@PathVariable Long studentModalityId, @Valid @RequestBody ReasonRequest request) {
        String reason = request.getReason();
        return ResponseEntity.ok(documentWorkflowService.rejectFinalModalityByCommittee(studentModalityId, reason, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/defense-evaluation/reset")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_MODALITY_BY_COMMITTEE + "')")
    public ResponseEntity<OperationResultResponse> resetDefenseEvaluation(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseEvaluationService.resetDefenseEvaluation(studentModalityId, SecurityUtils.getCurrentUser()));
    }


    @Operation(summary = "Crear seminario", description = "Crea un nuevo seminario en un programa académico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Seminario creado exitosamente"),
            @ApiResponse(responseCode = "409", description = "Ya existe un seminario con ese nombre en este programa académico"),
            @ApiResponse(responseCode = "403", description = "Permiso denegado: necesita PERM_CREATE_SEMINAR")
    })
    @PostMapping("/seminar/create")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_SEMINAR + "')")
    public ResponseEntity<CreateSeminarResponse> createSeminar(@Valid @RequestBody SeminarDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seminarModalityService.createSeminar(request, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/seminar/{seminarId}/detail")
    @PreAuthorize("hasRole('" + Roles.ROLE_PROGRAM_HEAD + "')")
    public ResponseEntity<SeminarDetailResponse> getSeminarDetail(@PathVariable Long seminarId) {
        return ResponseEntity.ok(seminarModalityService.getSeminarDetailForProgramHead(seminarId, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/seminar/available")
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "')")
    public ResponseEntity<SeminarResponse> listActiveSeminarsWithSeats() {
        return ResponseEntity.ok(seminarModalityService.listActiveSeminarsWithSeats(SecurityUtils.getCurrentUser()));
    }


    @PostMapping("/seminar/{seminarId}/enroll")
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "')")
    public ResponseEntity<EnrollSeminarResponse> enrollInSeminar(@PathVariable Long seminarId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seminarModalityService.enrollInSeminar(seminarId, SecurityUtils.getCurrentUser()));
    }


    @GetMapping("/seminars")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_SEMINAR + "')")
    public ResponseEntity<SeminarListResponse> listSeminars(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(seminarModalityService.listSeminarsForProgramHead(status, active, SecurityUtils.getCurrentUser()));
    }


    @PostMapping("/seminar/{seminarId}/start")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_SEMINAR + "')")
    public ResponseEntity<StartSeminarResponse> startSeminar(@PathVariable Long seminarId) {
        return ResponseEntity.ok(seminarModalityService.startSeminar(seminarId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/seminar/{seminarId}/cancel")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_SEMINAR + "')")
    public ResponseEntity<CancelSeminarResponse> cancelSeminar(@PathVariable Long seminarId, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(seminarModalityService.cancelSeminar(seminarId, reason, SecurityUtils.getCurrentUser()));
    }

    @PutMapping("/seminar/{seminarId}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_SEMINAR + "')")
    public ResponseEntity<UpdateSeminarResponse> updateSeminar(@PathVariable Long seminarId, @Valid @RequestBody SeminarDTO request) {
        return ResponseEntity.ok(seminarModalityService.updateSeminar(seminarId, request, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/seminar/{seminarId}/close-registrations")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_SEMINAR + "')")
    public ResponseEntity<CloseRegistrationsResponse> closeRegistrations(@PathVariable Long seminarId) {
        return ResponseEntity.ok(seminarModalityService.closeRegistrations(seminarId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/seminar/{seminarId}/complete")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_CREATE_SEMINAR + "')")
    public ResponseEntity<CompleteSeminarResponse> completeSeminar(@PathVariable Long seminarId) {
        return ResponseEntity.ok(seminarModalityService.completeSeminar(seminarId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/ready-for-defense")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_PROPOSE_DEFENSE + "')")
    public ResponseEntity<DefenseWorkflowResponse> modalityReadyForDefenseByDirector(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseWorkflowService.modalityReadyForDefenseByDirector(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/program-head/approve-final-and-notify-examiners")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_MODALITY + "')")
    public ResponseEntity<DefenseWorkflowResponse> programHeadApprovesAndNotifiesExaminers(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseWorkflowService.programHeadApprovesAndNotifiesExaminers(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @PostMapping("/{studentModalityId}/final-review-completed")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_MODALITY_BY_EXAMINER + "')")
    public ResponseEntity<DefenseWorkflowResponse> examinerFinalReviewCompleted(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseWorkflowService.examinerFinalReviewCompleted(studentModalityId, SecurityUtils.getCurrentUser()));
    }


    @GetMapping("/{studentModalityId}/examiner-evaluation")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_EXAMINER_MODALITIES + "')")
    public ResponseEntity<Object> getFinalDefenseEvaluationForExaminer(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseEvaluationService.getFinalDefenseEvaluationForExaminer(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    /**
     * Endpoint para que el jurado autenticado obtenga su calendario de próximas sustentaciones.
     * Solo incluye modalidades en estado DEFENSE_SCHEDULED, ordenadas por fecha de defensa ascendente.
     */
    @GetMapping("/examiner/defense-calendar")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_EXAMINER_MODALITIES + "')")
    public ResponseEntity<List<ModalityListDTO>> getExaminerDefenseCalendar() {
        return ResponseEntity.ok(defenseWorkflowService.getExaminerDefenseCalendar(SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/examiner-type/{studentModalityId}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_EXAMINER_MODALITIES + "')")
    public ResponseEntity<ExaminerTypeResponse> getExaminerTypeForModality(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseWorkflowService.getExaminerTypeForModality(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/examiner-evaluation/{studentModalityId}")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_EXAMINER_MODALITIES + "')")
    public ResponseEntity<Object> getExaminerEvaluationForModality(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(defenseEvaluationService.getExaminerEvaluationForModality(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    /**
     * El jurado autenticado obtiene su veredicto sobre documentos MANDATORY de propuesta.
     * Devuelve la decisión individual del jurado, notas y evaluación de propuesta (si aplica).
     * Ruta: GET /modalities/documents/{studentDocumentId}/examiner-proposal-evaluation
     */
    @GetMapping("/documents/{studentDocumentId}/examiner-proposal-evaluation")
    @PreAuthorize("hasRole('" + Roles.ROLE_EXAMINER + "')")

    public ResponseEntity<Object> getMyProposalEvaluation(@PathVariable Long studentDocumentId) {
        return ResponseEntity.ok(documentEditRequestService.getMyProposalEvaluation(studentDocumentId, SecurityUtils.getCurrentUser()));
    }

    @GetMapping("/documents/{studentDocumentId}/examiner-final-evaluation")
    @PreAuthorize("hasRole('" + Roles.ROLE_EXAMINER + "')")

    public ResponseEntity<Object> getMyFinalDocumentEvaluation(@PathVariable Long studentDocumentId) {
        return ResponseEntity.ok(documentEditRequestService.getMyFinalDocumentEvaluation(studentDocumentId, SecurityUtils.getCurrentUser()));
    }

    // =========================================================================
    // SOLICITUD DE EDICIÓN DE PROPUESTA APROBADA
    // =========================================================================

    /**
     * El estudiante solicita editar un documento MANDATORY que ya fue aprobado por los jurados.
     * Body: { "reason": "motivo justificado de la solicitud (mínimo 20 caracteres)" }
     */
    @PostMapping("/documents/{studentDocumentId}/request-edit")
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "')")
    public ResponseEntity<EditRequestCreatedResponse> requestDocumentEdit(
            @PathVariable Long studentDocumentId,
            @Valid @RequestBody com.SIGMA.USCO.documents.dto.DocumentEditRequestDTO request) {
        return ResponseEntity.ok(documentEditRequestService.requestDocumentEdit(studentDocumentId, request, SecurityUtils.getCurrentUser()));
    }

    /**
     * Un jurado vota sobre una solicitud de edición de documento.
     * Sigue la lógica de consenso: ambos jurados primarios deben votar;
     * si hay desacuerdo, el jurado de desempate decide.
     * Body: { "approved": true|false, "resolutionNotes": "..." }
     */
    @PostMapping("/document-edit-requests/{editRequestId}/resolve")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_REVIEW_DOCUMENTS + "')")
    public ResponseEntity<EditRequestResolutionResponse> resolveDocumentEditRequest(
            @PathVariable Long editRequestId,
            @Valid @RequestBody com.SIGMA.USCO.documents.dto.DocumentEditResolutionDTO request) {
        return ResponseEntity.ok(documentEditRequestService.resolveDocumentEditRequest(editRequestId, request, SecurityUtils.getCurrentUser()));
    }

    /**
     * El jurado autenticado obtiene las solicitudes de edición pendientes de una modalidad.
     * Los jurados primarios ven las PENDING; el jurado de desempate ve las TIEBREAKER_REQUIRED.
     */
    @GetMapping("/{studentModalityId}/document-edit-requests/pending")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_EXAMINER_MODALITIES + "')")
    public ResponseEntity<PendingEditRequestsResponse> getPendingEditRequestsForExaminer(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentEditRequestService.getPendingEditRequestsForExaminer(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    /**
     * El jurado autenticado obtiene TODAS las solicitudes de edición de documentos
     * de una modalidad (todos los estados: pendiente, desempate, aprobado, rechazado).
     * Incluye información completa: documento, solicitante, votos de cada jurado,
     * si el jurado autenticado ya votó, si puede votar y el resultado final.
     */
    @GetMapping("/{studentModalityId}/document-edit-requests/all")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_EXAMINER_MODALITIES + "')")
    public ResponseEntity<ExaminerEditRequestsResponse> getAllEditRequestsForExaminer(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentEditRequestService.getAllEditRequestsForExaminer(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    // =========================================================================
    // ENDPOINTS GET PARA EL ESTUDIANTE – SOLICITUDES DE EDICIÓN
    // =========================================================================

    /**
     * El estudiante autenticado obtiene TODAS sus solicitudes de edición de documentos
     * en todas sus modalidades, con el estado de votación de cada una.
     */
    @GetMapping("/my-document-edit-requests")
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "')")
    public ResponseEntity<MyEditRequestsResponse> getMyDocumentEditRequests() {
        return ResponseEntity.ok(documentEditRequestService.getMyDocumentEditRequests(SecurityUtils.getCurrentUser()));
    }

    /**
     * El estudiante autenticado obtiene todas las solicitudes de edición asociadas
     * a una modalidad específica (por studentModalityId).
     */
    @GetMapping("/{studentModalityId}/my-document-edit-requests")
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "')")
    public ResponseEntity<ModalityEditRequestsResponse> getMyDocumentEditRequestsByModality(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentEditRequestService.getMyDocumentEditRequestsByModality(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    /**
     * El estudiante autenticado obtiene el detalle de una solicitud de edición específica.
     */
    @GetMapping("/document-edit-requests/{editRequestId}")
    @PreAuthorize("hasRole('" + Roles.ROLE_STUDENT + "')")
    public ResponseEntity<EditRequestDetailResponse> getDocumentEditRequestDetail(@PathVariable Long editRequestId) {
        return ResponseEntity.ok(documentEditRequestService.getDocumentEditRequestDetail(editRequestId, SecurityUtils.getCurrentUser()));
    }

    /**
     * Obtiene la lista de jurados (examinadores) asociados a una modalidad específica.
     * Retorna información detallada de cada jurado: ID, nombre, email, tipo (primario 1, primario 2, desempate)
     * y fecha de asignación.
     * Ruta: GET /modalities/{studentModalityId}/examiners
     */
    @GetMapping("/{studentModalityId}/examiners")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_VIEW_EXAMINER_MODALITIES + "')")
    public ResponseEntity<ExaminerListResponse> getExaminersForModality(@PathVariable Long studentModalityId) {
        return ResponseEntity.ok(documentEditRequestService.getExaminersForModality(studentModalityId, SecurityUtils.getCurrentUser()));
    }

    /**
     * Retorna la lista completa de todos los estudiantes que pertenecen al
     * programa académico del comité autenticado, con filtro opcional por nombre.
     * <p>
     * GET /modalities/committee/program-students?studentName=raul
     */
    @GetMapping("/committee/program-students")
    @PreAuthorize("hasAuthority('" + Permissions.PERM_STUDENT_LIST + "')")
    public ResponseEntity<ProgramStudentsResponse> getProgramStudentsForCommittee(@RequestParam(required = false) String studentName) {
        return ResponseEntity.ok(modalityListingService.getProgramStudentsForCommittee(studentName, SecurityUtils.getCurrentUser()));
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
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_MODALITY + "')")
    public ResponseEntity<PendingDistinctionProposalsResponse> getPendingDistinctionProposals() {
        return ResponseEntity.ok(defenseEvaluationService.getPendingDistinctionProposals(SecurityUtils.getCurrentUser()));
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
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_MODALITY + "')")
    public ResponseEntity<AcceptDistinctionResponse> acceptDistinctionProposal(
            @PathVariable Long studentModalityId,
            @RequestBody(required = false) DistinctionDecisionRequest body) {
        String notes = body != null ? body.notes() : null;
        return ResponseEntity.ok(defenseEvaluationService.acceptDistinctionProposal(studentModalityId, notes, SecurityUtils.getCurrentUser()));
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
    @PreAuthorize("hasAuthority('" + Permissions.PERM_APPROVE_MODALITY + "')")
    public ResponseEntity<RejectDistinctionResponse> rejectDistinctionProposal(
            @PathVariable Long studentModalityId,
            @Valid @RequestBody ReasonRequest body) {
        String reason = body.getReason();
        return ResponseEntity.ok(defenseEvaluationService.rejectDistinctionProposal(studentModalityId, reason, SecurityUtils.getCurrentUser()));
    }

}
