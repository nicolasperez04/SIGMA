# SIGMA Backend - Agent Guidance

## Project Overview

Spring Boot 3.5.8 application managing university degree modalities (Proyecto de Grado, Práctica Profesional, etc.). Java 21, Maven, MySQL, JWT auth.

## Build & Run

```bash
./mvnw spring-boot:run
```

To skip tests during build:
```bash
./mvnw clean package -DskipTests
```

Docker:
```bash
docker build -t sigma-backend .
docker run -p 8080:8080 sigma-backend
```

## Configuration

**Environment variables** (`.env` file):
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - MySQL connection
- `JWT_SECRET` - JWT signing key
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` - SMTP config
- `UPLOAD_DIR` - File upload directory (default: `./SIGMA-uploads/SIGMA-files`)
- `FRONTEND_URL` - Frontend URL for CORS

The `.env` file is loaded **before** Spring's config via custom `EnvLoader` (see `SigmaApplication.java:14`). Environment variables from `.env` take precedence.

**Active profile**: `prod` (configured in `application.properties`). `dev` requiere `SPRING_PROFILES_ACTIVE=dev`; `DataInitializer` (seed de roles/permisos) solo corre bajo `dev`.

## API Documentation

Swagger UI available at: `http://localhost:8080/swagger-ui.html`
OpenAPI docs at: `http://localhost:8080/v3/api-docs`

## Key Packages

| Package | Purpose |
|---------|---------|
| `com.SIGMA.USCO.Users` | Authentication, user/role management |
| `com.SIGMA.USCO.Modalities` | Degree modality workflows |
| `com.SIGMA.USCO.Academic` | Faculty, program, degree programs |
| `com.SIGMA.USCO.documents` | Document handling, PDF generation |
| `com.SIGMA.USCO.notifications` | Email notifications |
| `com.SIGMA.USCO.report` | Reporting endpoints |

## Important Notes

- **Java version**: 21 with preview features enabled (`--enable-preview` compiler arg in pom.xml)
- **Upload files**: Stored in `./SIGMA-uploads/` (ensure directory exists before running)
- **Database**: Auto-updates schema (`hibernate.ddl-auto=update` in dev profile)
- **CORS**: Configured to allow frontend at `${FRONTEND_URL}` (default: `http://localhost:5173`)

## Refactoring: Notification Listeners (Ponytail Phase 0-1)

### Objective
Eliminate duplicated save+dispatch patterns across 5 notification listeners by:
1. Moving shared helper methods to `TranslationUtils.java`
2. Centralizing build+save+dispatch in `NotificationFactory.java`

### Completed
- **TranslationUtils.java**: `translateExaminerType()`, `getStudentList()`, `localizeObservations()` extracted from listeners
- **NotificationFactory.java**: Created with 4 methods — `buildAndDispatch` (2 overloads), `buildAndSave`, `saveAndDispatch`. (Los 2 overloads de attachment se eliminaron en Phase 2.4, Bloque 1.)
- **CommitteeNotificationListener.java**: Fully converted, fields replaced with `NotificationFactory`
- **ProgramHeadNotificationListener.java**: Fully converted, fields replaced with `NotificationFactory`
- **DirectorNotificationListener.java**: Fully converted, fields replaced with `NotificationFactory`
- **ExaminerNotificationListener.java**: Fully converted, `NotificationRepository` field removed
- **StudentNotificationListener.java**: Fully converted (870 lines, 26+ handlers). All `NotificationRepository.save()` + `dispatcher.dispatch()` pairs replaced with factory calls. 3 hand-rolled `Notification.builder()` cases remain (1 with `invitationId`, 2 with attachment) — these use `buildAndSave()`/`saveAndDispatch()` + inline `dispatchWithAttachment()`

### Hand-rolled builders still in StudentNotificationListener
- `handleModalityInvitationSent` (line 616): uses `.invitationId()` — kept hand-rolled + `saveAndDispatch()`
- `handleDefenseResult` leader path (line 312): uses `buildAndSave()` + inline `dispatchWithAttachment()`
- `handleModalityFinalApprovedByCommittee` (line 744): uses `buildAndSave()` + inline `dispatchWithAttachment()`

## Exception Mapping Audit (Phases 1.1–1.4)

### Status
- **1.1 (common/error layer)**: DONE. `common/` has 7 exception classes + `GlobalExceptionHandler` (94 lines), `ApiResponse` record, `PaginatedResponse`, `TranslationUtils`. `ApiException` NOT created (YAGNI — `BusinessException` is the root).
- **1.2 (type-erasing catches)**: FIXED. All findings corrected:
  - `SeminarModalityService`: added `catch (BusinessException e) { throw e; }` before 10 generic `catch (Exception e)` blocks (previously re-wrapped `ConflictException`/`ValidationException` into 500).
  - `StudentModalityListingService:1278` & `DocumentEditRequestService:1121`: added `catch (BusinessException e) { throw e; }` before `catch (RuntimeException)` (previously erased 404/409 into 400).
  - `AdminService:840`: `RuntimeException("Rol de programa no válido")` → `ValidationException`.
  - `DocumentModalityService:1194`: `IllegalArgumentException("Estado inválido")` → `ValidationException`.
  - `ModalityController:393`: `RuntimeException("No se pudo leer el archivo")` → `NotFoundException`.
  - `ModalityController:512` `resubmitCorrectedDocument`: removed `try/catch(IOException)` that leaked `e.getMessage()` as 500; method now declares `throws IOException`.
- **1.3 (controllers return ApiResponse, not raw types)**: FASE 2 SUPERSEDED — ver sección "## Fase 2 — Contratos de API" (shape de éxito = DTOs crudos; 0 endpoints Map salvo diccionarios legítimos documentados; excepción única AuthService register/login — frontend lee `errorMessage.includes("correo")`, NO tocar).
- **1.4 (@Valid coverage)**: DONE. 63/63 `@RequestBody` counted; 21 without `@Valid` all intentional (AuthController, ScheduleDefense, report filters, raw Map strings).

### Exception hierarchy
`BusinessException` (root, dominio → 400) → `ValidationException`, `ConflictException`, `NotFoundException`, `ForbiddenException`, `UnauthorizedException`.
`InternalException` (infraestructura → 500 explícito, mensaje fijo + causa; independiente de BusinessException).
`GlobalExceptionHandler` (Fase 3, 19 handlers): NotFound→404, Conflict→409, Forbidden→403, Unauthorized→401, AccessDenied→403, IllegalArgumentException→400 FIJO ("Solicitud inválida", sin leak), BusinessException→400, MethodArgumentNotValid→400 (data={campo:msg}), ConstraintViolationException→400 (data={campo:msg}), MethodArgumentTypeMismatch→400 fijo, DateTimeParse→400, DataIntegrityViolation→409, MaxUploadSizeExceeded→413 fijo, HttpMessageNotReadable→400 fijo, MissingServletRequestParameter→400 fijo, MissingServletRequestPart→400 fijo, NoResourceFound→404 fijo, InternalException→500 (mensaje fijo del thrower), generic Exception→500 (no message leak).

### Rule of thumb
Services must throw typed exceptions from `com.SIGMA.USCO.common.exception` so the handler maps them. Never re-wrap a caught domain exception into a generic `RuntimeException`/`ValidationException` — either rethrow as-is (`catch (BusinessException e) { throw e; }`) or let it propagate.

## Event Pipeline (Phase 1.6)

### Status: DONE
- **5 listeners** (`Student/ProgramHead/Committee/Director/ExaminerNotificationListener`): `handleEvent` now has `@Transactional(propagation = Propagation.REQUIRES_NEW)` + `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)`, wrapped in `try/catch` that logs `listener + event type + studentModalityId` then rethrows (context for manual alerting; multicaster error handler stays the single failure surface). REQUIRES_NEW is the only propagation Spring 6.2's `RestrictedTransactionalEventListenerFactory` allows; handlers **write** notifications via `NotificationFactory`, so readOnly would be wrong.
- **AsyncEventConfig**: `notificationTaskExecutor` now uses `ThreadPoolExecutor.CallerRunsPolicy` (backpressure instead of `RejectedExecutionException` on mass sends). Multicaster remains **synchronous** (intentional — AFTER_COMMIT runs on publisher thread); `dispatch()` keeps `@Async` (one hop total; removing it would put SMTP on the request thread).
- **ProjectTitleExtractionService**: `@EventListener` → `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)` (was running inside the publisher's transaction, before commit).
- **Dead try/catch**: the `dispatch(notification)` fallbacks around `dispatchWithAttachment` were already removed by the NotificationFactory refactor; Examiner's try/catch at `onFinalDefenseApproved` kept (also covers synchronous PDF generation).

### Phase 2.4 deferrals (resolved)
- `SeminarModalityService` swallow try/catch around `publishEvent` — REMOVED (8/2026); events now rely on the multicaster error handler.
- `DefenseModalityService` direct `examinerNotificationListener.notifyExaminersAssignment(...)` pre-commit call — REPLACED (8/2026) with `publishEvent(EXAMINER_ASSIGNED)`; `ExaminerNotificationListener.handleEvent` gained `case EXAMINER_ASSIGNED` and `notifyExaminersAssignment` lost `@Async` (runs in the REQUIRES_NEW handler, after commit).
- Typed event payloads — DECLINED (YAGNI): los eventos siguen con `Map<String,Object>`; los listeners usan claves `KEY_*`. El `studentModalityId = 0L` mágico se eliminó en el cierre de Fase 6 (SeminarModalityService SEMINAR_STARTED/SEMINAR_CANCELLED → `null`).

## Phase 2.4 — Notifications (DONE 8/2026)

Implementación de los 6 bloques del plan. Estado verificado contra el código.

### Pre-condiciones (estado verificado, sin tocar)
- **5 listeners con `@TransactionalEventListener`**: DONE. Los 5 (`Student/ProgramHead/Committee/Director/ExaminerNotificationListener`) tienen `@Transactional(REQUIRES_NEW)` + `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)` en `handleEvent` (Student:52-53, ProgramHead:40-41, Committee:45-46, Director:37-38, Examiner:51-52).
- **`@Async` en `dispatch`**: decisión mantener. `NotificationDispatcherService.dispatch`/`dispatchWithAttachment` siguen `@Async("notificationTaskExecutor")`; quitarlo pondría SMTP en el hilo del request.
- **try/catch muertos**: eliminados; único conservado = `ExaminerNotificationListener.onFinalDefenseApproved` (cubre la generación síncrona del PDF).
- **`DefenseModalityService` → `publishEvent`**: `DefenseModalityService:543` publica `EXAMINER_ASSIGNED`; `ExaminerNotificationListener.handleEvent` tiene `case EXAMINER_ASSIGNED`; `notifyExaminersAssignment` perdió `@Async` (su único caller es el handler).

### Bloque 1 — Código muerto
- `notifications/entity/NotificationTemplate.java` eliminado (0 usos).
- `NotificationRepository.findByRecipientIdOrderByCreatedAtDesc` eliminado (queda `findByRecipient_IdOrderByCreatedAtDesc`, usado por `NotificationService`).
- `NotificationMessageTemplates.closingDirector()` eliminado.
- 10 imports muertos eliminados en `StudentModalityListingService` y `ModalityCatalogService`.
- `NotificationFactory`: eliminados los 2 overloads de attachment (los sitios inline de `StudentNotificationListener` usan `dispatcher.dispatchWithAttachment` directo). La factory queda con 4 métodos: `buildAndDispatch` (x2), `buildAndSave`, `saveAndDispatch`.

### Bloque 2 — Texto duplicado → TranslationUtils
- `TranslationUtils.translateExaminerType()` reemplaza los switches inline "Jurado Principal"/"Jurado de Desempate" en `ExaminerNotificationListener` (105, 155, 616).
- `TranslationUtils.getStudentList(modality)` + overload `getStudentList(modality, withEmail)` (120-133) reemplazan los joins de estudiantes en Director (5 sitios), Examiner (4 + 1 con `withEmail=false`), ProgramHead (5).
- `TranslationUtils.localizeObservations()` (135), usado en `StudentNotificationListener:357`.

### Bloque 3 — StudentNotificationListener → NotificationMessageTemplates
- ~28 text-blocks movidos a métodos estáticos de `NotificationMessageTemplates` (mismo paquete `listeners`, sin import). Los `DateTimeFormatter`/cálculo de fechas quedan en el listener.
- Cada case del switch quedó en 1-3 líneas; `DOCUMENT_EDIT_APPROVED`/`DOCUMENT_EDIT_REJECTED` comparten handler (`handleDocumentEditResolved`).
- 3 builders hand-rolled conservados (ver sección "Hand-rolled builders"): `handleModalityInvitationSent` (:616 `.invitationId()`), `handleDefenseResult` líder (:312), `handleModalityFinalApprovedByCommittee` (:744).
- Criterio cumplido: mismo String exacto, mismo orden de args.

### Bloque 4 — AcademicCertificatePdfService
- `AcademicCertificateRepository` y `ExaminerCertificateRepository` ganaron `Optional<String> findTopByCertificateNumberStartingWithOrderByCertificateNumberDesc(String)` (zero-padding ⇒ max lexicográfico = max numérico).
- `CertificatePdfSupport.generateCertificateNumber(prefix, programId, Optional<String> currentMax)` (:251): parse +1 sobre el max actual. Eliminados los `findAll()`.
- Unificado: `AcademicCertificatePdfService.generate(sm, filePrefix, simplified)` privado (preámbulo común: borrar previo, número, dir, hash, save); los públicos `generateCertificate` (`ACTA_`, false) y `generateCertificateForCommitteeApproval` (`ACTA_COMITE_`, true) son wrappers. Callers intactos: Student:295/297/716, `AcademicCertificateTestController:72/74`.

### Bloque 5 — Bug correo de cancelación
`CommitteeNotificationListener.handleCancellationRequested` (71-90): el texto decía "revisada y aprobada por la Jefatura del Programa"; ahora dice "ha solicitado la cancelación de su modalidad de grado. En consecuencia, la solicitud será revisada y gestionada por el Comité de Currículo del Programa."

### Bloque 6 — Verificación final
- `./mvnw -q clean compile` → EXIT=0
- `./mvnw test` → 8/9 pass; el único error es `SigmaApplicationTests.contextLoads` por `MAIL_HOST` sin resolver en tests (env var solo en runtime vía `.env`; pre-existente, no relacionado con 2.4)
- Greps de cierre = 0: `NotificationTemplate`, `findByRecipientIdOrderByCreatedAtDesc`, `closingDirector`, imports de `USCO.notifications` en `StudentModalityListingService`/`ModalityCatalogService`
- Smoke test (GET /certificate/{id}, GET /notifications, un flujo que dispare correo de Student): NO corrido — requiere entorno vivo.

### Fuera de alcance (anotado para Fase 4)
`isCompleteModality` RESUELTO (ver cierre Fase 6: centralizado en `CertificatePdfSupport`). Pendientes: textos inline de los otros 4 listeners, `NotificationController`/`NotificationService` (Maps crudos), `ModalityServiceUtils.translateExaminerType` (duplicado de `TranslationUtils`).

## Phase 2.5 — Modalities: limpieza, transiciones centralizadas, dominio puro (DONE 8/2026)

### Bloque A — Código muerto eliminado
- `Entity/ExaminerEvaluation.java` + `Repository/ExaminerEvaluationRepository.java` (18 métodos, 0 consumidores) + campo inyectado muerto en `ModalityGroupService`. OJO: el **DTO** `ExaminerEvaluationDTO` está VIVO (`ModalityController:452` registerFinalDefenseEvaluation, `DefenseModalityService:557/1903`) — se conservó.
- `dto/ObservationRequest.java`, `dto/ValidationResultDTO.java` (0 usos).
- Proyecciones `Object[]` muertas eliminadas: `StudentModalityRepository.getModalityStatisticsByProgram`, `ModalityInvitationRepository.getInvitationStatistics` (0 callers — se borraron, no se proyectaron, YAGNI).
- `ModalityController`: 17 imports muertos + `@Slf4j` eliminados.

### Bloque B — Traducción de jurados unificada en `TranslationUtils.translateExaminerType`
- 3 sitios inline "Jurado Principal 1/2", "Jurado de Desempate" en `DefenseModalityService.assignExaminers` → `TranslationUtils` (salida byte-idéntica).
- Eliminados duplicados: `ModalityServiceUtils.translateExaminerType` (1 caller), `ExaminerType.toSpanish()` (2 callers). `DefenseCalendarReportService:705` (report module, duplicado privado) — **resuelto en Fase 2.6**.

### Bloque C — `ModalityStatusTransition` (nuevo, `Modalities/service`)
- API: `transition(modality, newStatus, responsible, observations)` (status + updatedAt + history + save, `now` único) y `recordHistory(modality, status, responsible, observations)` (solo historia).
- **62 sitios migrados** (DocumentModalityService 30+2, DefenseModalityService 12, CancellationService 8+1, DocumentEditRequestService 2+2, ModalityGroupService 4, CorrectionDeadlineSchedulerService 1). `responsible(null)` soportado (scheduler).
- Quedan 2 sitios `setStatus` SIN historia (intencional, sin historia original): `SeminarModalityService:639` (loop cancelación), `DefenseModalityService:678` (UNDER_EVALUATION_PRIMARY_EXAMINERS). El rechazo de edición (`DocumentEditRequestService:559`) YA usa `transition` (restaura estado previo desde historial).
- `historyRepository.save`/`ModalityProcessStatusHistory.builder` fuera del helper = 0.

### Bloque D — Dominio puro pragmático
- `ModalityController.getCancellationDocument` → lectura de archivo movida a `CancellationService.getCancellationDocumentResource(StudentDocument)` (precedente: `DocumentModalityService.viewStudentDocument` ya devolvía `Resource`). Controller limpiado.
- Imports muertos restantes eliminados (`ModalityCatalogService`: 11 de I/O; controller: Paths/Path/UrlResource/NotFoundException).
- Sin facades (decisión del usuario: alcance pragmático).

### Bloque E — Validación
- `ScheduleDefenseDTO`: `@Positive` en los 3 `*ExaminerId` + `@Valid` en los 3 endpoints (`proposeDefenseByDirector`, `rescheduleDefense`, `assignExaminers`). Constraints nullable-safe (los 3 flujos NO comparten requisitos; `assignExaminers` no exige fecha/lugar — la validación obligatoria sigue en los services).
- Nota: Phase 1.4 los había marcado "intencionales" — el plan 2.5 los incluyó explícitamente.

### Bloque F — `enrollStudent` → operación de entidad
- `SeminarRepository.enrollStudent` (INSERT nativo en `seminar_students`) eliminado; `SeminarModalityService:278` ahora usa `seminar.getEnrolledStudents().add(studentProfile)` (el `@ManyToMany` ya mapeaba la tabla; sync del persistence context).

### Verificación final
- `.\mvnw.cmd -q clean compile` → EXIT=0 (tras Fase 1, 2 y 3).
- `.\mvnw.cmd test` → 8/9 pass; único error = `SigmaApplicationTests.contextLoads` por `MAIL_HOST` (pre-existente).
- Greps de cierre = 0: `enrollStudent`, `getModalityStatisticsByProgram`, `getInvitationStatistics`, `toSpanish(`, `ModalityServiceUtils.translateExaminerType`, `ObservationRequest`, `ValidationResultDTO`, `Object[` en Modalities, `ExaminerEvaluationRepository`.

### Bug pre-existente detectado — RESUELTO (8/2026)
`DocumentEditRequestService` (~:557-592): al RECHAZAR una solicitud de edición la modalidad vuelve al estado previo a la solicitud restaurado desde el historial (fallback `EXAMINERS_ASSIGNED`), vía `ModalityStatusTransition.transition`. El `setStatus(PROPOSAL_APPROVED)` inconsistente con comentario/historial ya NO existe.

## Phase 2.6 — Report: validación, advice y traducciones unificadas (DONE 8/2026)

### Bloque 1 — Seguridad y drop-ins byte-idénticos
- `/reports/health` (`GlobalModalityReportController:355`) ganó `@PreAuthorize("hasAuthority('PERM_VIEW_REPORT')")` — ahora **28/28 endpoints** del módulo lo tienen.
- `DefenseCalendarReportService.translateExaminerType` (privado, ~705) ELIMINADO → caller `mapToExaminerInfo` (:687) usa `TranslationUtils.translateExaminerType` (salida byte-idéntica, null→"Jurado").
- Switch inline de jurado en `ModalityTraceabilityReportService:175` → `TranslationUtils.translateExaminerType` (byte-idéntico).

### Bloque 2 — Advice (`GlobalExceptionHandler`)
- Nuevo `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` → 400 con texto FIJO `"Parámetro con formato inválido"` (sin leak; shape idéntico a los demás handlers). Arregla `year=abc`/`periods=xyz` que antes daban 500 genérico. (Orden del handler: delante de `DateTimeParseException`.)

### Bloque 3 — Fechas de defense-calendar
- `GlobalModalityReportController:973-974` y `:1007-1008`: `String startDate/endDate` + parseo manual → `@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime`. Formato malo → cae en el nuevo handler 400.
- `DefenseCalendarReportService:60-62`: validación `startDate != null && endDate != null && startDate.isAfter(endDate)` → `ValidationException("La fecha de inicio no puede ser posterior a la fecha de fin")` → 400. Defaults intactos (`now() ± 3 meses`).
- Campos muertos eliminados: `ModalityReportFilterDTO` (startDate/endDate, ~41/46) y `CompletedModalitiesFilterDTO` (startDate/endDate, 43-44) — NINGÚN service los leía (Jackson ignora campos desconocidos; sin impacto API).

### Bloque 4 — Resolución de programa (`ReportUtils.getAuthenticatedUserProgram`, ~:132-148)
- Firma intacta (7 callers). Nuevo comportamiento: vacío → `ValidationException`; size==1 → su programa; múltiples → prefiere la autoridad `ProgramRole.PROGRAM_HEAD` si hay EXACTAMENTE 1; si no → `ValidationException` ("más de un programa... no se puede determinar"). Eliminado el `get(0)` arbitrario.
- `DefenseCalendarReportService:52-54`: lógica local de programa (preferencia PROGRAM_HEAD + fallback) reemplazada por `ReportUtils.getAuthenticatedUserProgram(...)`; campo muerto `academicProgramRepository` + imports (`ProgramRole`, `AcademicProgramRepository`) eliminados.
- Controller: `catch (BusinessException e) { throw e; }` insertado antes de CADA `catch (Exception e)` que devolvía 500 con `e.getMessage()` (26 bloques: :110, :138, :170, :195, :223, :256, :281, :317, :347, :435, :474, :499, :532, :557, :591, :622, :655, :683, :716, :744, :995, :1038 + 4 de trazabilidad :805, :834, :872, :912). Anomalía: en los 4 endpoints de trazabilidad el rethrow va ANTES del `catch (RuntimeException e)` (no entre RuntimeException y Exception) para que no quede código muerto — única posición funcional. Los `catch (DocumentException | IOException e)` no se tocaron. NOTA (cierre Fase 6): los 13 `catch (IllegalArgumentException e)` (400) fueron ELIMINADOS — el advice global ya los mapea a 400; solo quedan los 2 `catch (RuntimeException e)` de PDF con mensaje fijo.

### Bloque 5 — Traducciones unificadas hacia `TranslationUtils` (texto cambia en 2 reportes — decisión de negocio)
- `ModalityTraceabilityReportService`: eliminados `translateStatus` (~357-407, callers :98/:261), `translateDocumentStatus` (~409-427, caller :232) y `translateDistinction` (~429-448, caller :302) → `TranslationUtils.translateModalityProcessStatus` / `translateDocumentStatus` / `translateAcademicDistinction`. Header "Traductores" eliminado; import `DocumentStatus` muerto eliminado.
- `DefenseCalendarReportService`: eliminado `translateStatus` (~728-778, callers :227 `mapToUpcomingDefense` y :273 `mapToInProgressDefense`) → `TranslationUtils.translateModalityProcessStatus`.
- NOTA: `translateDocumentStatus`, `translateAcademicDistinction`, `translateModalityProcessStatus`, `translateExaminerType` YA eran canónicos en `TranslationUtils` (7 métodos estáticos: `TranslationUtils.java:18,70,91,111,120,124,135`) — NO se duplicaron. Solo `translateExaminerType` es byte-idéntico; status/distinción/documento cambian strings (p.ej. "Aprobada"→"Aprobado", "Mención Meritoria"→"Meritorio", "Pendiente de revisión"→"Pendiente"). Aceptado.
- `translateModalityType` (`DefenseCalendarReportService`, ~694) CONSERVADO (callers externos; fuera de alcance).

### Limpieza
- `enums/ExportFormat.java` ELIMINADO (0 usos en `src/main`, verificado).
- `CompletedModalitiesPdfGenerator.translateDistinction(String)` (:598) CONSERVADO (String-keyed, distinto de `translateAcademicDistinction(AcademicDistinction)`; fuera de alcance).

### Verificación final
- `.\mvnw.cmd -q clean compile` → EXIT=0.
- `.\mvnw.cmd test` → 8/9 pass; único error = `SigmaApplicationTests.contextLoads` por `MAIL_HOST` (pre-existente, no relacionado con 2.6).
- Greps de cierre = 0: `@CrossOrigin` (todo el repo), `ExportFormat`, `translateStatus`, `translateExaminerType`/`translateDocumentStatus` privados en report (queda solo `CompletedModalitiesPdfGenerator.translateDistinction(String)`), `get(0)` en `ReportUtils` (solo branches guardados size==1 / head único).

### Fuera de alcance (anotado)
- `ModalityServiceUtils.translateAcademicDistinction` (3 callers en report + 8 en DefenseModalityService) — decisión cross-módulo.
- `StudentModalityListingService:1142` y `ModalityCatalogService:350-355` tienen el mismo anti-pattern `get(0)` de `ReportUtils` — mismo fix aplicable si se quiere.
- Los `catch (Exception e)` del controller siguen devolviendo 500 con `e.getMessage()` para excepciones NO tipadas (no migración HTTP; solo rethrows de `BusinessException`).

## Fase 2 — Cierre: academic (2.1), Users (2.2), documents (2.3) + Lote C hardening (DONE 8/2026)

### 2.1 academic (declarado completo)
- Ítem 4 (try/catch en controllers + `Map.of` de éxito = contrato JSON) ya cumplido — sin cambios.
- Imports muertos eliminados: `AcademicProgramRepository:4` (NotBlank), `ProgramDegreeModalityRequest:6` (Service).

### 2.2 Users — `assignAuthority` unificado (AdminService, helpers PRIVADOS)
- `assignAuthority` (:337) y `assignAuthorityChecked` (:347) ahora cubren 7 sitios. Migrados: `assignExaminerToAdditionalProgram` (:390, con exists-check de rol EXAMINER + mensaje), `assignExaminerToMultiplePrograms` (:480, loop con semántica skip intacta: null/ya-asignado → continue, SIN rollback per-item — no existía), `registerUserByAdmin` 2 ramas (:687, :721). `assignExaminer` ya usaba el helper. Único `ProgramAuthority.builder()` restante = dentro del helper.

### 2.2 Users — `getUsers` paginado (único cambio de shape: `/admin/getUsers`)
- `UserRepository.findUsersByFilters`: JPQL extendido con `LEFT JOIN StudentProfile` + `:academicProgramId`/`:facultyId` (IS NULL OR), `LEFT JOIN u.roles` para el filtro de rol por EXISTS, `ORDER BY u.id ASC`, devuelve `Page<User>` (Pageable).
- `AdminService.getUsers(status, role, academicProgramId, facultyId, name, lastName, email, page, size)` → `PaginatedResponse<UserResponse>` (common/web). Filtros en memoria eliminados; batch de perfiles solo de la página.
- `AdminController.getUsers`: +`@RequestParam page` (default 0) / `size` (default 10).
- OJO: usuarios sin perfil NO son filtrables por programa/facultad (inherente al LEFT JOIN + IS NULL) — comportamiento aceptado.

### 2.2 Users — EAGER→LAZY (`User.roles`) con auditoría previa
- Auditoría: TODOS los callers de `getRoles()`/`getAuthorities()`/`getPermissions()` seguros — principal cargado vía `findByEmail` (con fetch) o dentro de tx. 0 callers fuera de tx navegando roles sin fetch. Listeners de notificaciones no navegan roles.
- `User.java:45` `roles` → `FetchType.LAZY`. `Role.java:26` `permissions` SE QUEDA EAGER (decisión del plan).
- `UserRepository.findByEmail` → `@EntityGraph(attributePaths = "roles")` (cubre login: `AppConfig`/UserDetailsService y `JwtAuthenticationFilter`).
- Nota: `findAll()`+roles puede generar N+1 (ModalityCatalogService/AdminService) — coste menor que el join EAGER previo; si molesta: `@BatchSize`/EntityGraph en `findAll`.

### 2.2 Users — limpieza item 6
- Imports muertos en `ProgramAuthorityRepository` (StudentModality, ModalityProcessStatus) eliminados.
- `throws MalformedURLException` decorativo ELIMINADO de `StudentController:142` y `StudentService:452`... PERO `viewMyDocument` SÍ lanza checked (`UrlResource(URI)`): ahora try/catch local → `NotFoundException("Archivo no encontrado o no legible")` (:469).
- Ítem 4 (Bean Validation): 6/6 DTOs con constraints + `@Valid` 12/12 en AdminController; `AuthRequest`/`ResetPasswordRequest` SIN constraints = deliberado (register/login excepción documentada 1.3).

### 2.3 documents — servicios puros (SecurityUtils fuera de services)
- `DocumentService`: `getDocumentHistory(Long, User)` (:206), `uploadCancellationDocument(Long, MultipartFile, User)` (:253), `getDocumentCancellation(Long, User)` (:236) — check líder inline → `ResourceAccessPolicy.requireLeader`; callers (`StudentController:91-94/:121-132`, `ModalityController:365`) pasan `SecurityUtils.getCurrentUser()`. Import SecurityUtils eliminado.
- `ProjectTitleService.getProjectTitle(Long, User)` — caller `ProjectTitleController:38` pasa `SecurityUtils.getCurrentUser()`; import eliminado.
- Módulo documents: 0 usos de SecurityUtils en services (solo controllers).
- `ResourceAccessPolicy` consolidación: `StudentService.viewMyDocument` → `requireActiveMember` (:461); `DocumentModalityService.isAuthorizedForDocument` → composición `tryRequire(requireLeader/requireActiveMember/requireAssignedExaminer)` + director y 2-rol authority INLINE (sin primitiva para director ni RoleIn de 2 roles) — semántica byte-equivalente, mensaje único "No tienes permiso para ver este documento" preservado. Patrón `tryRequire` igual al de ProjectTitleService.
- Ítems 3 (ProposalEvaluationService) y 4: N/A / ya cumplidos (lógica pura vía 2.5).

### Lote C — hardening
- `AcademicProgram.name` → `@Column(unique=true)` (duplicados → 409 vía DataIntegrityViolation). ⚠️ Riesgo: si la BD dev ya tiene duplicados, `ddl-auto=update` falla al arrancar.
- `AuthService:73` rol no encontrado → `NotFoundException` (404).
- `StudentService:179-180` re-wrap `e.getMessage()` → `ValidationException("No fue posible procesar el PDF.")` + `log.error` (sin leak).
- `ProjectTitleService:80` y `DocumentEditRequestService:90` → `NotFoundException`.

### Verificación final (8/2026)
- `.\mvnw.cmd -q clean compile` → EXIT=0 (2 iteraciones: fix `viewMyDocument` try/catch + import MalformedURLException).
- `.\mvnw.cmd test` → 8/9; único error = `SigmaApplicationTests.contextLoads` por `MAIL_HOST` (pre-existente). `mvnw test -Dtest='!SigmaApplicationTests'` → 8/8 EXIT=0.
- Greps de cierre: `jakarta.transaction` = 0; `ResponseEntity` en services = solo `AuthService` (deliberado); `SecurityUtils` en documents/services = 0; `User.roles` LAZY + `findByEmail` con EntityGraph verificados.
- `e.getMessage()` restantes: patrones pre-existentes documentados (report controller 2.6, catch-all genéricos 1.2, logs).

### Fuera de alcance (anotado)
- 6 `RuntimeException` en `DocumentEditRequestService` (:160,:225,:354,:660,:811,:952) — mismo patrón 1.2; migrar si se quiere.
- `StudentService:175` / `StudentModalityListingService:1243` : `ValidationException(e.getMessage())` pre-existentes (leak acotado, 400).
- `ValidationException` en `StudentModalityListingService:1246`, `SeminarModalityService` (10 bloques), `DocumentEditRequestService:1068`, `DocumentService:322` — fallbacks catch-all genéricos (patrón 1.2 documentado).

## Fase 3 — Report: datos y consultas (DONE 8/2026)

### Correcciones a premisas (verificadas con explore + lectura directa)
- **8 `findAll()`** (no 6) y **~30 loops N+1** (no ~19).
- El doble `findAll()` NO está en `ModalityHistoricalPdfGenerator` (los 8 PDF son renderers puros, no tocan BD) sino en `HistoricalReportService:53+:474` y `ComparisonReportService` (1+N por periodo histórico).
- `@Transactional(readOnly = true)`: **los 10 services ya lo tenían por método (14/14)** — el ítem "solo falta CompletedModalitiesReportService" era falso → no-op.

### Bloque A — Fundación batch (3 repos + ReportUtils)
- Nuevos métodos: `StudentModalityMemberRepository.findByStudentModalityIdInAndStatus`, `DefenseExaminerRepository.findByStudentModalityIdIn` (@Query con ORDER BY examinerType), `DefenseEvaluationCriteriaRepository.findByDefenseExaminerIdIn`.
- `ReportUtils` (nuevos helpers estáticos): `loadActiveMembersByModalityIds` → `Map<Long,List<Member>>`, `loadProfilesByUserIds` → `Map<Long,StudentProfile>` (clave = id del perfil = userId vía @MapsId), `loadExaminersByModalityIds`, `loadCriteriaByExaminerIds` + overload `buildStudentInfos(members, Map)`. El `buildStudentInfos(members, repo)` original ahora batchea internamente (`findAllByUserIdIn` ya existía).

### Bloque B — `findAll()` → `findForProgramHead` (push-down del filtro por programa)
- 7 sitios: `GlobalReportService:112,165`, `HistoricalReportService:52`, `DefenseCalendarReportService:64`, `DirectorAssignedModalitiesReportService:145` (branch onlyActive=false), `StudentListingReportService:52`, `CompletedModalitiesReportService:51`. Los **globales** (`StudentReportService`/`DirectorReportService`, filtro por tipo sin programa) NO se tocaron.
- Regla conservadora: se CONSERVA el filtro en memoria por programa (inofensivo; evita drift si `academicProgram` y `programDegreeModality.academicProgram` divergieran). Ambos campos son `optional=false` en la entidad → `findForProgramHead` nunca excluye filas válidas.
- `findByIdWithMembers` (JOIN FETCH members, 0 callers) ACTIVADO en `ModalityTraceabilityReportService:52`.

### Bloque C — Cargas duplicadas eliminadas
- `HistoricalReportService.calculateRankingPosition` YA NO relee la tabla: recibe `programModalities` (carga única vía `findForProgramHead`) → 1 scan por request.
- `ComparisonReportService`: `getModalitiesForComparison` carga UNA vez y el loop de periodos filtra en memoria (1+N → 1).

### Bloque D — N+1 eliminados (miembros: 21 sitios → 1 query por sección)
- Members: `loadActiveMembersByModalityIds` + `getOrDefault(id, List.of())` en los 9 services.
- Perfiles: `findByUserId`/`findById` por miembro → `loadProfilesByUserIds` (batched).
- Jurados/criterios: `DefenseCalendarReportService:162,287,290,546,580` → `loadExaminersByModalityIds`/`loadCriteriaByExaminerIds` (mata el flatMap N+1 de toda la tabla en `buildExaminerAnalysis`).
- Campo muerto `userRepository` ELIMINADO de `DefenseCalendarReportService`.
- Único `findByStudentModalityId(` restante en report = `ModalityTraceabilityReportService:178` (single-modality, legítimo).

### Bloque E — Counts en memoria + bug fix pre-existente
- `countByStudentModalityIdAndStatus` → `membersByModality.getOrDefault(id, List.of()).size()`.
- `countActiveModalitiesByLeader(directorId, ...)` consultaba `sm.leader.id` (líder estudiante) pero recibía un **id de director** → contaba ~0 (bug pre-existente). Reemplazo en memoria por `sm.getProjectDirector().getId().equals(...)` + `getActiveStatuses().contains(status)`. ⚠️ **Cambio de salida**: ahora `activeProjectsCount`/`totalProjects` devuelven el valor real (antes ~0). Consecuencia en `DirectorReportService:56`: `totalProjects == activeProjects` (la sección solo tiene activas; `completedProjects=0` hardcodeado pre-existente). **Validar con negocio.**

### Verificación (8/2026)
- `.\mvnw.cmd -q clean compile` → EXIT=0.
- `mvnw test -Dtest='!SigmaApplicationTests'` → 8/8 EXIT=0 (baseline).
- Greps de cierre en report = 0: `findAll()`, `findByStudentModalityIdAndStatus`, `countActiveModalitiesByLeader`, `countByStudentModalityIdAndStatus`, `findByUserId(`, `studentProfileRepository.findById`, `findByDefenseExaminerId(`.
- Caché: NO añadida (premisa del plan "solo si las métricas lo piden" — diferido). Medida runtime pendiente (requiere entorno vivo).

### Fuera de alcance (notas)
- `modality.getMembers()` en `DefenseCalendarReportService` (`buildStudentList`/`buildMonthlyAnalysis`) sin tocar (no es consulta por ítem).
- `studentProfileRepository.findById` y `findByUserId` son equivalentes (@MapsId) — batch único `findAllByUserIdIn` cubre ambos.

## Fase 4 — División de god classes (DONE 8/2026)

### Alcance y decisiones vinculantes
- Plan de 6 trabajos; **Trabajo 3 OMITIDO por decisión del usuario**: `GlobalModalityReportController` (1.042 líneas, 18 deps, 28 endpoints) NO se divide — sigue como está en `report/controller`.
- **NO** se creó `ModalityStatusService` (el bean `ModalityStatusTransition` ya cumple ese rol) ni `ProgramAssignmentService` (la asignación de programa ES la autoridad).
- Los services originales se BORRARON (sin fachadas); los endpoints públicos no cambiaron; los controllers solo se adaptaron en inyección/call sites.
- Se verificó antes de despachar: sin colisiones de nombres para las 7 clases nuevas.

### Trabajo 1 (agente) — DocumentModalityService (3.139 líneas) → 2
- **`DocumentWorkflowService`** (~2.150 líneas, 28 métodos: flujo, aprobaciones, correcciones, rúbrica, consenso, tiebreaker, cierre; 17 campos + dep ModalityDocumentService; sin `@Slf4j`).
- **`ModalityDocumentService`** (~800 líneas, 12 métodos: upload/view/resubmit, `checkAndUpdateModalityStatusIfAllMandatoryDocsUploaded`, 2 validadores de comité ahora públicos, `isAuthorizedForDocument`/`tryRequire`; `@Value uploadDir` viajó aquí).
- Borrados 2 privados muertos (`checkIfAllMandatoryDocumentsAcceptedByAllExaminers`, `checkIfAllDocumentsAcceptedByAllExaminers`). `ModalityController` rewireado.

### Trabajo 4 (agente) — AdminService (738 líneas) → 3
- **`UserAdminService`** (382 líneas, 10 públicos + `toUserResponse`): roles/permisos/usuarios/`getUsers` paginado/`registerUserByAdmin`.
- **`AuthorityAssignmentService`** (309 líneas): 8 asignaciones + `assignAuthority`/`assignAuthorityChecked` PÚBLICOS + helper nuevo `assignExaminerToPrograms(User, List<Long>) → AssignmentResult(assigned, skipped)` + record `AssignmentResult`. Los 2 loops multiprograma (assignExaminerToMultiplePrograms y registerUserByAdmin) fusionados en el helper, sin divergencia funcional.
- **`AdminCatalogService`** (60 líneas): solo `getModalities`. `AdminController` rewireado.

### Trabajo 2 (agente) — DefenseModalityService (1.799 líneas) → 2
- **`DefenseEvaluationService`** (1.042 líneas, 8 públicos + 9 privados): `registerFinalDefenseEvaluation` entero, consenso/tiebreaker/distinciones + 4 consultores; 8 campos. El `setStatus` directo `UNDER_EVALUATION_PRIMARY_EXAMINERS` se movió con `processPrimaryExaminerEvaluation`.
- **`DefenseWorkflowService`** (835 líneas, 10 públicos, 0 privados): schedule/approve/reschedule/assign + 3 consultores. `getPendingDefenseProposals`/`getExaminerTypeForModality` eran `@Transactional` sin readOnly y se conservaron byte-idénticos. `ModalityController` rewireado (18 call sites).

### Trabajo 5 (coord.) — StudentNotificationListener (870 líneas, NO era god class)
- Extraídos 2 helpers privados: `activeMembers(StudentModality)` (19 queries duplicadas `findByStudentModalityIdAndStatus` → 1) y `dispatchToActiveMembers(modality, type, triggeredBy, subject, Function<User,String>)` (11 loops idénticos buildAndDispatch → 1). Los 6 handlers con `log.info` per-member conservan su loop manual (usa `activeMembers`); los 2 con PDF adjunto y el de líder se mantienen byte-idénticos.
- **NO se movieron los subjects a `NotificationMessageTemplates`** (decisión YAGNI del coordinador): son strings únicos usados 1 vez y los otros 4 listeners también los mantienen inline; moverlos = +26 métodos sin dedup.

### Trabajo 6 (coord.) — StudentModalityListingService (1.274 líneas, NO era god class)
- Dead code ELIMINADO en los 4 listados (programHead/committee/director/examiner): `activeMembers` + `studentNames` + `studentEmails` computados y descartados (toModalityList ya los recalcula y SÍ los usa) → se ahorra 1 query + 2 joins por fila.
- Dedup de cálculos: `calculateDaysRemaining`, `computeStatusFlags` → record `StatusFlags`, `countApprovedDocs`/`countPendingDocs`/`countRejectedDocs`; el inline de historial de `getCurrentStudentModality` → `buildStatusHistory()`. **Los dos builders NO se fusionaron** (difieren en `modalityType`, `modalityId`, `defenseProposedBy` y docs — fusionar cambiaría el JSON).

### Verificación (8/2026)
- `.\mvnw.cmd -q clean compile` → EXIT=0 (fix: `status` local re-añadido en los 2 builders).
- `mvnw test -Dtest='!SigmaApplicationTests'` → 8/8 EXIT=0.
- Greps de cierre: `DocumentModalityService`/`DefenseModalityService`/`AdminService` = 0 en src; `GlobalModalityReportController` = 1 archivo (Work 3 omitido); `findByStudentModalityIdAndStatus` en StudentNotificationListener = 1 (el helper).
- Smoke runtime pendiente (requiere entorno vivo con MySQL/`.env`).

### Fuera de alcance (anotado)
- `ExaminerNotificationListener` conserva 2 queries directas (79, 621) — patrón `activeMembers` aplicable si se quiere (textos inline pendientes de la Fase 2.4).
- `DefenseWorkflowService:779-790` conserva su propio `activeMembers`/`studentNames`/`studentEmails` (es un DTO distinto del `ModalityListDTO`).
- `DocumentEditRequestService:664` similar (modalityInfo para el correo) — no comparte shape con el listing.

## Fase 5 — Reportes PDF: dedup de barras/tarjetas (Bloque 1 DONE, Bloques 2-4 pendientes, 8/2026)

### Objetivo
Eliminar ~1.000 líneas de duplicación en los 8 generadores PDF (`report/service`). Solo se moviliza lo que tiene ≥2 copias (YAGNI). Bloque 1 = barras de 3 columnas + tarjetas colored-fill.

### Bloque 1 — API canónica nueva en `InstitutionalPdfHeader`
- `addBarRow(PdfPTable, String label, String barText, String valueText, float percentage, BaseColor)` (~:453): fila label(BOLD 9 TEXT_BLACK) | `createValueBar(barText, percentage, color)` | valor(BOLD 10 color), widths `{1.5,4,1.5}`. `percentage` es FRACCIÓN 0-1 (convención de `createValueBar`).
- Overload `addBarRow(PdfPTable, String label, String text, float percentage, BaseColor)` (~:490): barText == valueText.
- `addMetricCard(PdfPTable, String label, String value, BaseColor)` (~:498): tarjeta rellena BOLD 16 WHITE + label 8 `LIGHT_GRAY` (constante nueva ~:36 `(240,240,240)`), fixedHeight 60, padding 10.
- IMPORTANTE: son STATIC de otra clase → TODAS las llamadas van calificadas `InstitutionalPdfHeader.addBarRow(...)` (2 agentes lo omitieron; compile lo atrapó y se calificó).

### Migrado (métodos privados BORRADOS ~18)
- `CompletedModalitiesPdfGenerator` (1.502→1.258): `addApprovalBar`, `addTimeBar`, `addGradeBar`, `addDistinctionBar`, `addPeriodEvolutionBar`, `addMetricCard`, `addStatsCard` → 39 call sites.
- `DefenseCalendarPdfGenerator` (850→628): `createSuccessRateBar` (→`createValueBar` inline en `addSuccessRateIndicator`, estructura intacta), `addDistributionBar`+`createDistributionBarCell`, `addGradeBar`+`createGradeBarCell`, `addMonthEvolutionBar` (valueText `"N completadas | xx.x%"`, color umbral `successRate >= 70`).
- `StudentListingPdfGenerator` (1.504→1.312): `addTimelineBar`+`createEnhancedTimelineBar`, `addEnhancedDistributionBar`+`createEnhancedDistributionBarCell` (conserva `N/D` del Map y `truncate(label,40)`), `addMetricCard`.
- `ModalityHistoricalPdfGenerator` (1.633→1.568): `addEnhancedPeriodBar` (conserva indicador ▲/▼/● y umbral `>= avgValue`), `addMetricCell` (12 call sites).

### Conservados (deliberado, 1 copia cada uno)
`createBarCell`+`addStatsCard` (StudentListing, label-first), `addStatCell` (Historical, 3 líneas), `addStatCard` (Defense, bg alpha), `addMetricCard` de Director/Comparison/Traceability (bordered-white) + `addEnhancedStatisticBar` (Director, document-level). Barras 2-col de Comparison (`createProgressBarTable`, `createMiniProgressBar`) NO se tocan (escala 0-100 interna).

### Micro-drift visual aceptado
Piso de barra 3→5% (createValueBar), bordes 1→0.5, paddings/fuentes de label 8-10→9, tarjeta stats 14/55→16/60, valueText multi-chunk → frase plana.

### Verificación Bloque 1
- `.\mvnw.cmd -q clean compile` → EXIT=0 (1 iteración: calificar 11 llamadas sin `InstitutionalPdfHeader.` en Defense+StudentListing).
- `mvnw test -Dtest='!SigmaApplicationTests'` → 8/8 EXIT=0.
- Greps = 0 en src: los 13 nombres de métodos borrados; `addGradeBar` = 0; llamadas sin calificar restantes = solo métodos privados conservados (verificado).

### Bloques pendientes
- **B2**: footers → `InstitutionalPageEventHelper(String programName, String footerCenterText)`; borrar `PageEventHelper` (Comparison:986-1017) y `FooterEvent` (Traceability:777-814) + swap de constantes a header.
- **B3**: paleta → borrar literales `(213,203,160)` y re-declaraciones en Traceability/helper.
- **B4**: `BaseReportPdfGenerator` abstracto (openDocument/newPageWithHeaderLightHeader|FullHeader/close) → 8 generadores `extends`.

## Fase 5 — Bloques 2-4 (DONE 8/2026)

### Bloque 2 — Footers unificados
- `InstitutionalPageEventHelper` ahora tiene constructor `(String programName, String footerCenterText)`; overload `(programName)` delega con `footerCenterText = programName`. Borrado el `footerCenterText` que se ponía como programName (antes la línea central SIEMPRE era el programa). Constantes `GOLD/RED/GRAY` locales → `InstitutionalPdfHeader.INST_GOLD/INST_RED/TEXT_GRAY`.
- **`PageEventHelper`** (ModalityComparisonPdfGenerator, clase anidada, ~986-1017) ELIMINADO → usa `InstitutionalPageEventHelper`. **`FooterEvent`** (ModalityTraceabilityPdfGenerator, clase anidada, ~777-814) ELIMINADO → mismo. Todos los demás generadores usaban el helper canónico (mismo nombre) y solo se les añadió el arg del centro cuando aplica.
- Swaps aplicados: Comparison `new InstitutionalPageEventHelper(programName)` → `InstitutionalPageEventHelper(programName, "Reporte Comparativo de Modalidades")`; Traceability → `(programName, "Reporte de Trazabilidad — Modalidad #id")`.

### Bloque 3 — Paleta
- Literal dorado institucional `(213,203,160)` consolidado en `InstitutionalPdfHeader.INST_GOLD` (solo ahí queda; verificado `new BaseColor(213,...)` = 0 fuera de `InstitutionalPdfHeader`).
- Quedan `new BaseColor(...)` de un solo uso (semánticos: verdes de éxito, naranjas de alerta, `ROW_ALT`/`RED_SOFT`/etc. de Traceability) — NO se tocan (1 copia cada uno, YAGNI).

### Bloque 4 — `BaseReportPdfGenerator` (nuevo, `report/service`)
- API: `PdfSession(Document, PdfWriter, ByteArrayOutputStream)` record + `openDocument(Rectangle size, ml,mr,mt,mb, String programName, String footerCenterText)` (abre documento, registra `InstitutionalPageEventHelper` si programName != null) + `newPageWithHeader(session,title)` / `newPageWithLightHeader(session,programName)` / `newPageWithFullHeader(session,faculty,program,subtitle)` (newPage + header correspondiente) + `close(session)`.
- `openDocument` param es `Rectangle` (los callers pasan `PageSize.A4`/`PageSize.A4.rotate()`, que son `Rectangle`; `PageSize` como tipo rompía la firma del `new Document(Rectangle,...)`) — 1 iteración de fix.
- Los 8 generadores (`PdfReport`, Completed, Defense, Director, Historical, Comparison, Traceability, StudentListing) `extends BaseReportPdfGenerator`; cada uno conserva su propio `generate()` (el base NO define el flujo). `generatePDF`/`generate*` de cada uno: `PdfSession session = openDocument(...)` → secciones con `newPageWithHeader`/`newPageWithLightHeader`/`newPageWithFullHeader` → `close(session)`.
- Detalle por generador (pares `document.newPage()+header` → métodos base):
  - Completed 8 secciones, Defense 7 (portada → light header; la portada sigue en `addCoverPage`, el newPage posterior de la portada quedó standalone `session.document().newPage()` en su `addCoverPage` como en el original), Historical 9, Comparison 8, Traceability 4, StudentListing 7, Director 5, PdfReport 4 + `addModalityDetails` (sección 6) conserva su `document.newPage()+addInternalHeader` local porque recibe `Document` y no `PdfSession` (única copia, YAGNI).
- `addCoverPage(session.document(), report)` + `document.newPage()` al final de cada `addCoverPage` privado conservados (separación portada/primera página, como el original). `DirectorAssigned:384` `document.newPage()` en loop por director conservado.
- PdfReport sección 4 (`addVisualDistributions`): SOLO `addInternalHeader` SIN newPage, igual que el original (el original no hacía newPage antes de esa sección).

### Verificación (8/2026)
- `.\mvnw.cmd -q clean compile` → EXIT=0 (1 iteración: `openDocument(PageSize size,...)` → `Rectangle`).
- `mvnw test -Dtest='!SigmaApplicationTests'` → 8/8 EXIT=0.
- Greps de cierre: `class PageEventHelper`/`class FooterEvent` = 0; `document.newPage()` restantes en generadores = solo los 3 casos documentados (fin de addCoverPage x2 + loop de director); `new Document(` restantes = solo en `BaseReportPdfGenerator:27`.
- Smoke runtime pendiente (requiere entorno vivo con MySQL/`.env`).

### Fuera de alcance (anotado)
- `PdfReport:42` (sección 4, addInternalHeader sin newPage) y `PdfReport:353-354` (addModalityDetails con Document) — comportamiento original conservado, no duplicación.
- `addCoverPage` privados siguen en cada generador (1 copia cada uno, no comparten shape completo).

## Fase 6 — Limpieza excepciones, carpeta de archivos y verificación Swagger (DONE 8/2026)

### Bloque D2 — Bug carpeta de archivos del estudiante (helper único)
- **Bug**: 4 sitios de escritura construían la carpeta del estudiante con el lastName DUPLICADO y SIN separador entre name y lastName: {name}{lastName}_{lastName}_{id}. Además, clave inconsistente: 2 sitios usaban student.getId() y 2 usaban studentModalityId/studentModality.getId() (misma persona → carpetas distintas).
- **Fix**: TranslationUtils.studentFolder(name, lastName, id) (common/util, ~:197) → {name}_{lastName}_{id} sanitizado ([^a-zA-Z0-9]→_). Los 4 sitios migrados:
  - StudentService:501 (historial académico) — antes NO sanitizaba.
  - ModalityDocumentService:154 (upload) y :789 (resubmit) — ambos ahora con student.getId() (consistencia).
  - DocumentService:311 (cancelaciones) — ahora con getLeader().getId() (antes key = modalityId).
- **OJO (decision)**: la lectura NO usa el helper — usa la ruta absoluta document.getFilePath() guardada en BD, por lo que los archivos viejos (carpeta con patrón antiguo) siguen siendo legibles. Sin fallback de lectura (YAGNI). La corrección solo afecta carpetas NUEVAS.

### Bloque D3 — 0 RuntimeException con mensaje concatenado al cliente
- 15 sitios 
ew RuntimeException("...: " + e.getMessage()) → 
ew RuntimeException("...", e) (mensaje fijo + causa para logs). El handler genérico ya devolvía 500 sin leak; ahora el texto interno tampoco queda en la cadena de excepción.
  - SeminarModalityService (10 catch-alls), StudentModalityListingService:1149, DocumentEditRequestService:1068, DocumentService:322.
- 2 sitios de reporte eran **404 de dominio** encubiertos: ModalityTraceabilityReportService:53 (modalidad no encontrada) y :125 (sin modalidad activa) → NotFoundException (import añadido).
- 2 catch-alls que re-envolvían genérico como ValidationException(e.getMessage()) (400 con texto interno) → RuntimeException(msg, e) (500 genérico): StudentModalityListingService:1146 y DocumentEditRequestService:1066.

### Bloque D4 — Report controller: 500 sin leak de mensaje
- GlobalModalityReportController: 24 bloques catch (Exception e)/DocumentException|IOException que devolvían 500 con e.getMessage() → mensaje FIJO (prefijo existente, sin concatenar). Shape JSON jsonError/buildErrorResponse intacto.
- NOTA (cierre Fase 6): los 13 `catch (IllegalArgumentException e)` → 400 con e.getMessage() y los 2 `catch (RuntimeException e)` de trazabilidad JSON fueron ELIMINADOS — el advice global mapea IllegalArgumentException→400. Quedan 2 `catch (RuntimeException e)` en endpoints PDF de trazabilidad con mensaje fijo (sin leak).

### Bloque E — Swagger verificado (sin cambios)
- SwaggerConfig completo (OpenAPI + bearer-jwt + GroupedOpenApi public). Controllers con @Tag/@Operation/@ApiResponses/@SecurityRequirement generalizado; AuthController público sin @SecurityRequirement (intencional). Nada que añadir.

### Bloque F — Verificación
- .\mvnw.cmd -q clean compile → EXIT=0.
- mvnw test -Dtest='!SigmaApplicationTests' → 8/8 EXIT=0.
- Greps de cierre = 0: getLastName() + "_" (patrón duplicado), 
ew RuntimeException("...: " + e.getMessage()), INTERNAL_SERVER_ERROR, "Error.*e.getMessage en report controller.
- Fix colateral pre-existente: BlackListedTokenRepository.deleteByExpiresAtBefore devolvía oid pero BlackListedTokenCleanupScheduler esperaba int (rompía compile) → int (Spring Data derived delete).

### Fuera de alcance (anotado)
- StudentService:176 — RESUELTO (cierre Fase 6): mensaje FIJO + `logger.warn` del parser; ya no hay leak de `e.getMessage()`.
- Sin 5 bloques IllegalArgumentException en report controller: ELIMINADOS en el cierre Fase 6 (advice global → 400).

## Cierre Fase 6 — Lote final de limpieza (8/2026)

- **0-1**: `AcademicCertificateTestController` → `@Profile("dev")` + import (antes: expuesto en prod, drift inverso: AGENTS.md lo documentaba como aislado pero el código no lo estaba).
- **1-1**: `SeminarModalityService.listSeminars`/`enrollInSeminar` → `catch (BusinessException e) { throw e; }` antes de los catch-all genéricos. Ahora el service tiene 10 bloques `catch (BusinessException)` ante 10 `catch (Exception)` (conteo verificado).
- **1-2**: `ModalityEvent` — `0L` mágico de `studentModalityId` → `null` en `SEMINAR_STARTED`/`SEMINAR_CANCELLED` (grep `0L, null, Map.of` = 0).
- **1-3**: `GlobalModalityReportController` — eliminados 13 `catch (IllegalArgumentException e)` (400 con `e.getMessage()`) + 2 `catch (RuntimeException e)` de trazabilidad JSON. Quedan 2 `catch (RuntimeException)` en endpoints PDF de trazabilidad con mensaje FIJO (sin leak). `e.getMessage()` en el controller = 0. Shape de éxito intacto.
- **2-1**: `StudentModalityRepository.countActiveModalitiesByLeader` eliminado (0 callers).
- **2-2**: `DocumentService` muerto eliminado de `ModalityGroupController` (import + campo).
- **2-3**: `tryRequire(Runnable)` subido a `ResourceAccessPolicy` (common/util, ~:52); `ProjectTitleService.isParticipant` y `ModalityDocumentService.isAuthorizedForDocument` migrados; 2 métodos privados duplicados eliminados (grep `private boolean tryRequire` = 0).
- **2-4**: `isCompleteModality` centralizado en `CertificatePdfSupport` (única definición + 2 callers: `AcademicCertificateTestController:72` y `StudentNotificationListener`); `ModalityController.isCompleteModality` delegado.
- **2-5**: `application-dev.properties` → `frontend.url=${FRONTEND_URL:http://localhost:5173}` (default coherente con prod).
- **2-6**: `StudentService:176` → `ValidationException` con mensaje FIJO + `logger.warn` del parser (antes leak de `e.getMessage()`).
- **3-1**: `HistoricalReportService` métrica fabricada `studentsBySemester = new HashMap<>()` → `null` (comentario `ponytail`; era variable local, no método).
- **Drifts AGENTS.md corregidos**: payloads tipados (DECLINED→reflejo real), `isCompleteModality` "fuera de alcance"→RESUELTO, "no se tocaron los catch IAE"→eliminados, conteo StudentNotificationListener 976→870, Fuera de alcance Fase 6 actualizado.
- Verificación: `.\mvnw.cmd -q clean compile` → EXIT=0; `mvnw test -Dtest='!SigmaApplicationTests'` → 8/8 EXIT=0.
- Greps de cierre = 0: `countActiveModalitiesByLeader`, `private boolean tryRequire`, `0L, null, Map.of`, `IllegalArgumentException e` en report controller, `e.getMessage()` en report controller.

## Fase 0 — Hotfixes críticos (DONE 8/2026)

Implementación de las 12 tareas T0.1–T0.12 del `PLAN_MAESTRO_REFACTORIZACION.md`.

### Fixes aplicados
- **T0.1**: `ModalityController` (:557, :564) `hasRole('ROLE_STUDENT')` → `hasRole('STUDENT')` (el authority real es `ROLE_STUDENT`; antes 403 permanente). Consistente con `ModalityGroupController`.
- **T0.2/T0.3**: `DocumentWorkflowService.closeModalityByCommittee`/`rejectFinalModalityByCommittee` — `String.format` con 2 args y 1 placeholder → pasa solo `reason` (antes el historial registraba "Motivo: MODALITY_CLOSED"). `previousStatus` conservado (se usa en el Map de retorno).
- **T0.4**: `DocumentWorkflowService.approveFinalModalityByCommittee` — precedencia del ternario corregida con paréntesis (antes "Observaciones: null" por `("..."+obs) != null`).
- **T0.5**: `StudentListingReportService` (resumen ejecutivo) — comparaciones enum-name vs texto traducido → texto vs texto vía `TranslationUtils.translateModalityProcessStatus` (antes active/completed siempre 0).
- **T0.6**: `StudentService.updateStudentProfileFromAcademicHistory` — `ValidationException(e.getMessage())` → mensaje FIJO + `logger.warn` del detalle del parser.
- **T0.7**: `DocumentService.describeDocumentStatus` (privado, 6/14 estados) ELIMINADO → `TranslationUtils.translateDocumentStatus` (exhaustivo 14/14, null → "N/A"). Queda `ModalityServiceUtils.describeDocumentStatus` (cross-módulo, fuera de alcance).
- **T0.8**: `DirectorReportService.generateDirectorsByModalityReport` + `StudentReportService` (2 métodos) ganan filtro por programa vía `ReportUtils.getAuthenticatedUserProgram(programAuthorityRepository)` (patrón de GlobalReportService; campo `m.getAcademicProgram().getId()`). `findByStatusIn` conservado.
- **T0.9**: `ResetPasswordRequest.newPassword` → `@NotBlank` + `@Size(min=6,max=60)`; `AuthController` reset-password → `@Valid`. Register/login/forgot SIN @Valid (deliberado, documentado).
- **T0.10**: `ModalityDocumentService` upload — `allowedFormat.contains(extension)` (falsos positivos) → split exacto `Arrays.stream(...split(",")).trim+toLowerCase().toList().contains` (patrón `DocumentService.validateFile`).
- **T0.11**: `SeminarModalityService.cancelSeminar` — antes cancelaba TODAS las modalidades del estudiante (cualquier tipo, terminadas) con `setStatus` sin historial. Ahora solo modalidades en `SEMINAR_CANCELLABLE_STATUSES` (whitelist inline 13 estados "en curso", comentario `ponytail:`) Y del tipo "SEMINARIO DE GRADO" (convención del módulo, :246/:253/:381 — el entity `Seminar` no tiene degreeModality), vía `modalityStatusTransition.transition(..., null, "Modalidad cancelada por cancelación del seminario: " + nombre)` con historial. **⚠️ Decisión de negocio a validar**: nombre del tipo por string y whitelist de estados (alinear con `ReportUtils.ACTIVE_STATUSES` si el negocio lo confirma).
- **T0.12**: `StudentService.saveAcademicHistoryPdf` — bloque de persistencia de `StudentDocument` SIN `documentConfig` ELIMINADO (causaba `DataIntegrityViolationException` tragada → 500 rollback-only; `documentConfig` es `@ManyToOne(optional=false)`). El registro canónico `AcademicHistoryPdf` se conserva. **⚠️ Decisión de negocio a validar**: el historial académico ya NO aparece en "Mis documentos" (opción conservadora b del plan).

### Verificación (8/2026)
- `.\mvnw.cmd -q clean compile` → EXIT=0.
- `mvnw test -Dtest='!SigmaApplicationTests'` → 8/8 EXIT=0.
- Greps: `hasRole('ROLE_` = 0 en src; `describeDocumentStatus` en DocumentService = 0 (quedan solo los de `ModalityServiceUtils`, cross-módulo fuera de alcance); `new ValidationException(e.getMessage())` en StudentService = 0 (quedan 10 en SeminarModalityService: catch-all IAE→ValidationException pre-existentes, patrón Fase 1.2 ya documentado).
- Implementado por 3 sub-agentes general paralelos (Modalities / report / Users+documents) + verificación central del coordinador.

## Fase 1 — Infraestructura de testing (DONE 8/2026)

### Comando canónico de tests (ya sin exclusión)
- `.\mvnw.cmd test` → **36 tests, 0 fallos, 2 skipped** (incluye `SigmaApplicationTests`). El viejo `-Dtest='!SigmaApplicationTests'` queda obsoleto.
- `.\mvnw.cmd -q clean compile` → EXIT=0.
- `.\mvnw.cmd -B verify` → build completo (lo usa CI).

### Infraestructura
- **Perfil `test`** (`src/test/resources/application-test.properties`): **H2 `MODE=MySQL`** (`jdbc:h2:mem:sigma;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER`) — `NON_KEYWORDS=USER` es OBLIGATORIO (H2 reserva `USER`, la entidad mapea a tabla `user`). `ddl-auto=create-drop`, `jwt.secret` fake (base64 ≥32B), `spring.mail.host=localhost`, `file.upload-dir` en tmp, `frontend.url=http://localhost`.
- **Testcontainers DIFERIDO** (decisión): Docker no disponible en el entorno → se usa el fallback del plan (H2 MySQL). Las dependencias testcontainers se añaden cuando haya Docker/CI con Docker (Fases 5/8). Las 2 queries nativas de `SeminarRepository` (tabla `seminar_students`) son H2-compatibles (verificado).
- `pom.xml`: añadidos `com.h2database:h2` (test), `com.tngtech.archunit:archunit-junit5:1.3.0` (test). `spring-security-test` ya estaba (ahora en uso). Surefire: inclusión por defecto (nada que configurar).
- `SigmaApplicationTests`: `@ActiveProfiles("test")` (antes fallaba por `${MAIL_HOST}` sin resolver — el EnvLoader solo corre vía `main()`).
- **CI**: `.github/workflows/ci.yml` — `./mvnw -B verify` en ubuntu + upload del reporte surefire como artifact (main + PRs).

### Tests de regresión de Fase 0 (10/10 cubiertos)
| Hotfix | Test |
|---|---|
| T0.1 (seminarios 403) | `ModalityControllerSecurityTest` — MockMvc + contexto Spring + **JWT real** (`JwtService.generateToken` + filtro recarga de BD). 3 tests: GET available → 200; POST enroll → 400 (ValidationException del service, nunca 403); sin token → 403 |
| T0.2/0.3/0.4 (historial corrupto) | `DocumentWorkflowHistoryTest` — 18 mocks, captor en `ModalityStatusTransition.transition`: observations contiene el reason real, no el estado previo; sin "null" |
| T0.5 (contadores 0) | `StudentListingReportServiceTest` — 2 activas + 1 GRADED_APPROVED → active=2, completed=1 (requiere 1 miembro activo por modalidad: los contadores del DTO se calculan sobre students) |
| T0.6 (leak parser) | `StudentServicePdfErrorTest` — parser lanza IAE con detalle → `ValidationException` con mensaje FIJO, sin el detalle |
| T0.7 (6/14 estados) | `DocumentServiceStatusTest` — `EDIT_REQUEST_REJECTED` (fuera de los 6 viejos) → texto canónico; red: 14/14 sin default viejo, 2 estados → 2 textos |
| T0.8 (fuga cross-programa) | `ReportProgramIsolationTest` — 2 modalidades prog. A + 1 prog. B → solo director A en la respuesta |
| T0.9 (resetPassword) | `AuthResetPasswordTest` — el service NO valida (la validación vive en el DTO + `@Valid`) → validador jakarta real: "123"→violation @Size, null→@NotBlank, "123456"→0 |
| T0.10 (extensiones) | `ModalityDocumentUploadFormatTest` — "pdf,docx" rechaza `tesis.doc` (antes `contains` lo aceptaba); `tesis.docx` sube OK |
| T0.11 (cancelSeminar) | `SeminarCancellationFilterTest` — 3 modalidades (seminario activa / seminario final / otra modalidad): `transition` llamado 1 sola vez con la correcta |
| T0.12 (StudentDocument) | `StudentServiceAcademicHistoryTest` — `saveAcademicHistoryPdf` no lanza, guarda `AcademicHistoryPdf`, `studentDocumentRepository.save` NEVER |

### Arquitectura
- `ArchitectureTest` (ArchUnit, `@AnalyzeClasses("com.SIGMA.USCO")`, 3 reglas):
  - `common_should_not_depend_on_business_modules` — `@ArchIgnore` hasta Fase 6.
  - `services_should_not_instantiate_runtime_exceptions` — `@ArchIgnore` hasta Fase 3. OJO API archunit 1.3.0: NO existe `JavaCall.Predicates.owner` → se compone `target(constructor().and(declaredIn(equivalentTo(RuntimeException.class))))`.
  - `controllers_should_not_inject_repositories` — **ACTIVA**; excluye `*TestController` (el único violador es `AcademicCertificateTestController`, dev-only).
- `@ArchIgnore` (no `@Disabled`): `@Disabled` no aplica a campos y ArchUnit necesita anotar los campos `@ArchTest`.
- `PdfDiagnosticTest` ELIMINADO (falso verde sin asserts).

### Notas
- Cobertura previa conservada: `AcademicHistoryPdfParserServiceTest` (4) y `PdfTitleExtractorServiceTest` (3).
- La suite Spring (2 clases) comparte contexto H2: `@Transactional` en `ModalityControllerSecurityTest` aísla datos; las clases unitarias no tocan BD.
- Smoke runtime de los flujos nuevos (seminarios, upload) pendiente — requiere entorno vivo.

## Fase 2 — Contratos de API (DONE 8/2026)

### Decisión de shape de éxito (vinculante, reemplaza la antigua sección 1.3)
- **Éxito = DTO crudo** (los records `*Response` son el body directamente, sin envolver en `ApiResponse`). Es lo que hacían los 54 endpoints ya tipados y lo que consume el frontend.
- **Convención**: `*Request` (entrada) / `*Response` (salida). DTOs mixtos `*DTO` se renombran en Fase 10.
- **Regla de oro**: cada DTO nuevo produjo **exactamente las mismas claves JSON que el `Map` que reemplaza** (nombres de campo == claves). Cambiar una clave rompe el frontend.
- **Inventario cerrado**: `docs/api-map-inventory.md` (tablas método→DTO→claves por service).

### Estado final (greps de cierre)
- `ResponseEntity<Map<String, Object>>` en controllers = **3 legítimos documentados** con `@Schema`: `reviewSecondaryDocumentExaminer` (ModalityController:264 — merge de consenso de forma variable, `ponytail:` en el endpoint), `healthCheck` y `getAvailableReportsCatalog` (GlobalModalityReportController:338,:353 — catálogo/health dinámicos).
- `ResponseEntity<?>` = **2** (AuthController register/login + AuthService — excepción deliberada, frontend lee `errorMessage.includes("correo")`).
- `Map<String, Object>` en firmas públicas de services = **0** (quedan solo privados y payloads de eventos `ModalityEvent`).
- **7 endpoints `ResponseEntity<Object>`** (uniones de 2-4 DTOs según rama, JSON intacto): `registerFinalDefenseEvaluation`, `getFinalDefenseEvaluationForExaminer`, `getExaminerEvaluationForModality`, `getMyFinalDefenseResult`, `reviewStudentDocumentByExaminer`, `getMyProposalEvaluation`, `getMyFinalDocumentEvaluation`. NO resueltas (YAGNI).

### DTOs clave
- `common/web/OperationResultResponse(success, message, studentModalityId)` + ctor 2-arg — shape compartido de operaciones {success,message[,...]}.
- `report/dto/ReportResponse<T>` (success, message, reportType, data, timestamp) + variantes (`modalities/types` sin reportType, `filtered` con filtersApplied, `by-student` con studentId); `jsonError` unificado; bug colateral: `Map.of("data", null)` → NPE corregido.
- `notifications/dto/`: `NotificationResponse` (lista y detalle mismo shape), `UnreadCountResponse`; GET /notifications → `List<NotificationResponse>`.
- `academic/dto/response/` (nuevo): 8 records (FacultyResponse, FacultyListResponse, ProgramResponse, ProgramDegreeModalityResponse/DataResponse/ListResponse, SuccessMessageResponse, MessageResponse). No fusionar formas parecidas (la clave de dato difiere: faculty/program/data).
- **`CancellationRequestResponse` DUPLICADO ELIMINADO** (Users/dto/response) — único uso era el inline de `StudentController.requestCancellation`, que ahora devuelve directo `Modalities/dto/response/CancellationRequestResponse`.
- Lista completa de DTOs + tablas método→DTO→claves: ver `docs/api-map-inventory.md`.

### T2.12 — Constantes de permisos/roles (`common/security/`)
- `Permissions.java` (56 constantes `PERM_*`, valor = autoridad exacta `"PERM_" + name`) y `Roles.java` (8 constantes, valor = nombre sembrado SIN prefijo; `hasRole('STUDENT')` ≡ autoridad `ROLE_STUDENT`).
- 150 `@PreAuthorize` sustituidos byte-idénticos + `DataInitializer` (53 permisos + 6 roles) + ProjectTitleService (3).
- Forma obligatoria: `@PreAuthorize("hasAuthority('" + Permissions.PERM_X + "')")` — la variante sin comillas del plan produce SpEL inválido en runtime.
- Discrepancias documentadas (NO corregidas, no cambian autorización): roles `ADMIN`/`JURY` en `@PreAuthorize` sin seeder dev (¿creados a mano en prod?); sembrados sin constante (`VIEW_REPORTS`, `VIEW_EXAMINERS`, `APPROVE_FINAL_MODALITY_BY_EXAMINER`, `VIEW_EXAMINER_EVALUATION` + 3 legacy en español); verificados nunca sembrados en dev (`PERM_REVIEW_DOCUMENT_COMMITTEE`, `PERM_VIEW_PROGRAM_DEGREE_MODALITY`, `PERM_UPDATE_PROGRAM_DEGREE_MODALITY`). Auditar contra BD prod.

### Verificación (8/2026)
- `.\mvnw.cmd -q clean compile` → EXIT=0 (1 fix central: constantes `Roles.ROLE_*` vs referencias `Roles.STUDENT` — el agente C dejó nombres con prefijo y referencias sin él; corregidas las 9 referencias en ModalityGroupController/ModalityController/StudentController/ProjectTitleController).
- `.\mvnw.cmd test` → 36 tests, 0 fallos, 2 skipped. Tests adaptados a DTOs: `DocumentWorkflowHistoryTest`, `ModalityDocumentUploadFormatTest`, `SeminarCancellationFilterTest`.
- Smoke runtime con frontend: PENDIENTE (requiere entorno vivo).

### Pendientes (fuera de alcance FASE 2)
- `filePath` (ruta absoluta) expuesto en `UploadDocumentResponse.path` y `StudentDocumentResponse.filePath` — decisión de negocio (id opaco).
- Uniones `ResponseEntity<Object>` (7) y merge de consenso — migrables si el frontend pide esquemas por rama.
- Discrepancias de permisos T2.12 (tabla arriba).
- Renombrar `*DTO` mixtos → `*Response` (Fase 10).

## Fase 3 — Excepciones y validación (DONE 8/2026)

### 0 `new RuntimeException` / `new IllegalArgumentException` en src/main (grep de cierre)
- **`InternalException`** (nueva, `common/exception`): `(String message)` / `(String message, Throwable cause)` → 500 explícito con el mensaje fijo del thrower. Migrados 21 sitios de infraestructura: SeminarModalityService (10 catch-alls), StudentModalityListingService (2), DocumentEditRequestService:1041-1043 (2), AcademicCertificatePdfService (2), ExaminerCertificatePdfService (1), DocumentService:293 (1), EmailService (1), JwtService (1).
- **`NotFoundException`** para dominio "no encontrado" (mensajes byte-idénticos): DocumentEditRequestService (7: 166,228,357,660,803,935,1007), StudentNotificationListener (18), ExaminerNotificationListener (8), ProgramHeadNotificationListener (1). Los listeners re-lanzan sin cambio de flujo (NotFoundException sigue siendo RuntimeException).
- **`IllegalArgumentException` → `ValidationException`** (mensajes byte-idénticos): AcademicHistoryPdfParserService (12, incluye los con variable; el de :110 pierde la causa — ValidationException no tiene ctor con causa, aceptado) y HistoricalReportService:59.

### GlobalExceptionHandler — 19 handlers (8 nuevos + 1 modificado)
Nuevos: MaxUploadSizeExceeded→413 "El archivo excede el tamaño máximo permitido", HttpMessageNotReadable→400 "Solicitud JSON inválida", MissingServletRequestParameter→400 fijo, MissingServletRequestPart→400 fijo, NoResourceFound→404 fijo, ConstraintViolation (jakarta)→400 con data={campo:msg} (shape igual a MethodArgumentNotValid), InternalException→500 (e.getMessage() — mensajes internos fijos). Modificado: IllegalArgumentException→400 FIJO "Solicitud inválida" (antes e.getMessage()).

### Validación de entrada
- `DocumentReviewDTO`: `@Valid` en proposalEvaluation/finalEvaluation. ProposalEvaluationRequest: 7 `@NotNull` alineados al mensaje del service ("Debe proporcionar calificaciones para todos los aspectos de la propuesta de grado"). FinalEvaluationRequest: los 7 `@NotNull` en inglés ELIMINADOS (eran código muerto — nunca se activaban sin `@Valid` — y la rúbrica depende de la modalidad: PRÁCTICA envía solo 5 campos; `validateFinalEvaluationByRubric` del service es el validador único, rubric-aware).
- `AuthRequest`: `@NotBlank`+`@Email` (email) y `@NotBlank`+`@Size(6-60)` (password), mensajes contienen "correo"/"contraseña" (el frontend lee `errorMessage.includes("correo")`); `@Valid` en register/login. Patrón institucional `^u\d+@usco\.edu\.co$` NO se duplicó en el DTO (vive en AuthService.register con su mensaje exacto).
- `RequiredDocumentDTO`: @NotBlank allowedFormat, @NotNull maxFileSizeMB y documentType.
- `ProjectTitleRequest`: @Size(max=500) + truncación manual ELIMINADA en ProjectTitleService (muerta; el endpoint ya tenía @Valid).
- `UpdateUserRequest.roleId` SIN @NotNull (deliberado): el DTO es compartido con changeUserStatus, que no envía roleId (User.jsx:124) — la constraint rompería el cambio de estado.

### `common/validation/FileValidator` (nuevo)
`validateNotEmpty`/`validateExtension`/`validateMime`/`validateSize` — mensajes canónicos = los del flujo de UPLOAD de ModalityDocumentService (contrato del frontend fijado por `ModalityDocumentUploadFormatTest`): "Formato de archivo no permitido", "El archivo supera el tamaño permitido", "Archivo vacío". Migrados `DocumentService.validateFile` (eliminado, 4 llamadas) y la validación inline de `ModalityDocumentService.uploadRequiredDocument` (guards null de allowedFormat/maxFileSizeMB conservados; "El archivo es obligatorio" conservado).

### T3.15 — Distinction
`accept-distinction` (ModalityController) usaba `@RequestBody Map<String,String>` crudo → `DistinctionDecisionRequest(notes)` (Modalities/dto/request), `required=false` preservado. `reject-distinction` ya usaba ReasonRequest (no tocado).

### Verificación (8/2026)
- `.\mvnw.cmd -q clean compile` → EXIT=0.
- `.\mvnw.cmd test` → 36/36 (2 skipped ArchUnit). 1 fix central: mensajes de FileValidator alineados al contrato del upload (el agente C los había unificado al de DocumentService; el test T0.10 fijaba "Formato de archivo no permitido").
- Greps de cierre = 0: `new RuntimeException`, `new IllegalArgumentException`, "Formato no permitido"/"Archivo supera el tamaño permitido" (viejos).
- Smoke 413/400/404: PENDIENTE (requiere entorno vivo; los handlers están en el advice global, sin tests de integración).

## Fase 4 — Seguridad (DONE 8/2026)

### T4.7 — Respuestas de seguridad en JSON (SecurityConfig + JwtAuthenticationFilter + JwtService)
- `SecurityConfig`: exceptionHandling → 401 `ApiResponse.error("No autenticado")` (entry point) y 403 `ApiResponse.error("No tiene permisos para realizar esta acción")` (denied handler). `/redoc.html` quitado de la whitelist.
- Whitelist de Swagger condicionada por propiedad `app.security.swagger-public` (default `true`; false → swagger solo con token).
- `JwtAuthenticationFilter`: además de `validateToken`, verifica `userDetails.isEnabled()` (usuario desactivado → rechazado aunque el token no haya expirado).
- `JwtService`: eliminado el claim `authorities` del token (era ruido; las autoridades se resuelven desde BD vía UserDetailsService).

### T4.8 — RateLimitFilter (nuevo, `security/RateLimitFilter`)
- Aplica SOLO a `/auth/login`, `/auth/register`, `/auth/forgot-password`: 5 req/min por IP, ventana fija de 60s, `ConcurrentHashMap<String,long[]>` + limpieza cada 100 requests (comentario `ponytail:`).
- Exceso → 429 JSON `ApiResponse.error("Demasiados intentos. Intente de nuevo en un minuto.")` (status 429 literal — servlet API no tiene `SC_TOO_MANY_REQUESTS`).
- Configurable: `app.security.rate-limit.enabled` (default `true`). Registrado con `addFilterBefore` del filtro de autenticación.

### T4.5 — AuthService login → 401
Credenciales inválidas / usuario desactivado / formato de correo → 401. **Mecanismo deliberado**: `login` conserva el `try/catch(AuthenticationException)` → `ResponseEntity.status(401).body("Credenciales incorrectas...")` (string crudo). NO se migra a `UnauthorizedException` porque el handler la envolvería en `ApiResponse` (que no tiene campo `errorMessage`) y rompería `errorMessage.includes("correo")` del frontend — excepción documentada de la Fase 2/1.3, "NO tocar". (El resto del servicio sí usa `UnauthorizedException` para reset-password, :157/:160.)

### T4.9 — Cobertura @PreAuthorize (auditoría T4.14: 0 endpoints sin anotación)
- 30 anotaciones nuevas sobre los 32 detectados (1 falso positivo: `getFinalDefenseResult` ya tenía la suya):
  - **ModalityController (17)**: `listRequirements`→`isAuthenticated()` (catálogo); `getAllModalities`/`getModalityById`→`hasRole(STUDENT)` (el service exige StudentProfile; PERM_VIEW_ALL_MODALITIES habría bloqueado estudiantes); `uploadDocument`→STUDENT or PROJECT_DIRECTOR; `startModality`, `validateDocuments`, `getMyAvailableDocuments`, `getMyFinalDefenseResult`, `resubmitCorrectedDocument`, `getCorrectionDeadlineStatus`, `getSeminarDetail`, `getMyProposalEvaluation`, `getMyFinalDocumentEvaluation`, `requestDocumentEdit`, `getMyDocumentEditRequests`, `getMyDocumentEditRequestsByModality`→`hasRole(STUDENT)`.
  - **StudentController (4)**: `getCurrentStudentModality`, `getDocumentHistory`, `requestCancellation`, `uploadCancellationDocument`→`hasRole(STUDENT)`.
  - **NotificationController (4)**, **FacultyController (1)**, **AcademicProgramController (2)**, **TemplateDocumentController (1)**, `listRequirements` (Modality) → `isAuthenticated()`.
  - **DocumentController (1)**: `getDocumentsByModality`→`PERM_VIEW_REQUIRED_DOCUMENT`.
  - **ProjectTitleController (2 corregidos)**: `getProjectTitle`→STUDENT or ADMIN or PROGRAM_HEAD (JURY ELIMINADO — rol no sembrado en dev); `updateProjectTitle`→solo `PERM_UPDATE_MODALITY` (ADMIN ELIMINADO).
- Auditoría T4.14 (script): 0 endpoints método sin `@PreAuthorize` (excluye AuthController público y AcademicCertificateTestController dev-only); 0 `hasRole` con roles inexistentes (7 roles usados, todos en `Roles.java`; ADMIN queda solo en getProjectTitle — el service lo autoriza); 53/56 permisos usados sembrados en DataInitializer; los 3 no sembrados (`REVIEW_DOCUMENT_COMMITTEE`, `VIEW_PROGRAM_DEGREE_MODALITY`, `UPDATE_PROGRAM_DEGREE_MODALITY`) son el hallazgo pre-existente de T2.12 — no los introdujo F4 (un permiso no sembrado nunca se concede → endpoint bloqueado para todos; auditar contra BD prod).

### T4.10 — Revocación al desactivar/cambiar rol (UserAdminService)
- `assignRoleToUser`: si el nuevo rol ≠ EXAMINER → elimina TODAS las ProgramAuthority del usuario (patrón `removeExaminerFromProgram`). Rol EXAMINER conserva sus asignaciones a programas.
- `desactiveUser`: elimina todas las ProgramAuthority.

### T4.11 — SecurityUtils fuera de services (StudentService)
- `SecurityUtils.getCurrentUser()` movido al controller en 5 métodos: `updateStudentProfile(StudentProfileRequest, User)`, `updateStudentProfileFromAcademicHistory(MultipartFile, User)`, `getStudentProfile(User)`, `getMyDocuments(User)`, `viewMyDocument(Long, User)` (todos los callers eran del controller). Import eliminado del service.

### Verificación (8/2026)
- `.\mvnw.cmd -q clean compile` → EXIT=0 (1 fix: `HttpServletResponse.SC_TOO_MANY_REQUESTS` no existe → 429 literal).
- `.\mvnw.cmd test` → 36/36, 0 fallos, 2 skipped. 2 fixes de tests por el cambio de comportamiento/firma:
  - `ModalityControllerSecurityTest` (T0.1): sin token → 401 (antes esperaba 403 — el entry point JSON de F4 es el comportamiento nuevo correcto).
  - `StudentServiceAcademicHistoryTest`/`StudentServicePdfErrorTest` (T0.6/T0.12): firma `updateStudentProfileFromAcademicHistory(file, student)`.
- Auditoría T4.14: 0 endpoints sin @PreAuthorize, 0 roles inexistentes (ver bloque T4.9).
- Smoke runtime (401/403 JSON, 429 rate limit, revocación, swagger-public=false): PENDIENTE — requiere entorno vivo.

### Fuera de alcance (anotado)
- 3 permisos usados y NO sembrados (T2.12) — decisión de negocio/BD prod.
- `hasRole('ADMIN')` en getProjectTitle: el rol ADMIN no está sembrado en dev (T2.12) — el endpoint responde 403 para admins en dev; en prod depende del seed manual.

## Fase 5 — Persistencia y rendimiento (DONE 8/2026)

### T5.1 — LAZY explícito en documents (8 entidades, 16 @ManyToOne)
StudentDocument, StudentDocumentStatusHistory, DocumentEditRequest, ProposalEvaluation, FinalDocumentEvaluation, ExaminerDocumentReview, DocumentEditRequestVote, RequiredDocument. Auditoría previa (~150 sitios): TODA navegación ocurre dentro de tx — **0 @EntityGraph añadidos**. Único hallazgo: `ProjectTitleExtractionService.onStudentDocumentUpdated` (AFTER_COMMIT sin tx) navegaba `documentConfig`/`studentModality` LAZY → `@Transactional(REQUIRES_NEW)` (patrón de los 5 listeners).
- Excepción deliberada al grep `FetchType.EAGER` = 0: `Role.permissions` @ManyToMany EAGER (decisión Fase 2.2, documentada).

### T5.2 — StudentModalityListingService batch
4 listados (ProgramHead/Committee/Director/Examiner): helper privado `loadActiveMembersByModalities` (findByStudentModalityIdInAndStatus 1× por listado) + `getOrDefault(id, List.of())` en `toModalityList`. `mapMembers`: findByUserId → `findAllByUserIdIn`. Los 4 **details** conservan su query single-modality (legítimo, no es loop).

### T5.3 — ModalityCatalogService
- getExaminers/getExaminersForCommittee: `userRepository.findAll()` → `findAllByRoles_Name("EXAMINER")` (ya existía en listeners); `programAuthorityRepository.findAll()` hoisteado fuera del loop en getExaminersForCommittee.
- getProgramCurriculumCommittee → **nuevo** `ProgramAuthorityRepository.findByRole(ProgramRole)`.
- getAllModalities: N+1 → `findByAcademicProgramIdAndActiveTrue(userProgramId)` + Map.
- getProjectDirectors/getProgramHeads: `iterator().next()` → `resolveContextProgram(Set)`: único → ese; múltiples → **primero determinista** (`sorted().findFirst()`, ponytail:). ⚠️ Decisión: para usuarios multi-programa el programa elegido puede cambiar (antes indefinido/arbitrario); el contrato (lista plana sin programa) no permite resolver la ambigüedad.

### T5.4 — AdminCatalogService.getModalities
`findByModalityId` ×2 por modalidad → **nuevos** `ModalityRequirementsRepository.findByModalityIdIn`, `RequiredDocumentRepository.findByModalityIdIn` (1 query c/u). Salida idéntica (orden por PK).

### T5.5 — DocumentEditRequestService.getAllEditRequestsForExaminer
N×(1+M) → 4 queries fijas: **nuevo** `DocumentEditRequestVoteRepository.findByEditRequestIdIn` (ORDER BY v.id, preserva orden) + `@EntityGraph({"studentDocument","requester","studentDocument.documentConfig"})` en `DocumentEditRequestRepository.findByStudentModalityId` + jurados vía `defenseExaminerRepository.findByStudentModalityId` (Map, fallback "Jurado" intacto). `getPendingEditRequestsForExaminer` conserva su N+1 (fuera de alcance, anotado).

### T5.6 — Defensa batch
- `DefenseWorkflowService.getExaminerDefenseCalendar`: `findByStudentModalityIdInAndStatus` 1× + Map.
- `DefenseEvaluationService` getFinalDefenseResult/getMyFinalDefenseResult: `findByDefenseExaminerIdIn` 1× + Map; `getPendingDistinctionProposals`: batch completo (jurados `findByStudentModalityIdIn` con ORDER BY examinerType, criterios, perfiles de líderes). Mismo orden.

### T5.7 — ModalityGroupService.getEligibleStudentsForInvitation
2N existsBy → **nuevos** `StudentModalityRepository.findByLeaderIdIn` + `StudentModalityMemberRepository.findByStudentIdIn` (2 queries) + Set en memoria. Misma lista/orden.

### T5.8 + T5.9 — Notificaciones
- `Notification.triggeredBy` EAGER → **LAZY** (auditoría: solo se escribe, 0 navegaciones). `findByRecipient_IdOrderByCreatedAtDesc` gana `@EntityGraph({"studentModality"})` + param `Pageable` (triggeredBy NO incluido — nadie lo lee, sería JOIN desperdiciado).
- `NotificationController` GET /notifications: `page`/`size` **opcionales** (ausentes → comportamiento previo). **Shape intacto** (List<NotificationResponse>); el frontend queda sin cambio hasta que adopte la paginación.

### T5.10 — report: N+1 restantes batchados
StudentListingReportService (members+perfiles 1 query por sección), DirectorAssignedModalitiesReportService (perfiles antes del loop de directores), DefenseCalendarReportService (`getMembers()` → `loadActiveMembersByModalityIds`, nuevo campo `studentModalityMemberRepository`), HistoricalReportService (members UNA vez, pasados a los 5 buildTopDirector). Restos `getMembers()` = 2 legítimos: ModalityTraceabilityReportService:140 (single-modality con JOIN FETCH de F3) y ModalityTraceabilityPdfGenerator:264 (sobre DTO).

### T5.11 — @Transactional
- readOnly=true: 8× DocumentEditRequestService (verificados sin escrituras), DefenseWorkflowService.getExaminerTypeForModality, DocumentService.getRequiredDocumentsByModalityAndStatus (no tenía tx, añadida), StudentService.getMyDocuments (ya lo tenía — N/A).
- Añadidos: ModalityGroupService.inviteStudentToModality, AuthorityAssignmentService.assignExaminerToPrograms (⚠️ ahora atómico: antes por-item sin rollback — aceptado), UserAdminService.createRole (antes sin tx).

### T5.12 — I/O fuera de tx (3 flujos)
- `DocumentService.uploadCancellationDocument`: outer sin tx (loads+validación+Files.copy) + `persistCancellationDocument` @Transactional.
- `ModalityDocumentService.uploadRequiredDocument` → `persistUpload` @Transactional; check de pertenencia vía **nuevo** `RequiredDocumentRepository.existsByIdAndModalityId` (evita navegar LAZY fuera de tx; Forbidden byte-idéntico; mismo orden de validaciones que el original — verificado con diff).
- `ModalityDocumentService.resubmitCorrectedDocument` → `persistResubmit` @Transactional (ruta desde `studentModality` param, no de la relación LAZY).
- `StudentService.updateStudentProfileFromAcademicHistory` perdió @Transactional (ponytail: Files.copy fuera de tx; saves van en txs cortas propias). ⚠️ Perfil + registro AcademicHistoryPdf ya NO son atómicos entre sí (antes rollback conjunto). Tests T0.6/T0.12 intactos.
- ⚠️ Riesgo pre-existente documentado: validación de pertenencia/estado tras el copy → archivo huérfano posible en disco si falla (filesystem no es rollbackable).

### T5.13 — AcademicProgramService.getActivePrograms
N+1 por facultad → **nuevo** `AcademicProgramRepository.findActiveProgramsWithActiveFaculty` (@Query con ORDER BY p.faculty.id, p.id — preserva el agrupamiento del flatMap previo). `getAllPrograms` conserva findAll() (catálogo completo = contrato).

### T5.14 — OSIV off
`spring.jpa.open-in-view=false` en application.properties, application-dev.properties, application-prod.properties y application-test.properties. Cualquier LazyInitializationException futura = hallazgo nuevo a resolver con fetch explícito (NUNCA revertir OSIV).

### T5.15 — Índices
@Index en 5 entidades (dev los crea vía ddl-auto=update; prod = `docs/migration-fase5-indexes.sql`): `student_modality_members(student_modality_id, status)` + `(student_id)`, `defense_examiners(student_modality_id)`, `defense_evaluation_criteria(defense_examiner_id)`, `student_documents(student_modality_id, document_config_id)`, `notification(recipient_id, created_at)`. Validación con EXPLAIN pendiente (requiere entorno vivo).

### T5.16 — BlackListedToken (PENDIENTE verificación prod)
`token @Column(length=5000, unique=true)`: con utf8mb4 el UNIQUE de 5000 chars excede 3072 bytes (MySQL ERROR 1071). **Verificar en prod** `SHOW INDEX FROM black_listed_token`; si el índice NO existe → migración de datos a hash SHA-256 (64 chars) + cambio en JwtService/BlackListedTokenCleanupScheduler. Sin acceso a prod no se tocó (alto riesgo a ciegas). Instrucciones en el script SQL.

### Verificación (8/2026)
- `.\mvnw.cmd -q clean compile` → EXIT=0. `mvnw test` → 36/36, 0 fallos, 2 skipped (OSIV off incluido).
- 1 fix central: `ModalityDocumentUploadFormatTest` (T0.10) — stub `existsByIdAndModalityId(1L,7L)` → true (el nuevo check por repo no estaba mockeado; orden de validaciones verificado con diff = idéntico al original).
- Greps de cierre: `fetch = FetchType.EAGER` = 1 (Role.permissions, deliberado F2.2); `findAll()` en services = 8 legítimos (catálogos completos = contrato: roles, permisos, programas/facultades/modalidades sin filtro, authorities hoisted 1×); `findByStudentModalityIdAndStatus` = 0 en loops (restos single-modality: 4 details, DocumentWorkflowService 15×, listeners por evento); `userRepository.findAll()` = 0; `getMembers()` en report = 2 legítimos; `findByUserId(` en loops = 0.
- Smoke runtime (100 modalidades < 50 queries, OSIV=false 0 LIE, EXPLAIN índices, T5.16): PENDIENTE — requiere entorno vivo.

### Fuera de alcance (anotado)
- `getPendingEditRequestsForExaminer` (DocumentEditRequestService) conserva su N+1.
- `getStudentModalityDetailForExaminer` (~:917) y `getPendingDefenseProposals` (:159) — single-modality, patrón batch aplicable si se quiere.
- `Role.permissions` EAGER deliberado (F2.2).

## Cierre auditoría Fases 1–5 (8/2026)

Auditoría del estado real de F1–F5 contra el plan (5 explore agents) + cierre de las desviaciones encontradas. Todas las fases verificadas "Correcta".

### Fixes aplicados (código)
- `NotificationService.markAsRead` → `@Transactional` (criterio T5.11a; antes sin anotación, funcional por save autotransaccional).
- `AcademicCertificatePdfService`: N+1 de `findByUserId` por miembro en `addStudentsTable` → `loadProfilesByUserIds` (1× `findAllByUserIdIn`, Map por userId; misma salida, "No registrado" intacto).
- `notifications/dto/OperationResultResponse` ELIMINADO (duplicado vivo de `common/web/OperationResultResponse`; JSON idéntico `{success, message}` — el de common es record de 3 campos con ctor 2-arg). Imports de NotificationService/NotificationController rewireados.

### Drifts de documentación corregidos (AGENTS.md)
- GlobalExceptionHandler: 18 → **19 handlers** (2 líneas).
- StudentNotificationListener: 17 → **18** `NotFoundException`.
- DocumentEditRequestService: 5× → **8×** `@Transactional(readOnly = true)`.
- T4.5 aclarado: `AuthService.login` NO usa `UnauthorizedException` (conserva `ResponseEntity.status(401).body(string crudo)`) porque el handler la envolvería en `ApiResponse` (sin campo `errorMessage`) y rompería `errorMessage.includes("correo")` del frontend — NO tocar.

### Verificación
- `.\mvnw.cmd -q clean compile` → EXIT=0.
- `.\mvnw.cmd test` → 36 tests, 0 fallos, 2 skipped (@ArchIgnore).
- Greps de cierre = 0: `notifications.dto.OperationResultResponse`, `findByUserId(` en `AcademicCertificatePdfService`.

## Fase 6 — Desacoplamiento y god classes (DONE 8/2026)

### T6.1 — ModalityEvent al paquete de dominio
- `notifications/event/ModalityEvent.java` → `Modalities/event/ModalityEvent.java` (package `com.SIGMA.USCO.Modalities.event`). 16 imports actualizados (15 main + 1 test; incluido 1 static import en `ExaminerCertificatePdfService`).
- **Payloads tipados DIFERIDOS a F7** (decisión YAGNI ya documentada en F1.6: los eventos siguen con `Map<String,Object>` + claves `KEY_*`; el plan permite "en esta fase o F7"). No se tocaron publishers ni listeners más allá del import.

### T6.2 — common estable (opción b del plan)
- Nuevo paquete `com.SIGMA.USCO.shared.util`: `TranslationUtils` y `ResourceAccessPolicy` se movieron ahí desde `common/util` (único cambio: package; pueden depender de negocio). 21 + 11 imports de consumidores actualizados.
- `common/` queda SIN imports de negocio (web, exception, validation, security, MimeTypeGuard puros).
- ArchUnit: quitados los 2 `@ArchIgnore` → **3/3 reglas verdes** (`common_should_not_depend_on_business_modules`, `services_should_not_instantiate_runtime_exceptions`, `controllers_should_not_inject_repositories`). Tests: 36/36, 0 skipped.

### T6.3 — SecurityUtils fuera de services: 0 usos
- **18 services migrados** al patrón canónico (controller resuelve `SecurityUtils.getCurrentUser()` y pasa `User` al service): Modalities 10 (CancellationService 8, DocumentEditRequestService 9, DefenseWorkflowService 10, DefenseEvaluationService 8, ModalityCatalogService 5, DocumentWorkflowService 13, ModalityDocumentService 5, ModalityGroupService 5, SeminarModalityService 10, StudentModalityListingService 10 = 83 usos), report 7 (GlobalReportService/Completed/Comparison/DirectorAssigned/DefenseCalendar/StudentListing/Historical = 8 usos; 6 con `String userEmail`, DefenseCalendar con `User`), notifications 1 (NotificationService 4 métodos, helper `getCurrentUser()` borrado).
- Controllers actualizados: ModalityController, ModalityGroupController, StudentController (Users — llama CancellationService/StudentModalityListingService), GlobalModalityReportController (helper `currentUser()`), NotificationController.
- Schedulers verificados limpios (CorrectionDeadlineSchedulerService sin SecurityUtils); listeners no aplican (evento).
- Tests ajustados: SeminarCancellationFilterTest, DocumentWorkflowHistoryTest, ModalityDocumentUploadFormatTest, StudentListingReportServiceTest.

### T6.7 — AdminCatalogService → Modalities
- `Users/service/AdminCatalogService.java` → `Modalities/service/AdminCatalogService.java` (gestiona DegreeModality/Requirements/RequiredDocument). Import de AdminController actualizado; grep 0 de `Users.service.AdminCatalogService`.

### T6.9 — Duplicados estructurales
- (a) SeminarModalityService: los 10 bloques try/catch ya siguen el patrón F3 (IAE→ValidationException, BusinessException→rethrow, Exception→InternalException fijo) — validado, SIN cambios.
- (b) `DefenseWorkflowService.assignExaminers` (:336): extraído helper privado `assignExaminer(studentModality, examinerId, type, label, ...)` (:403) usado por los 3 bloques (byte-idéntico).
- (c) `DefenseEvaluationService`: `getFinalDefenseResult`/`getMyFinalDefenseResult` comparten `buildFinalDefenseResult(studentModality, studentName, studentEmail)` (:528) — byte-idéntico.

### T6.10 — Reglas de dominio duplicadas
- (a) `ModalityServiceUtils.validateNumericRequirements(profile, requirements, failureMessage)` (:152) unifica el loop numérico ×3 (DocumentWorkflowService:158, ModalityGroupService:127,389). **Drift documentado, NO corregido** (decisión de negocio): la whitelist individual (`DocumentWorkflowService:120`) incluye `CORRECTIONS_REJECTED_FINAL`; la grupal (`ModalityGroupService:89`) NO (y omite `GRADED_APPROVED` a pesar del comentario) — comentarios `ponytail:` en ambos sitios.
- (b) `ModalityServiceUtils.translateAcademicDistinction` ELIMINADO → todos los callers (8 DefenseEvaluationService + 3 report) usan `TranslationUtils.translateAcademicDistinction`. **Cambio de string aceptado**: "Mención Meritoria"→"Meritorio", "Aprobado por consenso"→"Aprobado", "Mención Laureada"→"Laureado" (vocabulario canónico F2.6).
- (c) `translateModalityType` ×2 = **falso duplicado DOCUMENTADO**: `DefenseCalendarReportService:727` traduce NOMBRES de modalidad (PROYECTO_DE_GRADO→"Proyecto de Grado"...); `CertificatePdfSupport:279` traduce GROUP/INDIVIDUAL→"Grupal"/"Individual". Vocabularios distintos, NO se fusionan.
- (d) `PdfReport.isDirectorNotRequired` BORRADO → `!ReportUtils.isDirectorRequired(x)` en :207 y :425 (semántica byte-idéntica verificada: mismas 4 subcadenas, null→"requerido").

### Verificación
- `.\mvnw.cmd -q clean compile` → EXIT=0.
- `.\mvnw.cmd test` → 36 tests, 0 fallos, **0 skipped** (3 reglas ArchUnit activas y verdes).
- Greps de cierre = 0: `SecurityUtils` en `*Service.java`; `ModalityServiceUtils.translateAcademicDistinction`; `isDirectorNotRequired`; imports de negocio en `common/`; `Users.service.AdminCatalogService`; `notifications.event.ModalityEvent`; `common.util.TranslationUtils`/`ResourceAccessPolicy`.

### Pendientes (anotado)
- Payloads tipados de eventos (T6.1/F7) — decisión YAGNI vigente.
- Drift whitelist CORRECTIONS_REJECTED_FINAL (T6.10a) — validar con negocio.
- `ReportUtils.getAuthenticatedUserProgram` conserva `SecurityUtils.getCurrentUser()` interno (util estático de report, fuera del criterio `*Service.java`).
- Smoke runtime pendiente (requiere entorno vivo).

## Fase 7 — Notificaciones y eventos (DONE 8/2026)

### Objetivo
Eliminar correos duplicados (T7.1), centralizar textos en `NotificationMessageTemplates` (T7.2/T7.3),
mover la generación de PDF fuera del hilo del request (T7.6) y añadir un outbox con retry (T7.4/T7.13).
Implementado en oleadas: A (textos), B (outbox + Student), D (PDF + limpieza), C (Examiner onFinalDefenseApproved).

### T7.1 — Dedup de correos por evento
Eliminados 2 bloques de estudiantes duplicados en `ExaminerNotificationListener`
(`notifyExaminersAssignment`, `handleDefenseScheduled`): el estudiante ya recibía su correo vía
`StudentNotificationListener`; ahora cada evento envía exactamente 1 correo al estudiante.
Criterio smoke: asignación de jurados → el estudiante recibe UN solo correo.

### T7.2/T7.3 — `NotificationMessageTemplates` canónico
- `NotificationMessageTemplates` (listeners): **+20 constantes de subject** (`public static final String XXX_SUBJECT`),
  **2 métodos de subject dinámicos** (`programHeadModalityApprovedSubject(String)`, `programHeadFinalReviewReadySubject(String)`)
  y **23 métodos de template** (p.ej. `examinerDesignation`, `programHeadModalityStarted`, `directorCancellationApproved`,
  `committeeCancellationRequested`). Los `DateTimeFormatter`/cálculo de fechas quedan en los listeners.
- Migrados a templates: Examiner (7 bloques), ProgramHead (7), Director (6), Committee (3). Mismo String exacto/orden de args.
- **`StudentNotificationListener` conserva sus ~22 subjects inline** (decisión F4 Trabajo 5, YAGNI: strings únicos de 1 uso;
  los 4 listeners ya migrados cubren el dedup real). No volver a moverlos.
- **38 `orElseThrow()` planos → `NotFoundException`** (el plan decía 17; desactualizado): Student 18, ProgramHead/Director/Committee/Examiner 19.
  Grep de cierre: `new RuntimeException` en notificaciones = 0; `.orElseThrow` restantes = todos `NotFoundException`.

### T7.4 — Outbox: `Notification` + campos de entrega
- `Notification` +4 columnas: `deliveryAttempts` (int, default 0), `lastAttemptAt` (LocalDateTime), `attachmentPath` (length 1000), `attachmentName`.
- `NotificationRepository.findByEmailSentFalseAndDeliveryAttemptsLessThan(int)` (outbox query).
- **`NotificationRetryScheduler`** (nuevo, `notifications/service`): `@Scheduled(fixedDelay=300000)` re-envía notificaciones fallidas
  (`emailSent=false` y `deliveryAttempts<3`); si `attachmentPath` no nulo reintenta con adjunto vía `dispatcher.retryDispatch`.
  `markCertificatesSent` (por modalidad, `findByStudentModalityIdAndStatus(mId, GENERATED)`) marca SENT los certificados del retry exitoso.
- **`shouldSendEmail=false` → `emailSent=true`** (jefes/comités sin SMTP quedan fuera del retry). Regla estable comentada en el código.
- El set `ConcurrentHashMap` dedup se conserva como guard documentado (`// ponytail:`): la BD (emailSent+attempts) es la fuente de verdad.
- Límite de retry documentado: si el PDF falló en el 1er intento (`attachmentPath` null), el retry NO puede regenerar → alerta en log (no bloquea).

### T7.5 — `CertificateStatus.SENT` tras éxito de envío
`NotificationDispatcherService.dispatchWithAttachment(Notification, Supplier<GeneratedAttachment>, Consumer<Long>)`
recibe un `Consumer<Long>` `onSuccess` que el listener usa para `updateCertificateStatus(certId, SENT)` — se marca SOLO tras éxito del email.
El retry marca SENT por modalidad (`markCertificatesSent`). Eliminadas las llamadas síncronas a SENT en los listeners.

### T7.6 — PDF fuera del hilo del request (async, LAZY, OSIV off)
- **`dispatchWithAttachment`** genera el adjunto dentro del executor async (`@Async("notificationTaskExecutor")`) vía `Supplier<GeneratedAttachment>`:
  - `handleModalityFinalApprovedByCommittee` (Student): **supplier memoizado con `synchronized`** — el acta se genera 1 vez para N destinatarios.
  - `handleDefenseResult` (Student, líder) y `onFinalDefenseApproved` (Examiner, por jurado): supplier por destinatario que re-carga
    modality/`DefenseExaminer` por id dentro del async (LAZY; OSIV off). `try/catch(IOException → RuntimeException)` con mensaje fijo
    (checked exception de `generateCertificate` no es propagable desde `Supplier`).
- `GeneratedAttachment(Path path, String name, Long certificateId)` (record anidado en `NotificationDispatcherService`).
- `retryDispatch(Notification, Runnable)` síncrono (solo scheduler).

### T7.7 — `NotificationType` limpieza
Eliminados 9 valores muertos (0 publishers): MODALITY_CANCELLED, DOCUMENT_APPROVED, DOCUMENT_REJECTED, DIRECTOR_CHANGED,
FINAL_FAILED, MODALITY_INVITATION_CANCELLED, MODALITY_MEMBER_LEFT, MODALITY_MEMBER_JOINED, MODALITY_GROUP_READY.
`docs/migration-fase7-notifications.sql` (nuevo) mapea los tipos viejos → vivos (ver cabecera del SQL). **Ejecutar ANTES del deploy**
para no dejar notificaciones históricas con tipo inexistente.

### T7.9 — `AcademicCertificateTestController` → `ResourceAccessPolicy`
- `ResourceAccessPolicy` (shared/util) +`requireProgramAuthorityIn(User, Long, List<ProgramRole>, String)` y `requireProjectDirector(StudentModality, User, String)`.
- `AcademicCertificateTestController` compone con `tryRequire` (líder/miembro/jurado vía query, director, PERM_VIEW_REPORT, autoridad roleIn).
  Eliminada la navegación LAZY de `defenseExaminers`. `@Profile("dev")` intacto (sigue aislado de prod).

### T7.12 — `translateDistinction` → `TranslationUtils.translateAcademicDistinction`
Eliminado `CertificatePdfSupport.translateDistinction` (3 callers); los PDF usan el vocabulario canónico (T6.10b):
"Meritorio", "Laureado", "Aprobado"... Documentado con `// ponytail:`. Texto del PDF cambia por decisión de negocio (F6).

### T7.10 — Templates fuera de Java: DIFERIDO (opcional)
NO se movieron los textos a templates externos (Thymeleaf/.ftl). Los mensajes viven en `NotificationMessageTemplates`
y en los listeners. Solo tiene sentido si el negocio pide editar textos sin redeploy; reintroducirlo requiere
template engine + cambio transversal en los 5 listeners y el dispatcher.

### Verificación (8/2026)
- `.\mvnw.cmd -q clean compile` → EXIT=0.
- `.\mvnw.cmd test` → **39 tests, 0 fallos, 0 skipped** (36 base + 3 nuevos `NotificationRetrySchedulerTest`).
  WARN pre-existente en test: `idx_notification_recipient_created` no se crea en H2 (columna `recipient_id`), no afecta a MySQL prod ni a los tests.
- Greps de cierre: `String subject = "` en ProgramHead/Committee/Director/Examiner = 0 (solo Student, deliberado); `translateDistinction` = 0;
  `dispatchWithAttachment(Path)` (firma vieja) = 0; `new RuntimeException` en notificaciones = 0.
- Smoke runtime pendiente (requiere entorno vivo): asignación de jurados → 1 correo al estudiante; 3 jurados → 4 PDFs en executor async;
  SMTP apagado → retry outbox.

### Fuera de alcance (anotado)
- Subjects inline de `StudentNotificationListener` conservados (YAGNI, F4 Trabajo 5).
- `NotificationController`/`NotificationService` con `Map` crudo en algunos endpoints — no tocado en F7 (pendiente de contrato API, Fase 2/10).

## Fase 10 — Optimización y limpieza (DONE 8/2026)

### Objetivo
Cerrar deuda de convenciones, dead code y duplicaciones. Implementado en 5 oleadas (auditoría previa + agentes por propiedad de archivos).

### T10.1 — Dead code en repositorios (eliminados, verificado 0 callers por grep)
- `UserRepository.findByStatus`; `ProgramAuthorityRepository.existsByAcademicProgram_IdAndRole`.
- `AcademicHistoryPdfRepository`: 5 muertos (queda solo `save`, usado por `StudentService`).
- `StudentProfileRepository`: `existsByUserId`, `findByAcademicProgramId`. ⚠️ `findByAcademicProgramId` se **conservó** (tiene 2 callers: `StudentModalityListingService:1050`, `ModalityGroupService:181`).
- `ProgramDegreeModalityRepository`: 6 muertos (`findByAcademicProgramId`, `findByDegreeModalityId`, `findByActiveTrue`, `findByAcademicProgramFacultyId`, `findByActive`).
- `StudentModalityRepository`: 22 líneas comentadas + 10 muertos (`findByStatus`, `existsByStudentIdAndStatusIn`, `existsByLeaderIdAndStatusIn`, `findByModalityType`, `findGroupModalitiesByStatus`, `isLeaderOfModality`, `findByAcademicProgramAndType`, `findByIdWithInvitations`, `findGroupModalitiesByProgramWithMembers`, `findByLeaderWithStatusAndName`).
- `DegreeModalityRepository.existsByNameIgnoreCase` (vivo: `existsByNameIgnoreCaseAndFacultyId`); `findByStatus` se **conservó** (callers `AdminCatalogService:35`, `ModalityCatalogService:249`).
- `StudentDocumentRepository.findByStudentModalityIdAndDocumentConfig_DocumentName`; `ExaminerDocumentReviewRepository.findAllByStudentDocumentId`; `DocumentEditRequestVoteRepository.findByEditRequestIdAndIsTiebreakerVote`.
- `AcademicHistoryPdfParserService.matchDouble` (privado muerto).

### T10.2/T10.3 — Inyecciones y loop muertos
- `StudentModalityListingService`: campo `academicHistoryPdfRepository` eliminado. `NotificationService`/`DefenseCalendarReportService` ya limpios (F7/F3).
- `CancellationService`: loop muerto que iteraba `activeMembers` para un `DirectorChangedEvent` que **no existe** (solo comentario) — eliminado con su query local. El `ModalityEvent(DIRECTOR_ASSIGNED)` real intacto.

### T10.4 — Mapper único `RequiredDocumentDTO.from(RequiredDocument)`
- Nuevo método estático en el DTO (setea 9 campos + `modalityId`). Reemplaza los 4 builders inline (DocumentService×2, AdminCatalogService, ModalityCatalogService).
- **Drift corregido**: `getRequiredDocumentsByModalityAndStatus` (DocumentService) ahora incluye `modalityId` (antes no lo seteaba).
- ⚠️ **Cambio de contrato no previsto en plan**: `getModalityDetails` (ModalityCatalogService) ahora puebla `active`/`requiresProposalEvaluation` reales (antes quedaban `false`). Validar con frontend.

### T10.6 — Duplicaciones menores
- `calculateMedian`/`calculateMedianDouble` → 1 genérico `<T extends Number> Double calculateMedian(List<T>)`.
- `TranslationUtils.sanitizeFileName(String)` (regex `[^a-zA-Z0-9._-]`) + overload `sanitizeFileName(String, String regex)`. Los sitios de nombre de archivo usan el permisivo; los de carpeta de modalidad (`[^a-zA-Z0-9]`) conservan su regex vía overload (comportamiento preservado). `GlobalModalityReportController:576` fuera de alcance (report controller).
- `UserAdminService`: constante `EMPTY_PERMISSIONS` (reemplaza el `Set.of()` duplicado en createRole/updateRole).
- `UserRepository`: +3 default methods (`findAllExaminers`, `findAllProgramHeads`, `findAllProgramCurriculumCommittee`) → 12 usos reemplazados (ModalityCatalog ×2, Committee ×3, ProgramHead ×7).
- `ExaminerNotificationListener`: helper privado `activeMembers(StudentModality)` (patrón del Student listener) → 2 queries inline eliminadas.
- existsBy*IgnoreCase (academic): **NO extraído helper** (decisión documentada) — los mensajes difieren por entidad/campo; no hay dedup real de mensajes.

### T10.7 — Traducciones: NO fusionar (falsos duplicados, decisión)
La auditoría determinó que casi todos los `translate*` restantes son **falsos duplicados** (vocabularios, claves y firmas distintos): `translateProposedDistinction` (femenino "Meritoria"), `describeModalityStatus`/`describeDocumentStatus` (frases largas vs etiquetas cortas canónicas), `CompletedModalitiesPdfGenerator.translateDistinction` (String-keyed `"MERITORIOUS"`), `translateTimelineStatus`/`translatePerformance`, `translateTrend`/`getTrendColor`/`getTrendIcon` (claves `IMPROVING` vs `GROWING`). Unificar cambiaría la salida → se dejan como están. Los 8 canónicos siguen en `TranslationUtils`.

### T10.8 — Naming
- **(a) Paquetes renombrados** (46 archivos, `git mv` en 2 pasos por case): `Modalities/Controller→controller` (2), `Modalities/Entity→entity` (25, incl. `entity.enums`), `Modalities/Repository→repository` (11), `Users/Entity→entity` (8, incl. `enums`). Imports actualizados en main+test. **Fix colateral**: la regla ArchUnit `controllers_should_not_inject_repositories` usa `..controller..` (minúscula); `ModalityControllerSecurityTest` (test en `..controller..`) ahora casaba y violaba → se añadió `.haveSimpleNameNotEndingWith("Test")` a la regla.
- **(b)** `assignAuthorityProgram.java` → `AssignAuthorityProgramRequest`.
- **(c)** `ScheduleDefenseDTO` → `ScheduleDefenseRequest` (8 usos: 3 endpoints + DefenseWorkflowService).
- **(d)** Typo `FgetSeminarDetailForProgramHead` → `getSeminarDetailForProgramHead` (SeminarModalityService, ModalityController, SeminarDetailResponse Javadoc).
- La convención *DTO→*Request/*Response genérica (T10.8c original) NO se aplicó masivamente (los `@Data` en `dto/` son DTOs válidos; no son magic). Documentado.

### T10.9 — @Data eliminado de 32 entidades
`@Getter @Setter @EqualsAndHashCode(onlyExplicitlyIncluded=true) @ToString(onlyExplicitlyIncluded=true)` con `@Include` en `id` y campos escalares. Colecciones excluidas del toString (DegreeModality, Seminar, StudentModality, Role, User, AcademicProgram, Faculty). Sensibles excluidos: `User.password`, `BlackListedToken.token`, `PasswordResetToken.token`. `Role` conservó su equals/hashCode manual id-based. Grep `@Data` en @Entity = 0 (los ~90 `@Data` restantes son DTOs).

### T10.10 — Magic strings
- **(a) Roles**: 28 literales de rol → `Roles.ROLE_*` (EXAMINER 13, PROGRAM_HEAD 4, PROJECT_DIRECTOR 5, PROGRAM_CURRICULUM_COMMITTEE 4, STUDENT 2). SpEL `@PreAuthorize`/`hasRole` intactos (no son magic de código).
- **(b) Emprendimiento**: constante `ModalityServiceUtils.ENTREPRENEURSHIP_MODALITY_NAME` → 3 sitios de código reemplazados (otras apariciones eran comentarios/prosa).
- **(c)** `AuthService.logout`: `ZoneId.systemDefault()` → `ZoneId.of("America/Bogota")` (decisión de negocio: institución colombiana).
- **(d)** `AuthService`: constante `BLACKLIST_EXPIRATION = Duration.ofHours(5)` con `// ponytail:` documentando la relación con `jwt.expiration=18000000ms` de `JwtService`.

### T10.11 — Fechas/consistencia
- **(a)** `AcademicHistoryPdf.uploadDate` ELIMINADO → canónico `createdAt` (via @PrePersist). El setter inline en `StudentService:520` eliminado. Los `getUploadDate/setUploadDate` restantes son de `StudentDocument` (entidad distinta, intacta).
- **(c)** Mismatch @Size vs @Column: `DocumentEditRequestDTO.reason` (`@Size max=2000`) vs `DocumentEditRequest.reason` (`@Column length=200000`) → DTO subido a `max=200000` (texto libre de justificación; alinear al menor rompería flujos). `// ponytail:`.
- **(b)** `extractedProgramName` en `AcademicHistoryPdf`: DIFERIDO (decisión de negocio de auditoría).
- **(d)** Clock: DIFERIDO (depende de Fase 8, no implementada).
- **(e)** `collect(Collectors.toList())` → `.toList()`: **120 aplicados**, 0 dejados por mutabilidad (verificado). Imports `Collectors` huérfanos eliminados.

### T10.12 — Convención de controladores (verificado, sin cambios)
`AdminController`: 0 `ResponseEntity<Map>`, 33 métodos todos con DTOs crudos (conforme F2). `FacultyController.getFacultyDetail` ya devuelve DTO crudo (F2). No se cambió nada.

### T10.13 — Historias frágiles
- **`CancellationService`**: `history.get(size-2)` → restauración determinista (patrón del comité): historial ASC, filtrar estados de cancelación (`CANCELLATION_REQUESTED`, `CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR`, `CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR`, `CANCELLATION_REJECTED`), DESC, primero no-cancelación; fallback `MODALITY_SELECTED`.
- **`StudentModalityListingService`**: `authorities.get(0)` → lógica determinista inline (único → ese; múltiples → preferir PROGRAM_HEAD si exactamente 1; si no → ForbiddenException). No usó `ReportUtils.getAuthenticatedUserProgram` (firma toma repo+SecurityUtils, no una lista ya cargada).
- **`.modalityType(null)`** → `studentModality.getModalityType() != null ? .name() : null` (mismo origen que los otros builders).

### T10.14 — `localizeObservations` (TranslationUtils)
- Reemplazo-por-orden → **regex alternado con tokens ordenados por longitud DESCENDENTE + `Pattern.quote` + `Matcher.replaceAll`** (el token más largo gana → elimina el bug de prefijos `AGREED_APPROVED` vs `..._2` y la dependencia del orden del Map).
- Bloque de distinción: `catch(Exception)` → `catch(IllegalArgumentException)` (inner y outer); sin try anidado redundante. Salida observable idéntica.

### T10.15 — Logging estructurado
NO se aplicó masivamente (los listener errors ya incluyen studentModalityId/eventType). Los catch-all de services siguen con mensaje fijo + causa (F3). Diferido — bajo valor sin un deploy que pida JSON.

### Verificación (8/2026)
- `.\mvnw.cmd -q clean compile` → EXIT=0. `.\mvnw.cmd test` → **39 tests, 0 fallos, 0 skipped**.
- Greps de cierre: `@Data` en @Entity = 0; `SecurityUtils` en services = 0; `Collectors.toList()` = 0; `Modalities.(Controller|Entity|Repository)`/`Users.Entity` = 0; `get(0)`/`iterator().next()` en Modalities = 0; `assignAuthorityProgram`/`ScheduleDefenseDTO`/`FgetSeminarDetailForProgramHead` = 0.
- Greps con excepciones documentadas (no del alcance/legítimas): `new RuntimeException` = 3 (suppliers async de F7, fijos); `ResponseEntity<Map>` = 3 (healthCheck, catálogo, consenso — F2); `findAll()` = 8 (catálogos completos = contrato, F5).
- God classes >1000 líneas: 5 generadores PDF (renderers puros, no divididos por decisión F5) + `DocumentWorkflowService`/`StudentModalityListingService` (splits de F4). No son nuevos.

### Fuera de alcance / diferido (anotado)
- `extractedProgramName` (T10.11b) y Clock (T10.11d, F8) — decisiones de negocio pendientes.
- Traducciones T10.7 no fusionadas (falsos duplicados, decisión de negocio del vocabulario).
- `GlobalModalityReportController:576` sanitización inline no tocada (report controller).
- Cambio de contrato en `getModalityDetails` (T10.4, active/requiresProposalEvaluation) — validar con frontend.
- Smoke runtime pendiente (requiere entorno vivo).

## Auditoría post-implementación + smoke (DONE 8/2026)

Auditoría final contra el plan (5 explore agents) + arranque real contra MySQL. Todo CUMPLIDO; se aplicaron fixes menores y se corrigió el drift de esquema en la BD.

### Fixes de código aplicados
- **Riesgo LAZY (dev)**: `AcademicCertificateTestController.generateTestCertificate` navegaba `getDefenseExaminers()` (LAZY OneToMany) en `CertificatePdfSupport.isCompleteModality` sin tx (OSIV off) → `@Transactional(readOnly = true)` en el método. `academicProgram`/`projectDirector` son `@ManyToOne` EAGER (sin riesgo). Dev-only.
- **Residuo de refactor**: `DocumentService.validateFile` (wrapper 1:1 que delegaba a `FileValidator`, 1 caller) → inlined en `uploadCancellationDocument`.

### Drift de esquema BD (arranque con `ddl-auto=validate` fallaba) — corregido con migración
`docs/migration-fase10-blacklisted-expires.sql` (aplicada manualmente en BD SIGMABD):
- `black_listed_token` + `expires_at datetime(6) NOT NULL` (tabla sin datos; requerida por `BlackListedTokenCleanupScheduler.deleteByExpiresAtBefore`).
- `notification` + columnas de la outbox F7: `delivery_attempts int NOT NULL DEFAULT 0` (13.840 filas existentes), `last_attempt_at`, `attachment_path`, `attachment_name` (NULL).

### Smoke runtime (perfil prod, MySQL 3306 = MariaDB de XAMPP)
- App arranca limpio: "Tomcat started on port 8099" (esquema valida OK tras la migración).
- `POST /auth/login` con credenciales inválidas → **401** (flujo completo: BD + JWT + RateLimitFilter).
- Swagger deshabilitado en prod (intencional, `springdoc.*.enabled=false`). `/v3/api-docs` no está en prod.
- **OJO entorno**: el MariaDB de XAMPP (3306) es un proceso manual (`C:\xampp\mysql\bin\mysqld.exe --defaults-file=C:\xampp\mysql\bin\my.ini`); si se detiene, la app no arranca (conexión rechazada). MySQL93 service (3307) es OTRO server, no lo usa el proyecto.

### Verificación
- `.\mvnw.cmd -q clean compile` → EXIT=0. `.\mvnw.cmd test` → **39/39, 0 fallos, 0 skipped, BUILD SUCCESS**.

## Fase 11 — Cierre de cobertura E2E (fixes de negocio, DONE 8/2026)

- **1.1 — Reset de evaluación de defensa (bug 8)**: `DefenseEvaluationService.resetDefenseEvaluation(studentModalityId, committeeMember)` + `POST /modalities/{id}/defense-evaluation/reset` en `ModalityController` (`@PreAuthorize(PERM_APPROVE_MODALITY_BY_COMMITTEE)`). Borra criterios, limpia `academicDistinction` si `DISAGREEMENT_PENDING_TIEBREAKER`, transiciona a `DEFENSE_SCHEDULED` (si hay fecha) o `EXAMINERS_ASSIGNED`. Guards: solo rol `PROGRAM_CURRICULUM_COMMITTEE` del programa; 400 si no hay evaluación pendiente. Desbloquea el deadlock cuando un jurado votó antes de tiempo / quedó atascado en estados UNDER_EVALUATION.
- **1.2 — Drift whitelist grupal (T6.10a, decisión de negocio)**: `ModalityGroupService.startStudentModalityGroup` ahora incluye `CORRECTIONS_REJECTED_FINAL` en `finalizedStatuses` (alineado a la variante individual). `GRADED_APPROVED` se omite deliberadamente.
- **1.3 — Permisos informativos STUDENT (A-48bis, decisión de negocio)**: 5 constantes nuevas en `Permissions.java` (`PERM_START_MODALITY`, `PERM_UPLOAD_DOCUMENT`, `PERM_REQUEST_CANCELLATION`, `PERM_REQUEST_EDIT`, `PERM_VIEW_RESULT`), sembradas en `DataInitializer` (dev) para `ROLE_STUDENT`. **NO usados en ningún `@PreAuthorize`** (la autorización real es por `ROLE_STUDENT`). Para BD poblada (prod, sin DataInitializer): ejecutar `docs/migration-fase11-student-permissions.sql` (idempotente).
- **1.4 — Facultad con programas activos (A-07, decisión de negocio)**: `FacultyService.deactivateFaculty` lanza `ValidationException` si la facultad tiene algún `AcademicProgram.active == true`.
- Tests nuevos: `DefenseEvaluationResetTest` (6), `ModalityGroupStartWhitelistTest` (2), `FacultyDeactivateTest` (3). `.\mvnw.cmd test` → **57 tests, 0 fallos, 0 skipped, BUILD SUCCESS**.
