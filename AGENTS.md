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
- **NotificationFactory.java**: Created with 5 methods — `buildAndDispatch` (2 overloads), `buildAndSave`, `saveAndDispatch`, `buildAndDispatchWithAttachment` (2 overloads)
- **CommitteeNotificationListener.java**: Fully converted, fields replaced with `NotificationFactory`
- **ProgramHeadNotificationListener.java**: Fully converted, fields replaced with `NotificationFactory`
- **DirectorNotificationListener.java**: Fully converted, fields replaced with `NotificationFactory`
- **ExaminerNotificationListener.java**: Fully converted, `NotificationRepository` field removed
- **StudentNotificationListener.java**: Fully converted (~1780 lines, 26+ handlers). All `NotificationRepository.save()` + `dispatcher.dispatch()` pairs replaced with factory calls. 3 hand-rolled `Notification.builder()` cases remain (1 with `invitationId`, 1 with attachment in `handleDefenseResult`, 1 with attachment in `handleModalityFinalApprovedByCommittee`) — these use `buildAndSave()`/`saveAndDispatch()` + inline `dispatchWithAttachment()`

### Hand-rolled builders still in StudentNotificationListener
- `handleModalityInvitationSent` (line 1028): uses `.invitationId()` — kept hand-rolled + `saveAndDispatch()`
- `handleDefenseResult` leader path (line 449): uses `buildAndSave()` + inline `dispatchWithAttachment()`
- `handleModalityFinalApprovedByCommittee` (line 1244): uses `buildAndSave()` + inline `dispatchWithAttachment()`

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

### Deferred to Phase 2.4
- `SeminarModalityService` swallow try/catch around `publishEvent` — REMOVED (8/2026); events now rely on the multicaster error handler.
- `DefenseModalityService` direct `examinerNotificationListener.notifyExaminersAssignment(...)` pre-commit call — REPLACED (8/2026) with `publishEvent(EXAMINER_ASSIGNED)`; `ExaminerNotificationListener.handleEvent` gained `case EXAMINER_ASSIGNED` and `notifyExaminersAssignment` lost `@Async` (runs in the REQUIRES_NEW handler, after commit).
- Typed event payloads (`Map<String,Object>` → strong optional fields, `studentModalityId = 0L` magic) — done per-publisher in Phase 2.
