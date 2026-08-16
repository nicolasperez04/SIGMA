# 🎓 SIGMA — Sistema Interno de Gestión de Modalidades Académicas

Backend del sistema de gestión de modalidades de grado para la **Facultad de Ingeniería de la Universidad Surcolombiana (USCO)**.

Automatiza el ciclo de vida completo de las modalidades de grado (Proyecto de Grado, Práctica Profesional, etc.): desde la selección de modalidad y formación de grupos, pasando por la carga y evaluación de documentos, asignación de jurados, sustentación, calificación con distinciones, hasta la expedición de certificados.

---

## ✨ Funcionalidades principales

### 🔐 Autenticación y autorización
- Registro e inicio de sesión con JWT (access token)
- Roles: `SUPERADMIN`, `PROGRAM_HEAD`, `PROGRAM_CURRICULUM_COMMITTEE`, `STUDENT`, `PROJECT_DIRECTOR`, `EXAMINER`
- Permisos granulares (base de datos) con ~20+ permisos evaluados vía `@PreAuthorize`
- Autoridades por programa académico (`ProgramAuthority`)
- Recuperación de contraseña por email
- Bloqueo de tokens (logout)

### 🏫 Gestión académica
- CRUD de **Facultades**
- CRUD de **Programas Académicos** (ej. Ingeniería de Software, Electrónica, etc.)
- Configuración de **modalidades disponibles por programa** con requisitos de créditos y toggle de proceso de sustentación

### 📋 Modalidades de grado
- Flujo completo de selección de modalidad (individual o grupal)
- **Formación de grupos**: líder invita miembros, aceptan/rechazan
- Asignación de **director de proyecto**
- **Estados del proceso** con 40+ estados que cubren revisión de jefatura, comité de currículo, jurados, sustentación y cierre
- Historial de cambios de estado (auditoría)

### 📄 Gestión documental
- Carga y reemplazo de documentos por el estudiante
- Documentos obligatorios (`MANDATORY`), secundarios (`SECONDARY`) y de cancelación (`CANCELLATION`)
- **Flujo de revisión**: jefatura → comité de currículo → jurados
- Evaluación de **propuestas** con rúbrica de 7 aspectos
- Evaluación de **documentos finales** con rúbrica de hasta 11 aspectos
- Revisión por jurados con votación individual y **desempate**
- **Solicitudes de edición** de documentos aprobados con votación entre jurados
- Historial de cambios de estado por documento

### 🎤 Sustentación y evaluación
- Programación de sustentación (fecha, lugar)
- Asignación de **2 jurados principales** + 1 **jurado de desempate**
- Evaluación con rúbrica de 5 criterios estándar (dominio, síntesis, argumentación, innovación, presentación)
- Rúbrica adicional para **emprendimiento** (5 criterios)
- Cálculo de nota definitiva (0.0–5.0) según Acuerdo 071/2023
- Propuesta de **menciones**: Meritoria, Laureada
- Revisión de distinción por comité de currículo

### 📜 Certificados
- **Certificado de grado** (uno por modalidad, número único, hash SHA-256)
- **Certificado de participación** para jurados
- Generación en PDF con iTextPDF

### 🔔 Notificaciones
- Sistema **event-driven** con 22+ tipos de eventos
- Notificaciones en aplicación + email vía SMTP (Gmail)
- Plantillas configurables por tipo de notificación y rol destinatario

### 📊 Reportes
- 20 tipos de reportes institucionales (RF-45 a RF-61)
- Exportación a **PDF**, **Excel**, **CSV** y **JSON**
- Reportes de modalidades activas, completadas, por director, por estado, cronograma de sustentaciones, estadísticas por programa/semestre, etc.

### 🎓 Seminarios
- Gestión de seminarios (nombre, costo, fechas, capacidad)
- Inscripción de estudiantes

---

## 🛠️ Tecnologías

| Tecnología | Versión |
|---|---|
| **Java** | 21 (con preview features) |
| **Spring Boot** | 3.5.8 |
| **Spring Security** | 6.x (JWT, OAuth2 Resource Server) |
| **Spring Data JPA** | — |
| **Spring Mail** | — |
| **MySQL** | 8.x |
| **Hibernate** | 6.x |
| **JJWT** | 0.12.6 |
| **iTextPDF** | 5.5.13.3 |
| **Apache PDFBox** | 2.0.30 |
| **Apache Commons IO** | 2.16.1 |
| **SpringDoc OpenAPI** | 2.5.0 |
| **Lombok** | — |
| **Maven** | 3.9+ |

---

## 📦 Modelo de dominio

### Usuarios y seguridad
```
User ──┬── Role ── Permission
       ├── ProgramAuthority (User + AcademicProgram + ProgramRole)
       ├── PasswordResetToken
       └── StudentProfile (código, créditos aprobados, GPA, semestre)
```

### Estructura académica
```
Faculty ──┬── AcademicProgram ── ProgramDegreeModality ── DegreeModality
          └── DegreeModality
```

### Modalidad de grado
```
StudentModality ──┬── StudentModalityMember (grupo)
                  ├── ModalityInvitation
                  ├── DefenseExaminer (jurados)
                  ├── DefenseEvaluationCriteria (rúbrica)
                  ├── ExaminerCertificate
                  ├── AcademicCertificate
                  ├── Seminar
                  └── ModalityProcessStatusHistory
```

### Documentos
```
RequiredDocument ── StudentDocument ──┬── ProposalEvaluation
                                      ├── FinalDocumentEvaluation
                                      ├── ExaminerDocumentReview
                                      ├── DocumentEditRequest ── DocumentEditRequestVote
                                      └── StudentDocumentStatusHistory
```

### Flujo de estados (`ModalityProcessStatus`)
```
MODALITY_SELECTED
  → UNDER_REVIEW_PROGRAM_HEAD
  → READY_FOR_PROGRAM_CURRICULUM_COMMITTEE
  → UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE
  → PROPOSAL_APPROVED
  → PENDING_PROGRAM_HEAD_FINAL_REVIEW
  → DEFENSE_SCHEDULED
  → EXAMINERS_ASSIGNED
  → DEFENSE_COMPLETED
  → UNDER_EVALUATION_PRIMARY_EXAMINERS
  → EVALUATION_COMPLETED / DISAGREEMENT_REQUIRES_TIEBREAKER
  → GRADED_APPROVED / GRADED_FAILED
  → MODALITY_CLOSED
```

---

## ⚙️ Configuración

### Variables de entorno (`.env`)

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | URL de conexión MySQL | `jdbc:mysql://localhost:3306/SIGMABD` |
| `DB_USERNAME` | Usuario MySQL | `root` |
| `DB_PASSWORD` | Contraseña MySQL | `root` |
| `JWT_SECRET` | Clave secreta HMAC-SHA256 (64 hex) | `E54791C3...` |
| `MAIL_HOST` | Servidor SMTP | `smtp.gmail.com` |
| `MAIL_PORT` | Puerto SMTP | `587` |
| `MAIL_USERNAME` | Correo SMTP | `sigmausco@gmail.com` |
| `MAIL_PASSWORD` | Contraseña SMTP (app password) | `xxxx` |
| `FRONTEND_URL` | URL del frontend para CORS | `http://localhost:5173` |
| `UPLOAD_DIR` | Directorio de subida de archivos | `./SIGMA-uploads/SIGMA-files` |

El `.env` se carga automáticamente al iniciar la aplicación mediante un `EnvLoader` personalizado, ANTES de que Spring resuelva sus propiedades.

### Perfiles

| Perfil | Activo por defecto | `ddl-auto` | Swagger |
|---|---|---|---|
| `dev` | No | `update` | Habilitado |
| `prod` | **Sí** | `validate` | Deshabilitado |

### Seed de roles y permisos

`DataInitializer` (`@Profile("dev")`, no corre en `prod`) crea los 60 permisos y 6 roles base (SUPERADMIN, PROGRAM_HEAD, PROGRAM_CURRICULUM_COMMITTEE, STUDENT, PROJECT_DIRECTOR, EXAMINER) de forma **idempotente** (create-if-absent; en dev no re-escribe permisos de roles existentes).

En `prod` el seed NO se ejecuta: la base fresca necesita que estos roles/permisos se carguen manualmente antes del primer arranque (los `@PreAuthorize` de los endpoints dependen de ellos). Procedimiento sugerido: arrancar una vez con `SPRING_PROFILES_ACTIVE=dev` contra la BD en blanco (crea rol/permiso con `ddl-auto=update`), o insertarlos vía script SQL replicando lo que hace `DataInitializer`.

---

## 🚀 Inicio rápido

### Prerrequisitos
- Java 21 (JDK)
- MySQL 8.x
- Maven 3.9+ (o usar `mvnw`)

### Desarrollo local

```bash
# 1. Clonar el repositorio
git clone <repo-url>
cd SIGMA

# 2. Configurar variables de entorno (copiar y editar .env)
cp .env.example .env

# 3. Ejecutar en modo desarrollo
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O bien:

```bash
# Compilar y ejecutar
./mvnw clean package -DskipTests
java --enable-preview -jar target/SIGMA-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
# Construir imagen
docker build -t sigma-backend .

# Ejecutar contenedor
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/SIGMABD \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=root \
  -e JWT_SECRET=... \
  sigma-backend
```

---

## 📖 Documentación de la API

| Recurso | URL |
|---|---|
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` |
| **OpenAPI JSON** | `http://localhost:8080/v3/api-docs` |

### Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/auth/register` | Registrar usuario |
| `POST` | `/auth/login` | Iniciar sesión (JWT) |
| `POST` | `/auth/forgot-password` | Solicitar recuperación |
| `POST` | `/auth/reset-password` | Restablecer contraseña |
| `GET` | `/admin/users` | Listar usuarios (admin) |
| `POST` | `/admin/registerUser` | Registrar usuario (admin) |
| `GET` | `/faculties/all` | Listar facultades |
| `GET` | `/academic-programs/all` | Listar programas |
| `GET` | `/modalities/{id}` | Detalle de modalidad |
| `GET` | `/students/profile` | Perfil de estudiante |
| `GET` | `/notifications/` | Mis notificaciones |
| `GET` | `/reports/{reportType}` | Reportes institucionales |
| `GET` | `/certificate/{studentModalityId}` | Descargar certificado |

---

## 📁 Estructura del proyecto

```
SIGMA/
├── src/main/java/com/SIGMA/USCO/
│   ├── SigmaApplication.java
│   ├── config/             # Configuraciones globales (CORS, Swagger, Async, DataInitializer)
│   ├── security/           # JWT, SecurityConfig, AppConfig, filtros
│   ├── Users/              # Auth, usuarios, roles, permisos
│   │   ├── controller/     # AuthController, AdminController, StudentController
│   │   ├── entity/         # User, Role, Permission, ProgramAuthority, etc.
│   │   ├── repository/
│   │   ├── service/        # AuthService, UserService, etc.
│   │   └── dto/
│   ├── academic/           # Facultades y programas académicos
│   │   ├── controller/     # FacultyController, AcademicProgramController, etc.
│   │   ├── entity/         # Faculty, AcademicProgram, StudentProfile, etc.
│   │   ├── repository/
│   │   ├── service/
│   │   └── dto/
│   ├── Modalities/         # Modalidades de grado
│   │   ├── Controller/     # ModalityController, ModalityGroupController
│   │   ├── Entity/         # StudentModality, DefenseExaminer, Seminar, etc.
│   │   ├── repository/
│   │   ├── service/        # ModalityService, DefenseService, etc.
│   │   └── dto/
│   ├── documents/          # Gestión documental
│   │   ├── controller/     # DocumentController, TemplateDocumentController
│   │   ├── entity/         # StudentDocument, ProposalEvaluation, etc.
│   │   ├── repository/
│   │   ├── service/
│   │   └── dto/
│   ├── notifications/      # Notificaciones y eventos
│   │   ├── controller/     # NotificationController
│   │   ├── entity/         # Notification
│   │   ├── event/          # Eventos del dominio
│   │   ├── listeners/      # Manejadores asíncronos
│   │   ├── publisher/
│   │   ├── repository/
│   │   ├── service/
│   │   └── dto/
│   └── report/             # Reportes institucionales
│       ├── controller/     # GlobalModalityReportController
│       ├── service/
│       ├── dto/
│       └── enums/          # ReportType
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   └── application-prod.properties
├── Dockerfile
├── pom.xml
├── mvnw / mvnw.cmd
└── .env
```

---

## 🧪 Ejecutar pruebas

```bash
./mvnw test
```

---

## 📄 Licencia

Este proyecto es propiedad de la **Facultad de Ingeniería — Universidad Surcolombiana (USCO)**.

Desarrollado como proyecto de grado para la gestión interna de modalidades académicas.
