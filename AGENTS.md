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
