# Inventario de contratos de API (FASE 2 — CERRADO 8/2026)

Documento de cierre de la FASE 2 del `PLAN_MAESTRO_REFACTORIZACION.md`.
Objetivo cumplido: **0 endpoints `Map<String,Object>` sin tipar** (salvo 3 diccionarios legítimos documentados + 1 excepción deliberada) y **0 `ResponseEntity<?>`** (salvo la excepción Auth).

## Regla de oro

Cada DTO de respuesta produjo **exactamente las mismas claves JSON que el `Map` que reemplaza**
(nombres de campo del record == claves del Map, mismos tipos). El frontend NO se rompió.
El JSON de éxito es el **DTO crudo** (no se envuelve en `ApiResponse`).

## Estado final

| Grep | Resultado |
|---|---|
| `ResponseEntity<Map<String, Object>>` en controllers | **3** — todos legítimos documentados con `@Schema`: `reviewSecondaryDocumentExaminer` (ModalityController:264, merge de consenso de forma variable), `healthCheck` y `getAvailableReportsCatalog` (GlobalModalityReportController:338,:353, catálogo/health dinámicos) |
| `ResponseEntity<?>` | **2** — `AuthController.register`/`login` + `AuthService` (excepción deliberada: el frontend lee `errorMessage.includes("correo")`; NO tocar) |
| `Map<String, Object>` en services | **0 públicos** (quedan solo privados — exentos — y payloads de eventos `ModalityEvent`) |
| `Map<String, Object>` anidados en DTOs | Legítimos: `evaluationCriteria` (DefenseEvaluationService), filas de `pendingDistinctionProposals`, `traceability`/merge en `reviewFinalDocumentByExaminer` |

## Uniones `ResponseEntity<Object>` documentadas (7 endpoints)

El service devuelve `Object` (union de 2-4 DTOs según rama). El JSON no cambió; NO se resolvieron (YAGNI):

- `registerFinalDefenseEvaluation` → PrimaryEvaluationPendingResponse | ConsensusEvaluationResponse | TiebreakerRequiredResponse | TiebreakerEvaluationResponse
- `getFinalDefenseEvaluationForExaminer` / `getExaminerEvaluationForModality` → ExaminerEvaluationNotFoundResponse | ExaminerFinalEvaluationResponse / ExaminerEvaluationRecordedResponse
- `getMyFinalDefenseResult` (rama `{hasResult,message}` sin DTO)
- `reviewStudentDocumentByExaminer` (DocumentWorkflowService, early-returns de consenso)
- `getMyProposalEvaluation` / `getMyFinalDocumentEvaluation` (rama sin veredicto → ExaminerEvaluationNotFoundResponse)

## DTOs creados por módulo

### `Modalities/dto/response/` (la mayoría; ver tablas por service abajo)

Pre-creados por tandas DTO: `FinalDefenseResponse`, `ExaminerDocumentReviewResponse`, `DocumentEditRequestResponseDTO`,
`CancellationRequestResponse`, `CancellationRejectedByDirectorResponse`, `CancellationRejectedResponse`,
`DirectorAssignmentResponse` (+`DirectorInfo`), `DirectorChangeResponse`, `DefenseScheduleResponse`,
`PendingDefenseProposalsResponse`, `DefenseProposalApprovalResponse`, `DefenseRescheduleResponse`,
`ExaminerAssignmentResponse`, `DefenseWorkflowResponse`, `ExaminerTypeResponse`, `StartGroupModalityResponse`,
`InviteStudentResponse`, `AcceptInvitationResponse`, `ProgramStudentsResponse` (+`StudentSummary`).

Creados por los agentes de servicios: `ExaminerEvaluationNotFoundResponse`, `EditRequestCreatedResponse`,
`EditRequestResolutionResponse` (+`EditVoteSummary`), `ExaminerEditRequestsResponse` (+`ExaminerContext`, `ModalityContext`,
`EditRequestSummary`, `EditRequestListItem`, `EditRequestVoteInfo`, `EditRequestMyVote`), `PendingEditRequestsResponse`,
`MyEditRequestsResponse`, `ModalityEditRequestsResponse`, `EditRequestDetailResponse`, `ExaminerListResponse` (+`ExaminerInfo`),
`CreateSeminarResponse`, `SeminarResponse`, `EnrollSeminarResponse`, `SeminarDetailResponse`, `SeminarListResponse`,
`StartSeminarResponse`, `CancelSeminarResponse`, `UpdateSeminarResponse` (+`SeminarSummary`), `CloseRegistrationsResponse`,
`CompleteSeminarResponse`, `UploadDocumentResponse`, `ValidateAllDocumentsUploadedResponse`, `AvailableDocumentsResponse`
(+`AvailableDocumentDTO`, `DocumentStatistics`), `StudentDocumentResponse`, `RequiredDocumentsUploadedResponse`
(+`MissingDocumentInfo`), `DocumentsAcceptedForCommitteeResponse` (+`NotAcceptedDocumentInfo`), `ResubmitDocumentResponse`,
`CorrectionDeadlineStatusResponse`, `ConsensusEvaluationResponse` (**preserva typo `exito`** — contrato), `TiebreakerRequiredResponse`,
`TiebreakerEvaluationResponse`, `PrimaryEvaluationPendingResponse`, `ExaminerFinalEvaluationResponse`,
`ExaminerEvaluationRecordedResponse`, `PendingDistinctionProposalsResponse`, `AcceptDistinctionResponse`,
`RejectDistinctionResponse`, `ReviewStudentDocumentResponse`, `ApproveModalityResponse`, `CommitteeDocumentReviewResponse`,
`ApproveCorrectedDocumentResponse`, `RejectCorrectedDocumentFinalResponse`, `CloseModalityResponse`,
`ApproveFinalModalityResponse`, `RejectFinalModalityResponse`.

### `common/web/`
`OperationResultResponse(boolean success, String message, Long studentModalityId)` + ctor `(success, message)` —
shape compartido de las operaciones `{success, message[, studentModalityId]}`.

### `notifications/dto/`
`NotificationResponse` (id, type, subject, message, createdAt, read, studentModalityId, invitationId — lista y detalle mismo shape),
`UnreadCountResponse` (unreadCount). `GET /notifications` → `List<NotificationResponse>`.

### `report/dto/`
`ReportResponse<T>` (success, message, reportType, data, timestamp) + variantes: `modalities/types` sin reportType,
`modalities/filtered` con `filtersApplied`, `modality-traceability/by-student` con `studentId`. `jsonError` unificado.
Bug colateral: `Map.of("data", null)` → NPE corregido.

### `academic/dto/response/` (nuevo)
`FacultyResponse` (message, faculty), `FacultyListResponse` (faculties), `MessageResponse` (message) [Users/dto/response],
`ProgramResponse` (message, program), `ProgramDegreeModalityResponse` (success, message, data),
`ProgramDegreeModalityDataResponse` (success, data), `ProgramDegreeModalityListResponse` (success, data, count),
`SuccessMessageResponse` (success, message). No se fusionaron formas parecidas (la clave de dato difiere: faculty/program/data).

### `Users/dto/response/`
`MessageResponse` (message), `ProjectTitleResponse` [documents/dto/response] (proposal, title, status... — según el Map real).
`CancellationRequestResponse` (Users) **BORRADO** — duplicado de `Modalities/dto/response/CancellationRequestResponse`; el
único uso era el inline de `StudentController.requestCancellation`, que ahora devuelve directo el record del service.

## Tablas método → DTO → claves

### DefenseEvaluationService (6 públicos)
| Método | DTO | Claves |
|---|---|---|
| `registerFinalDefenseEvaluation` | union (ver arriba) | success,message,grade,approved · **exito**,consenso,estadoFinal,distincionAcademica,calificacionFinal,distincionPendienteRevision,mensaje · success,hasConsensus,requiresTiebreaker,status,message · success,isTiebreaker,finalStatus,academicDistinction,finalGrade,pendingDistinctionReview,message |
| `getFinalDefenseEvaluationForExaminer` | union | success,message · success,evaluationId,grade,approved,observations,evaluationDate,isFinalDecision,examinerType,evaluationCriteria(Map) |
| `getPendingDistinctionProposals` | PendingDistinctionProposalsResponse | success,totalPending,pendingDistinctionProposals(List<Map>) |
| `acceptDistinctionProposal` | AcceptDistinctionResponse | success,studentModalityId,newStatus,confirmedDistinction,message |
| `rejectDistinctionProposal` | RejectDistinctionResponse | success,studentModalityId,newStatus,finalDistinction,reason,message |
| `getExaminerEvaluationForModality` | union | success,message · success,evaluation |

### DocumentWorkflowService (13 públicos)
| Método | DTO | Claves |
|---|---|---|
| `startStudentModalityIndividual` | StartGroupModalityResponse (reuso) | eligible,studentModalityId,studentModalityName,modalityType,message |
| `reviewStudentDocument` | ReviewStudentDocumentResponse | message,documentId,newStatus |
| `approveModalityByProgramHead/ByCommittee/ByExaminers` | ApproveModalityResponse (compartido) | approved,newStatus,message |
| `reviewStudentDocumentByCommittee` | CommitteeDocumentReviewResponse | success,documentId,documentName,newStatus,newModalityStatus(@NON_NULL),message |
| `approveCorrectedDocument` | ApproveCorrectedDocumentResponse | success,message,documentId,newDocumentStatus,newModalityStatus |
| `rejectCorrectedDocumentFinal` | RejectCorrectedDocumentFinalResponse | success,message,documentId,finalStatus |
| `closeModalityByCommittee` | CloseModalityResponse | success,studentModalityId,previousStatus,newStatus,closedBy,reason,message |
| `approveFinalModalityByCommittee` | ApproveFinalModalityResponse | success,studentModalityId,previousStatus,newStatus,academicDistinction,finalGrade("N/A"),approvedBy,observations,message |
| `rejectFinalModalityByCommittee` | RejectFinalModalityResponse | success,studentModalityId,previousStatus,newStatus,rejectedBy,reason,message |
| `reviewStudentDocumentByExaminer` | union/Map (early-returns) | sin DTO — deferral |
| `reviewFinalDocumentByExaminer` | **Map legítimo** (merge de consenso) | claves del consenso + secondaryEvaluation,finalEvaluation,currentModalityStatus,traceability |

### DocumentEditRequestService (9 públicos)
| Método | DTO | Claves |
|---|---|---|
| `getMyProposalEvaluation`/`getMyFinalDocumentEvaluation` | union | success,message · (rama con veredicto) |
| `requestDocumentEdit` | EditRequestCreatedResponse | success,editRequestId,documentId,documentName,newDocumentStatus,newModalityStatus,message |
| `resolveDocumentEditRequest` | EditRequestResolutionResponse | 3 formas: success,editRequestId,message,votesReceived,votesRequired / +newStatus,votes / +documentId,documentName,finalStatus,newDocumentStatus,newModalityStatus,resolvedByTiebreaker,votes |
| `getAllEditRequestsForExaminer` | ExaminerEditRequestsResponse | success,examiner,modality,summary,editRequests |
| `getPendingEditRequestsForExaminer` | PendingEditRequestsResponse | success,studentModalityId,examinerType,isTiebreaker,pendingEditRequests |
| `getMyDocumentEditRequests` | MyEditRequestsResponse | success,totalRequests,editRequests |
| `getMyDocumentEditRequestsByModality` | ModalityEditRequestsResponse | success,studentModalityId,totalRequests,editRequests |
| `getDocumentEditRequestDetail` | EditRequestDetailResponse | success,editRequest |
| `getExaminersForModality` | ExaminerListResponse | 2 formas: success,studentModalityId,examiners,message / +modalityName,modalityStatus,examinersCount |

### SeminarModalityService (10 públicos)
createSeminar → CreateSeminarResponse (success,message,seminarId,programName,seminarName) · listActiveSeminarsWithSeats → SeminarResponse (success,seminars) · enrollInSeminar → EnrollSeminarResponse (success,message,seminarName,enrollmentDate,currentParticipants,maxParticipants,availableSeats) · getSeminarDetailForProgramHead → SeminarDetailResponse (success,seminar) · listSeminarsForProgramHead → SeminarListResponse (success,seminars,total) · startSeminar → StartSeminarResponse (success,message,seminarId,seminarName,status,startDate,enrolledStudents,emailsSent) · cancelSeminar → CancelSeminarResponse (success,message,seminarId,seminarName,status,previouslyEnrolledStudents,emailsSent) · updateSeminar → UpdateSeminarResponse (success,message,seminar) · closeRegistrations → CloseRegistrationsResponse (success,message,seminarId,seminarName,status,currentParticipants,maxParticipants,updatedAt) · completeSeminar → CompleteSeminarResponse (success,message,seminarId,seminarName,status,startDate,endDate,totalParticipants)

### ModalityDocumentService (8 públicos)
uploadRequiredDocument → UploadDocumentResponse (message,path,documentStatus,modalityStatus — SIN success, como el Map) · validateAllDocumentsUploaded → ValidateAllDocumentsUploadedResponse (canContinue,missingDocuments) · getAvailableDocumentsForStudent → AvailableDocumentsResponse (success,studentModalityId,documents,statistics) · getStudentDocuments → List<StudentDocumentResponse> (studentDocumentId,documentName,documentType,status,notes,uploadedAt,**filePath**) · validateAllRequiredDocumentsUploaded → RequiredDocumentsUploadedResponse (allDocumentsUploaded,totalRequired,totalUploaded,missingDocuments,missingCount) · validateAllDocumentsAcceptedForCommittee → DocumentsAcceptedForCommitteeResponse (allAccepted,notAcceptedDocuments,notAcceptedCount,totalRequired) · resubmitCorrectedDocument → ResubmitDocumentResponse (success,message,documentId,newStatus) · getCorrectionDeadlineStatus → CorrectionDeadlineStatusResponse (2 formas: hasCorrectionRequest,currentStatus,message / +correctionRequestDate,correctionDeadline,daysRemaining,isExpired,reminderSent)

### CancellationService / DefenseWorkflowService / ModalityGroupService / StudentModalityListingService
Operaciones {success,message} → `OperationResultResponse` (common.web). Cancelaciones → CancellationRequestResponse /
CancellationRejectedByDirectorResponse / CancellationRejectedResponse. Director → DirectorAssignmentResponse /
DirectorChangeResponse(+DirectorInfo). Defensas → DefenseScheduleResponse / PendingDefenseProposalsResponse /
DefenseProposalApprovalResponse / DefenseRescheduleResponse / ExaminerAssignmentResponse / DefenseWorkflowResponse /
ExaminerTypeResponse. Grupos → StartGroupModalityResponse / InviteStudentResponse / AcceptInvitationResponse.
Listados → ProgramStudentsResponse(+StudentSummary).

### Notifications (NotificationController)
GET /notifications → List<NotificationResponse> · GET unread → UnreadCountResponse · POST read → OperationResultResponse.

### Report (GlobalModalityReportController)
15 endpoints → ReportResponse<T> (variantes arriba). PDFs sin cambio. health/available = Map legítimo con @Schema.

### Academic
Ver tabla de `academic/dto/response/` arriba. Los 4 endpoints que ya eran concretos no se tocaron.

## T2.12 — Constantes de permisos/roles (`common/security/`)

- `Permissions.java`: 56 constantes `PERM_*` (valor = autoridad exacta `"PERM_" + name`).
- `Roles.java`: 8 constantes (valor = nombre sembrado SIN prefijo `ROLE_`; `hasRole('STUDENT')` ≡ autoridad `ROLE_STUDENT`).
- 150 anotaciones `@PreAuthorize` sustituidas (byte-idénticas tras concatenación) + `DataInitializer` (53 permisos + 6 roles) + 3 strings en ProjectTitleService.
- Forma usada: `@PreAuthorize("hasAuthority('" + Permissions.PERM_X + "')")` — la forma sin comillas del plan produce SpEL inválido en runtime.
- Discrepancias DOCUMENTADAS (no corregidas, no cambian autorización):
  1. `ADMIN` y `JURY` en `@PreAuthorize` (ProjectTitleController) NO existen en el seeder dev (probablemente creados a mano en BD prod).
  2. Sembrados sin constante: `VIEW_REPORTS` (vs `VIEW_REPORT`), `VIEW_EXAMINERS` (vs `VIEW_EXAMINER`), `APPROVE_FINAL_MODALITY_BY_EXAMINER`, `VIEW_EXAMINER_EVALUATION` + 3 legacy en español (`VER_DOCUMENTOS_ESTUDIANTE`, `CREAR_USUARIO`, `EDITAR_USUARIO`).
  3. Verificados nunca sembrados en dev: `PERM_REVIEW_DOCUMENT_COMMITTEE`, `PERM_VIEW_PROGRAM_DEGREE_MODALITY`, `PERM_UPDATE_PROGRAM_DEGREE_MODALITY`.

## Pendientes (fuera de alcance FASE 2)

- `filePath` (ruta absoluta) expuesto en `UploadDocumentResponse.path` y `StudentDocumentResponse.filePath` — decisión de negocio (reemplazar por id opaco) anotada en los javadocs de ambos records.
- Uniones `ResponseEntity<Object>` (7) y merge de consenso (`reviewFinalDocumentByExaminer`) — migrables si el frontend pide esquemas por rama.
- Discrepancias de permisos de T2.12 (tabla arriba) — auditarlas contra la BD prod.
- Renombrar DTOs `*DTO` mixtos → `*Response` (Fase 10 del plan maestro).

## Verificación (8/2026)

- `.\mvnw.cmd -q clean compile` → EXIT=0.
- `.\mvnw.cmd test` → 36 tests, 0 fallos, 2 skipped (ArchUnit @ArchIgnore).
- Greps de cierre: `ResponseEntity<Map` = 3 (los documentados arriba); `ResponseEntity<?>` = 2 (Auth, excepción); `Map<String, Object>` en firmas públicas de services = 0.
- Smoke runtime con frontend: PENDIENTE (requiere entorno vivo).
