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

**Active profile**: `dev` (configured in `application.properties`)

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
- **StudentNotificationListener.java**: Fully converted (976 lines, 26+ handlers). All `NotificationRepository.save()` + `dispatcher.dispatch()` pairs replaced with factory calls. 3 hand-rolled `Notification.builder()` cases remain (1 with `invitationId`, 2 with attachment) — these use `buildAndSave()`/`saveAndDispatch()` + inline `dispatchWithAttachment()`

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
- **1.3 (controllers return ApiResponse, not raw types)**: COMPLIANT. Sole exception `AuthService` (register/login) — deliberate, frontend reads `errorMessage.includes("correo")`. Don't touch.
- **1.4 (@Valid coverage)**: DONE. 63/63 `@RequestBody` counted; 21 without `@Valid` all intentional (AuthController, ScheduleDefense, report filters, raw Map strings).

### Exception hierarchy
`BusinessException` (root) → `ValidationException`, `ConflictException`, `NotFoundException`, `ForbiddenException`, `UnauthorizedException`.
`GlobalExceptionHandler`: NotFound→404, Conflict→409, Forbidden→403, Unauthorized→401, AccessDenied→403, IllegalArgumentException→400, BusinessException→400, MethodArgumentNotValid→400 (data={campo:msg}), DateTimeParse→400, DataIntegrityViolation→409, generic Exception→500 (no message leak).

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
- Typed event payloads (`Map<String,Object>` → strong optional fields, `studentModalityId = 0L` magic) — done per-publisher in Phase 2.

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
`isCompleteModality` duplicado (Student:1460 vs TestController:89), textos inline de los otros 4 listeners, `NotificationController`/`NotificationService` (Maps crudos), `ModalityServiceUtils.translateExaminerType` (duplicado de `TranslationUtils`).

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
- Quedan 3 sitios `setStatus` SIN historia (intencional, sin historia original): `SeminarModalityService:639` (loop cancelación), `DefenseModalityService:678` (UNDER_EVALUATION_PRIMARY_EXAMINERS), `DocumentEditRequestService:559` (ver bug abajo).
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

### Bug pre-existente detectado (NO corregido — fuera de alcance, se preservó comportamiento)
`DocumentEditRequestService:559`: al RECHAZAR una solicitud de edición, `setStatus(PROPOSAL_APPROVED)` — pero el comentario (:556) y la historia registrada (:580-582, `EXAMINERS_ASSIGNED`) dicen que la modalidad debe VOLVER a `EXAMINERS_ASSIGNED`. Inconsistencia entre código/comentario/historial. Fix sugerido de 1 línea (cambiar `PROPOSAL_APPROVED` → `EXAMINERS_ASSIGNED`) — validar con el negocio antes.

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
- Controller: `catch (BusinessException e) { throw e; }` insertado antes de CADA `catch (Exception e)` que devolvía 500 con `e.getMessage()` (26 bloques: :110, :138, :170, :195, :223, :256, :281, :317, :347, :435, :474, :499, :532, :557, :591, :622, :655, :683, :716, :744, :995, :1038 + 4 de trazabilidad :805, :834, :872, :912). Anomalía: en los 4 endpoints de trazabilidad el rethrow va ANTES del `catch (RuntimeException e)` (no entre RuntimeException y Exception) para que no quede código muerto — única posición funcional. Los `catch (IllegalArgumentException e)` (400) y `catch (DocumentException | IOException e)` no se tocaron.

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

### Trabajo 5 (coord.) — StudentNotificationListener (976 líneas, NO era god class)
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
