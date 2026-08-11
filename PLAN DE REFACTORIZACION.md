**INFORME POR MÓDULO**

2\.1 Users + security (36 archivos)

Estado: El patrón dominante del proyecto: services devolviendo ResponseEntity con Map.of(...), validación manual, sin advice.

Problemas:

- AdminService (916 líneas, 15 dependencias de 4 módulos) — god class; registerUserByAdmin de 160 líneas con 3 ramas.
- AdminService.getUsers: findAll() completo + N+1 (findByUserId + findByUser\_Id por usuario) + UserResponse.builder() triplicado.
- AuthService.sendResetPasswordLink: link hardcodeado [http://localhost:5173](http://localhost:5173/), token de 10 min pero el correo dice 15, y email inexistente → 500 (enumeration leak).
- AuthService.resetPassword: 2 saves sin @Transactional.
- StudentController.viewMyDocument: accede a 2 repos + filesystem directamente, duplicando StudentService.viewMyDocument que queda muerto.
- User/Role con @ManyToMany(EAGER) en ambas direcciones — cada login carga roles+permisos.
- AdminService usa jakarta.transaction.Transactional (JTA) — mezclado con la de Spring.
- BlackListFilter: existsByToken contra MySQL por request, tabla sin TTL ni limpieza.
- JwtAuthenticationFilter: token inválido → continúa la cadena → 403 en vez de 401.
- StudentService:197 captura Error (incluye OOM); bug de nombre de carpeta: name + lastName + "\_" + lastName + "\_" + id.
- 4 try/catch idénticos en AdminController (400 con mensaje crudo).
- Código muerto: studentProfileRepository sin usar, userHasStudentRole muerto, imports sin usar en ProgramAuthorityRepository.

Riesgos: errores de dominio (usuario no existe) responden 500; el registro masivo degrada; logout escala mal.

Recomendaciones: extraer assignAuthority unificado; mover getUsers a query derivada con paginación; arreglar reset-password (tx, link config, mensaje de tiempo); 401 en el filter; quitar EAGER.



2\.2 Modalities (74 archivos) — el núcleo

Estado: El defecto está en la capa de servicio.

Problemas:

- God services: DocumentModalityService 3.769 líneas, ~31 transiciones de estado inline, 12 @Transactional, 30+ publishEvent, 10+ repos de 4 módulos.
- ResponseEntity en los 8 services (100+ retornos con status(403), badRequest(), INTERNAL\_SERVER\_ERROR).
- try/catch idéntico repetido ~8 veces solo en SeminarModalityService.
- 56 publicaciones de evento con payload Map<String,Object> stringly-typed — contrato sin tipos; publicación síncrona dentro de la tx (un listener que falle revierte el negocio).
- ~60 sitios con ModalityProcessStatusHistory.builder() duplicado — agregar un estado toca decenas de puntos.
- @Valid en 3 de ~60 endpoints; solo 3 DTOs con constraints.
- ModalityController (867 líneas): lógica de archivos, UrlResource, dependencias de 4 módulos, wildcard dto.\*; ModalityGroupController inyecta DocumentService directo.
- ModalityProcessStatusHistoryRepository.java:17: retorno Arrays (bug de compilación-runtime confirmado) — la query ...Desc no puede materializarse.
- SeminarRepository.enrollStudent nativo vs @ManyToMany enrolledStudents en la entidad → colección stale en sesión.
- ExaminerEvaluation marcada @deprecated pero 100% cableada (12 métodos de repo con lógica de tiebreaker duplicada).
- Proyecciones Object[] posicionales en 2 repos; List<String> contra columnas enum.
- N+1 en listados (~3N queries); 0 readOnly=true en consultas.
- "Jurado Principal 1/2" traducido en 7 archivos / 11 sitios.

Riesgos: cambio de una regla de negocio = tocar decenas de métodos; el repo roto puede tirar endpoints de historial; los eventos stringly-typed rompen en runtime sin warning.

Recomendaciones: primero el bug del Arrays; luego helper de transición de estado (StatusTransitionHelper), eventos tipados, quitar HTTP de los services, dividir DocumentModalityService por flujo.



2\.3 academic (23 archivos)

Estado: CRUD de Facultad/Programa/Modalidad-de-grado + parser de historiales académicos PDF. Los repos (StudentProfileRepository) son el hub compartido por ~15 services de otros módulos.

Problemas:

- AcademicHistoryPdfParserService no hace OCR (solo extracción iText5+PDFBox; rechaza PDFs escaneados) mientras tess4j 5.8.0 (~100 MB con nativos) está en el pom sin un solo import — AGENTS.md describe algo inexistente.
- Parsing síncrono en el hilo de request: file.getBytes() hasta 20 MB, 3 estrategias por archivo, sin timeout; bloquea Tomcat en CPU-bound.
- 3 controllers con try/catch + Map.of duplicados; RuntimeException→404 en uno, IllegalArgumentException→400 en otro (misma causa, distinto código).
- Bean Validation declarada en FacultyDTO pero nunca disparada (ningún @Valid); reglas duplicadas como if manuales en los services.
- FacultyService/AcademicProgramService sin @Transactional (TOCTOU en existsByCode + save); acceso a colección LAZY sin tx (funciona solo por OSIV).
- Entidades JPA serializadas al cliente (respuestas inconsistentes entre métodos).
- Ciclo de paquetes: academic importa Modalities (entidades y JPQL cross-módulo) y viceversa — hub bidireccional.
- iText 5.5.13.3 (EOL) + PDFBox 2.0.30 (viejo) duplicados para la misma tarea.

Riesgos: subida de historial lenta cae la app; la dependencia muerta infla el jar ~100 MB; ciclo de paquetes complica cualquier refactor de entidades.

Recomendaciones: mover parsing a async (o al menos ThreadPool), decidir OCR sí/no (si no se usa → quitar tess4j), @Transactional + validación en DTOs, unificar respuesta en DTOs.



2\.4 documents (42 archivos)

Estado: gestión de documentos (subida/descarga/plantillas/evaluación de propuestas) + actas PDF. Capas presentes, pero mismo vicio de ResponseEntity en services.

Problemas:

- AcademicCertificateTestController.generateTestCertificate — endpoint "Test" de producción sin @PreAuthorize ni verificación de pertenencia: cualquier usuario autenticado descarga el acta PDF de cualquier modalidad. Crítico de seguridad.
- DocumentService/ProposalEvaluationService/TemplateDocumentService devuelven ResponseEntity; SecurityUtils.getCurrentUser() dentro del service.
- ProposalEvaluationService.getEvaluationsByDocument no valida autorización — cualquier usuario lee evaluaciones ajenas.
- uploadCancellationDocument sin @Transactional (guardado parcial si falla el historial); validación solo por extensión (no MIME).
- DocumentModalityService:338: substring(lastIndexOf('.')) → StringIndexOutOfBounds con nombres sin punto; carpetas con nombres sin sanitizar (path traversal bajo).
- isCompleteModality duplicado con StudentNotificationListener:1457.

Riesgos: filtración de documentos/evaluaciones; guardado parcial de datos.

Recomendaciones: @PreAuthorize + ownership check en descargas; validación MIME; @Transactional; delegar archivos a un ResourceService.



2\.5 notifications (23 archivos)

Estado: el módulo mejor estructurado tras el refactor previo (ModalityEvent único, NotificationFactory, TranslationUtils, 5 listeners, dispatcher async). Pero tiene 3 defectos serios de diseño transaccional.

Problemas:

- Race condition crítica: los 5 listeners usan @EventListener con multicaster async → se ejecutan antes del commit del publicador. El findById() del listener puede no ver la fila aún no commiteada → orElseThrow() mata el handler → notificación perdida silenciosamente. Deberían ser @TransactionalEventListener(phase = AFTER\_COMMIT).
- Doble salto de hilo: multicaster async → listener → dispatch() @Async — 2 tareas del executor por notificación; con 24 hilos y cola 500, un seminario masivo satura → RejectedExecutionException sin retry.
- try/catch muerto: los try/catch alrededor de dispatcher.dispatchWithAttachment() son código muerto (la excepción ocurre en el hilo async; el fallback dispatch(notification) jamás corre).
- Lazy loading sin transacción: los listeners (excepto ProgramHead) acceden a colecciones LAZY de entidades detached en hilos async (sin OSIV) → LazyInitializationException mata el handler.
- StudentNotificationListener 1.465 líneas, 27 handlers, switch de 26 casos; mezcla orquestación + texto de correo + generación de PDF.
- DefenseModalityService:631 inyecta y llama un listener directamente (acoplamiento no-evento) además de publicar el mismo evento en 634.
- NotificationTemplate entidad muerta (plantillas hardcodeadas en text-blocks en 5 listeners).
- ProjectTitleExtractionService (listener) ejecuta lógica de negocio de documentos — responsabilidad invertida.
- AcademicCertificatePdfService 798 líneas: 2 métodos casi idénticos; generateCertificateNumber hace findAll() por acta (O(n), condición de carrera).
- Bug por copy-paste: CommitteeNotificationListener — correo de cancelación dice "aprobada por la Jefatura del Programa".
- ModalityEvent usa studentModalityId = 0L mágico en eventos de seminario.

Riesgos: pérdida silenciosa de notificaciones (el peor síntoma: nadie se entera); fallos intermitentes tipo "a veces llega, a veces no".

Recomendaciones: @TransactionalEventListener(AFTER\_COMMIT) en los 5 listeners + @Transactional(readOnly=true); un solo salto de hilo (o async en el listener y dispatch síncrono, o quitar el @Async de dispatch); quitar try/catch muertos; extraer construcción de correos a plantillas/templates.

2\.6 report (52 archivos)

Estado: 17 endpoints en GlobalModalityReportController (999 líneas, 18 dependencias) → 10 services de agregación → 7 PDF generators (816-1.633 líneas c/u). DTOs consistentes. Cero caché, paginación o validación.

Problemas:

- findAll() de student\_modality completa 1-2 veces por request en 6 services, filtrando en Java por programa (el programa lo conoce el usuario autenticado — una derived query lo reemplaza). Los repos ya tienen findForProgramHead\* que el módulo ignora.
- ~19 loops N+1 (findByStudentModalityIdAndStatus() por modalidad + findById() por miembro) en 7 services → cientos de round-trips por reporte.
- Datos fabricados presentados como reales: cancelled(0), averageGrade(null), topDirectorsCurrentYear(new ArrayList<>()), maxStudentsInGroup(3), successRateByYear({}), averageDefenseDuration(120.0), trend("STABLE") — con comentarios "Se puede calcular si se necesita".
- Bug silencioso confirmado: CompletedModalitiesReportService:455 compara labels traducidos al español ("MERITORIOUS", "LAUREATE") contra un campo ya traducido → los conteos de distinción siempre dan 0.
- StudentReportService: year/semester se concatenan al label pero nunca se aplican como filtro — el reporte ignora sus parámetros.
- Duplicación ~50% entre los 7 generators (pipeline Document+PageEvent+cover+headers+footer idéntico); barras de gráficos dibujadas 5+ veces; footers privados en 2 generators en vez del compartido.
- translateStatus duplicado 3 veces con redacciones distintas (DefenseCalendarReportService 50 líneas, ModalityTraceabilityReportService, TranslationUtils) → el mismo estado muestra labels distintos según el reporte.
- 6 services importan TranslationUtils desde notifications.listeners — dirección de dependencia invertida (módulo de bajo nivel).
- GlobalModalityReportController: 7 try/catch casi idénticos, Map.of de envoltura 13×, @CrossOrigin("\*") (sobreescribe el CORS global), LocalDateTime.parse sin @DateTimeFormat → fecha malformada = 500, sin check startDate <= endDate.
- CompletedModalitiesReportService es el único sin @Transactional(readOnly=true) → riesgo LazyInitializationException + cada query commitea su tx.
- ReportUtils.getAuthenticatedUserProgram usa authorities.get(0) → usuario multi-programa reporta contra un programa arbitrario.
- /reports/health sin @PreAuthorize.

Riesgos: OOM con datos crecientes; números incorrectos en decisiones académicas (la peor clase de bug); LazyInitializationException intermitente.

Recomendaciones: refactor de consultas (derived queries con JOIN FETCH + readOnly=true), corregir distinción y filtros, base común de PDF, mover TranslationUtils a common, @RestControllerAdvice para fechas, quitar @CrossOrigin("\*").




2\.7 config (6 archivos)

Estado: cohesivo y pequeño. EnvLoader, EmailService, DataInitializer, CorsConfig, AsyncEventConfig, SwaggerConfig (springdoc).

Problemas:

- DataInitializer re-escribe permisos de roles en cada arranque (cambios operativos se pierden) y está @Profile("dev") mientras el perfil activo es prod → en BD fresca de prod nunca seedea roles/permisos.
- EnvLoader: .env relativo al CWD (no existe en Docker → falla silenciosa); parseo con java.util.Properties (passwords con =/espacios se rompen).
- AsyncEventConfig: errorHandler solo loguea → fallo de email = notificación perdida sin alerta; el multicaster async global deshabilita @TransactionalEventListener (los eventos se ejecutan sin transacción — agrava el bug de §2.5).
- EmailService: solo texto plano, sin plantillas ni reintento; MessagingException→RuntimeException.

Riesgos: seed inconsistente entre entornos; despliegue Docker sin configuración de email silenciosamente.

Recomendaciones: seed idempotente con create-if-absent y @Profile("dev"|"seed") o data.sql versionada; EnvLoader con ruta absoluta/parametrizada.












**PLAN MAESTRO DE REFACTORIZACIÓN — SIGMA Backend**

Base: auditoría arquitectónica completa (257 archivos, 8 módulos, 5.5/10). Reglas: no romper funcionalidad, minimizar riesgo, omitir escritura de tests (verificación = compilación + smoke manual), evitar sobreingeniería (sin MapStruct, sin CQRS, sin microservicios, sin interfaces de una sola implementación).


**ESTRATEGIA GENERAL**

Principio rector: transversal primero, módulos después, rendimiento al final

El 80% de los problemas de la auditoría son 3 patrones transversales repetidos en todos los módulos:

1. Manejo de errores inexistente (try/catch + ResponseEntity + mensajes crudos).
1. Servicios acoplados a HTTP (124 métodos devuelven ResponseEntity).
1. Validación manual duplicada (3 @Valid en todo el repo).

Si refactorizáramos módulo por módulo sin resolver antes estos tres, cada módulo se tocaría dos veces: una para el patrón transversal y otra para su refactor específico. Por eso el orden es:

Fase 0  Estabilización (bugs + seguridad)      → nada depende de esto, todo lo bloquea

Fase 1  Bases transversales (common + patrones) → prerrequisito de Fase 2

Fase 2  Migración módulo por módulo            → depende de Fase 1

Fase 3  Reportes: datos y consultas            → depende de Fase 2 (report)

Fase 4  God classes                            → requiere módulos limpios (Fase 2)

Fase 5  PDF generators                         → independiente, mecánico

Fase 6  Limpieza y calidad final               → al final, toca todo

Qué debe esperar (y por qué)

- God classes (Fase 4): dividir DocumentModalityService (3.769 líneas) antes de quitarle el ResponseEntity sería reescribir dos veces cada método. Primero se limpia el patrón HTTP, después se divide.
- Rendimiento de consultas (Fase 3): los findAll()/N+1 son aditivos y de bajo riesgo, pero los bugs de datos falsos (métricas fabricadas, distinción siempre 0) NO esperan — son Fase 0 por corrupción de información.
- PDF generators (Fase 5): no bloquean nada; se consolidan en un lote mecánico.



Dependencias entre refactorizaciones

Cambio	Depende de	Por qué

@RestControllerAdvice	Fase 0 (bugs)	Un advice con errores de dominio mal mapeados escondería bugs

Services sin ResponseEntity	Excepciones + advice	El service deja de decidir status HTTP; el advice lo decide

Bean Validation en DTOs	Services sin HTTP	Los constraints se validan en el controller (límite de confianza)

@TransactionalEventListener	Fase 0 (tx reset)	Cambia semántica de comportamiento; debe ir antes de tocar listeners

División de god classes	Fase 2 completa	Extraer métodos de una clase que aún devuelve ResponseEntity = doble trabajo

Movimiento de TranslationUtils a common	Fase 1 (creación de common)	Una sola mudanza, todos los imports se corrigen en el mismo commit


Reglas de oro (las establece Fase 1, las aplica Fase 2)

1. Controllers: @Valid, delegación total al service, ResponseEntity solo aquí.
1. Services: dominio puro; lanzan excepciones de negocio; nunca HTTP.
1. Errores: 1 @RestControllerAdvice global + excepciones propias (BusinessException, NotFoundException).
1. Transacciones: org.springframework.transaction.annotation.Transactional, readOnly=true en lecturas.
1. Eventos: @TransactionalEventListener(AFTER\_COMMIT) + payload tipado.
1. common es el único lugar de donde los módulos importan utilidades compartidas.




FASE 0 — ESTABILIZACIÓN DEL SISTEMA

Objetivo: eliminar bugs y riesgos que corrompen datos, rompen runtime o exponen el sistema. Sin refactor estructural. Orden de ejecución por severidad.

0\.1 Bug crítico de runtime

Problema	ModalityProcessStatusHistoryRepository.java:17 declara retorno java.util.Arrays en query derivada → Spring Data no puede materializar la consulta

Impacto	Falla de runtime/arranque en cualquier uso del historial descendente

Solución	Cambiar el tipo a List<ModalityProcessStatusHistory> (método ...Desc). Eliminar el import java.util.Arrays

0\.2 Vulnerabilidades de seguridad (ordenadas)

Problema	AcademicCertificateTestController.generateTestCertificate — endpoint "Test" en producción, sin @PreAuthorize, sin verificación de pertenencia; cualquier usuario autenticado descarga el acta de cualquier modalidad

Impacto	Fuga de documentos académicos de terceros

Solución	Eliminar el endpoint (o moverlo a dev-only con @Profile("dev")) + @PreAuthorize y ownership check en el equivalente legítimo

Problema	GlobalModalityReportController.java:66 — @CrossOrigin("\*") sobreescribe el CORS global (FRONTEND\_URL)

Impacto	Cualquier origen puede llamar a /reports/\*\*

Solución	Quitar la anotación; confiar en CorsConfig

Problema	ProposalEvaluationService.getEvaluationsByDocument no valida autorización; DocumentService sirve archivos sin ownership check

Impacto	Lectura de evaluaciones/documentos ajenos

Solución	Ownership check (módulo de pertenencia: el documento pertenece a un StudentModality del usuario) en la consulta; prueba manual de 403

Problema	AuthService.sendResetPasswordLink (Users) — email inexistente → 500 (user enumeration) + link hardcodeado http://localhost:5173 + token 10 min vs mensaje "15 min"

Impacto	Filtra existencia de cuentas; link roto en producción

Solución	Respuesta 200 genérica en ambos casos; FRONTEND\_URL de config; unificar mensaje a 10 min



0\.3 Pérdida/incorrección de datos

Problema	CompletedModalitiesReportService.java:455-458 — compara labels traducidos al español contra campo ya traducido → conteos de distinción SIEMPRE 0

Impacto	Estadísticas incorrectas que se reportan oficialmente

Solución	Comparar contra el enum AcademicDistinction crudo (mismo patrón que buildDistinctionAnalysis)

Problema	Métricas fabricadas en reportes (HistoricalReportService, DefenseCalendarReportService, etc.): cancelled(0), trend("STABLE"), averageDefenseDuration(120.0), maxStudentsInGroup(3)

Impacto	Datos inventados presentados como reales en actas oficiales

Solución	Marcar con valores null/omitir del DTO donde no haya cálculo real; calcular las 2-3 viables con queries existentes. No rellenar con placeholders

Problema	StudentReportService:66-79 — year/semester se concatenan al label pero nunca se aplican como filtro

Impacto	El reporte ignora sus parámetros

Solución	Aplicar los filtros en la query (derived query con año/semestre) o devolver 400 si aún no se implementa

Problema	AuthService.resetPassword — 2 saves sin @Transactional (contraseña nueva guardada, token no marcado)

Impacto	Estado parcial

Solución	@Transactional sobre el método

Problema	DocumentService.uploadCancellationDocument sin @Transactional (guardado parcial si falla el historial); DocumentModalityService:338 substring(lastIndexOf('.')) → StringIndexOutOfBounds con nombres sin punto

Impacto	Datos inconsistentes; 500 en subida de archivo sin extensión

Solución	@Transactional + FilenameUtils.getExtension (Apache Commons de poi, ya presente) con guarda de null


0\.4 Transaccionales/eventos (pérdida silenciosa de notificaciones)

Problema	Los 5 listeners usan @EventListener con multicaster async → corren ANTES del commit del publicador; findById() no ve la fila → orElseThrow() mata el handler

Impacto	Notificaciones perdidas silenciosamente (el estudiante nunca se entera)

Solución	(a) A corto plazo: @TransactionalEventListener(phase = AFTER\_COMMIT) en los 5 listeners. (b) Agregar @Transactional(readOnly=true) en cada listener para el acceso a colecciones LAZY. Nota: con el multicaster async los eventos corren sin transacción — el fix definitivo es Fase 1.4

Problema	try/catch muerto alrededor de dispatcher.dispatchWithAttachment (excepción ocurre en hilo async, el fallback nunca corre)

Impacto	Reintento fantasma

Solución	Eliminar los try/catch; el manejo real es emailSent=false + log







**FASE 1 — CAMBIOS ARQUITECTÓNICOS GLOBALES**

1\.1 Crear paquete common (base de todo)

Actualmente: no existe com.SIGMA.USCO.common; TranslationUtils vive en notifications.listeners y 6 services de report la importan (dependencia invertida); utilidades duplicadas (traducciones en 7 archivos).

Propuesto:

com.SIGMA.USCO.common

├── exception/  ApiException, BusinessException, NotFoundException, GlobalExceptionHandler

├── web/        ApiResponse<T> (envelope estándar), PaginatedResponse<T>

└── util/       TranslationUtils (movido de notifications.listeners)



Cambios: mover TranslationUtils y actualizar los ~15 imports (listeners y report); crear las excepciones; crear GlobalExceptionHandler. Riesgo: bajo (movimiento mecánico + nuevos archivos); se valida con compile.


1\.2 Manejo centralizado de excepciones

Actualmente (patrón repetido ~40 veces):

// Controller (AdminController:164-174, FacultyController:39-59, GlobalModalityReportController, ...)

try {

`    `return service.asignar(...);

} catch (Exception e) {

`    `return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));

}

// y el service lanza: throw new RuntimeException("El rol no existe");

Propuesto:

// common/exception/BusinessException.java

public class BusinessException extends RuntimeException {

`    `public BusinessException(String message) { super(message); }

}

public class NotFoundException extends BusinessException {

`    `public NotFoundException(String message) { super(message); }

}

// common/exception/GlobalExceptionHandler.java

@RestControllerAdvice

public class GlobalExceptionHandler {

`    `@ExceptionHandler(NotFoundException.class)        // → 404

`    `@ExceptionHandler(BusinessException.class)        // → 400

`    `@ExceptionHandler(MethodArgumentNotValidException.class) // → 400 con campo+mensaje

`    `@ExceptionHandler(DateTimeParseException.class)   // → 400 "Formato de fecha inválido"

`    `@ExceptionHandler(DataIntegrityViolationException.class) // → 409

`    `@ExceptionHandler(Exception.class)                // → 500 genérico, log con stacktrace, NUNCA e.getMessage()

}

Regla: e.getMessage() jamás viaja al cliente; los servicios reemplazan RuntimeException por BusinessException/NotFoundException a medida que se migran (Fase 2).

Riesgo: medio — el advice cambia códigos HTTP hoy inconsistentes (misma causa da 404 en un controller y 400 en otro). Mitigación: primero mapear el comportamiento existente dominante (400 para validación de negocio, 404 para no encontrado), documentar en el commit.



1\.3 Desacoplar services de HTTP (los 124 métodos)

Actualmente:

// SeminarModalityService.java:52

public ResponseEntity<?> createSeminar(SeminarDTO request) {

`    `try {

`        `Seminar seminar = ...;

`        `return ResponseEntity.ok(seminar);

`    `} catch (IllegalArgumentException e) {

`        `return ResponseEntity.badRequest().body(e.getMessage());

`    `}

}

Propuesto:

// Service (dominio puro)

public SeminarResponse createSeminar(SeminarDTO request) {   // sin ResponseEntity, sin try/catch

`    `if (seminarRepository.existsByName(request.name())) {

`        `throw new BusinessException("Ya existe un seminario con ese nombre");

`    `}

...

}

// Controller (único dueño de HTTP)

@PostMapping

public ResponseEntity<ApiResponse<SeminarResponse>> createSeminar(@Valid @RequestBody SeminarDTO request) {

`    `return ResponseEntity.ok(ApiResponse.success(service.createSeminar(request)));

}



1\.4 Bean Validation real

Actualmente: 3 @Valid en 867 líneas de ModalityController; constraints muertas en FacultyDTO; validaciones manuales duplicadas (email @usco.edu.co en 2 services, rangos gpa en StudentService, checks de nombre en 3 servicios de seminarios).

Propuesto:

public record RegisterUserByAdminRequest(

`    `@NotBlank @Email String email,

`    `@NotBlank @Size(min = 6, max = 60) String password,

`    `@NotBlank String firstName, @NotBlank String lastName

) {}

// Validación de dominio (la única que vive en el service):

if (!email.endsWith("@usco.edu.co")) throw new BusinessException("Debe usar correo institucional");

Clasificación (pedida por la auditoría):

Validación	Destino

Campos obligatorios, formato email, tamaños, rangos numéricos	Bean Validation en DTOs (reemplaza los if de los services)

Email institucional, programa↔facultad, permisos de estado (transiciones)	Reglas de negocio en service (excepción de dominio)

Reglas repetidas entre flujos (validez de fecha de defensa, rangos)	1-2 validadores personalizados en common si se repiten ≥3 veces

Checks de unicidad (nombre de seminario, código de facultad)	Service con existsBy\* + manejo de DataIntegrityViolationException (409)



1\.5 Unificación transaccional

Actualmente: jakarta.transaction.Transactional en 12 archivos (JTA — Spring 6 ignora readOnly/propagation/isolation/timeout silenciosamente) mezclada con la de Spring; 0 readOnly=true en consultas.

Propuesto:

- Mecánico: reemplazar el import en los 12 archivos por org.springframework.transaction.annotation.Transactional.
- En servicios de lectura pura (listados, catálogos, reportes): @Transactional(readOnly = true).
- En multi-writes sin anotación (resetPassword, uploadCancellationDocument): @Transactional.

Riesgo: bajo (cambio de import con misma semántica base; readOnly es optimización segura). Verificación: compile + smoke de escrituras en cada módulo.



1\.6 Corrección del pipeline de eventos

Actualmente: publicador → multicaster async (hilo A) → listener (@EventListener, sin tx, accede a colecciones LAZY de entidades detached) → dispatch() @Async (hilo B) → SMTP. 2 saltos de hilo, sin transacción en el listener, RejectedExecutionException en envíos masivos sin retry.

Propuesto (un solo salto, después del commit):

@TransactionalEventListener(phase = TransactionPhase.AFTER\_COMMIT)

public void handleEvent(ModalityEvent event) {

`    `// corre en hilo async (multicaster), DESPUÉS del commit del publicador

`    `// @Transactional(readOnly = true) para las lecturas del handler

}

- dispatch() pierde @Async (queda síncrono dentro del hilo del listener → 1 salto total, cola ya no se satura al doble).
- Los try/catch muertos se eliminan; el errorHandler del multicaster loguea con contexto (evento + listener) para alerta manual.
- Eventos tipados: ModalityEvent gana campos fuertes (opcionales por evento) en vez de Map<String,Object> + studentModalityId = 0L mágico — se hace al tocar cada publicador en Fase 2.

Riesgo: medio-alto — cambia timing de entrega (AFTER\_COMMIT). Mitigación: Fase 1.6 corre tras 0.4 (que ya aplica AFTER\_COMMIT parcial) y antes de tocar cualquier listener en Fase 2.



1\.7 Convenciones de contrato (DTOs de respuesta)

- Un solo envelope ApiResponse<T>: {success, message, data, timestamp} (compat con el shape actual).
- Servicios devuelven DTOs de respuesta (\*Response), nunca entidades JPA (hoy FacultyService.createFaculty devuelve Faculty).
- Mappers manuales (sin MapStruct — ponytail): se dejan como están; solo se unifican los builders triplicados (p.ej. UserResponse.builder() en AdminService).

Verificación Fase 1: compile EXIT 0 por submódulo (1.1-1.7) + smoke de un flujo completo: login → crear modalidad → aprobar → notificación → reporte. El contrato JSON debe verse idéntico desde el frontend.





**FASE 2 — MIGRACIÓN POR MÓDULO (aplicando las reglas de Fase 1)**

Orden de ejecución (piloto pequeño primero para validar patrones, grande al final):

2\.1 academic (piloto — 23 archivos, 0 ResponseEntity en services)

- @Valid en los 3 controllers (reactivar las constraints ya declaradas en FacultyDTO/ProgramDTO).
- Quitar entidades como retorno → DTOs consistentes entre métodos.
- @Transactional en FacultyService/AcademicProgramService (TOCTOU en existsByCode).
- Sin try/catch + Map.of en controllers (ya cubierto por el advice).



2\.2 Users (36 archivos — 23 métodos HTTP)

- Migrar AuthService (3), StudentService (5), AdminService (15) a dominio puro + ApiResponse.
- Extraer assignAuthority(userId, programId, roleName, ProgramRole) (3 métodos duplicados).
- getUsers: query derivada con filtros + paginación (mata el findAll() + N+1 + builder triplicado).
- Bean Validation en los 6 request DTOs; quitar validación manual duplicada.
- Bug del link de frontend ya resuelto en 0.2; aquí se externaliza a config.
- StudentController.viewMyDocument: delegar a StudentService.viewMyDocument (hoy muerto) y eliminar repos/filesystem del controller; quitar código muerto (studentProfileRepository, userHasStudentRole).
- User/Role: EAGER → LAZY + consulta de login con join fetch (o conservar EAGER solo en Role.permissions si el N+1 se mantiene bajo — decisión a validar con perf).

2\.3 documents (42 archivos — 9 métodos HTTP)

- Migrar DocumentService (5), ProposalEvaluationService (3), TemplateDocumentService (1).
- Ownership checks (0.2) consolidados en un helper de common (ResourceAccessPolicy si se repite).
- @Transactional en uploads; MIME check (ya hay MediaType disponible) sin romper el contrato de extensión actual.
- ProjectTitleExtractionService: mover de notifications a documents (es lógica de documentos).

2\.4 notifications (23 archivos)

- Los 5 listeners ya con @TransactionalEventListener (0.4/1.6): quitar @Async de dispatch, quitar try/catch muertos.
- StudentNotificationListener: extraer construcción de correos → NotificationMessageTemplates (ya existe parcialmente) — reducir el switch de 26 casos; quitar duplicados inline de "Jurado Principal/Desempate" y lista de estudiantes (ya existen en TranslationUtils).
- Eliminar NotificationTemplate muerta y NotificationRepository duplicado (findByRecipient\_Id vs findByRecipientId).
- DefenseModalityService:631: reemplazar la llamada directa al listener por publishEvent (ya hay evento en 634).
- AcademicCertificatePdfService: unificar generateCertificate/generateCertificateForCommitteeApproval; generateCertificateNumber con query de max (no findAll()).

2\.5 Modalities (74 archivos — 88 métodos HTTP, el grueso)

- Migrar los 8 services a dominio puro (el trabajo mecánico más grande; dividido en 3 sub-lotes: catálogo/grupos/ediciones → seminarios/cancelaciones/listados → defensas/documentos).
- Bean Validation en todos los request DTOs (@Valid en ~60 endpoints).
- Helper de transición de estado (ModalityStatusTransition en Modalities/service o common): centraliza builder + changeDate + responsible + observations + save (hoy ~60 sitios) → cada transición es 1 llamada. OCP: agregar un estado = tocar 1 switch.
- ModalityController (867 líneas): mover servido de archivos a service; quitar imports de 4 módulos.
- Eliminar ExaminerEvaluation deprecada + su repo (12 métodos) — verificar antes que nada la consuma.
- SeminarRepository.enrollStudent: JPQL INSERT a operación de entidad (sync del persistence context) o @Modifying(flushAutomatically=true).
- Proyecciones Object[] → interfaces proyectadas.
- Unificar "Jurado Principal/Desempate" → TranslationUtils.translateExaminerType (3 sitios inline restantes).

2\.6 report (52 archivos — solo validación + advice, sin migración HTTP)

- @Valid/@DateTimeFormat en parámetros de fecha; DateTimeParseException ya mapeada (1.2); check startDate <= endDate.
- Quitar @CrossOrigin("\*") (ya en 0.2); @PreAuthorize en /reports/health.
- ReportUtils.getAuthenticatedUserProgram: authorities.get(0) → resolver por match de programa o 400 explícito.
- Mover TranslationUtils (ya hecho en 1.1); eliminar translateStatus duplicados → llamar a TranslationUtils (DefenseCalendarReportService:693, ModalityTraceabilityReportService:357).

Verificación Fase 2: compile EXIT 0 por módulo; smoke de 3-5 endpoints por módulo; contrato JSON idéntico (diff del shape).




**FASE 3 — REPORTES: DATOS Y CONSULTAS**

- Sustituir los 6 findAll() completos por derived queries con filtro por academicProgramId (el programa lo da el usuario autenticado) + JOIN FETCH de las colecciones usadas. Los repos ya tienen findForProgramHead\*/findByIdWithMembers sin usar — activarlos.
- Eliminar los ~19 loops N+1 (findByStudentModalityIdAndStatus por ítem) con joins/batch; studentProfileRepository.findById() por miembro → findAllById batch.
- @Transactional(readOnly = true) en los 10 services (el único sin anotar: CompletedModalitiesReportService).
- Revisar ModalityHistoricalPdfGenerator y amigos: findAll() doble por request → una sola carga con proyección.
- Evaluar caché simple (@Cacheable sobre los DTOs agregados, TTL corto) solo si las métricas lo piden — no adelantarse.
- Medida: tiempo de un reporte histórico antes/después (objetivo: eliminar al menos 80% de las queries por request).



FASE 4 — DIVISIÓN DE GOD CLASSES

Precondición: Fase 2 completa (clases ya sin HTTP, con validación y transacciones limpias → extraer es mecánico).

Clase	Hoy	División propuesta

DocumentModalityService	3.769 líneas, ~31 transiciones	DocumentWorkflowService (flujo principal) + ModalityDocumentService (archivos) + ModalityStatusService (transiciones, usando el helper de 2.5)

DefenseModalityService	2.154	DefenseWorkflowService + DefenseEvaluationService (rúbrica/tiebreaker)

StudentModalityListingService	1.378	ModalityQueryService (lecturas, readOnly) + listados DTO

GlobalModalityReportController	999, 18 deps	3 controllers temáticos (HistoricalReportsController, DirectoryReportsController, PdfReportsController) + envelope común

StudentNotificationListener	1.465	Orquestador + NotificationTemplates (texto) + delegación a AcademicCertificatePdfService (PDF)

AdminService	916, 15 deps	UserAdminService (CRUD usuarios) + AuthorityAssignmentService + ProgramAssignmentService

Regla: un split = un commit, sin cambiar firmas públicas de los endpoints (los controllers se adaptan), compile + smoke por split.



**FASE 5 — PDF GENERATORS (base común)**

- Extraer base BaseReportPdfGenerator (template method): pipeline Document A4 + PdfWriter + InstitutionalPageEventHelper + cover + headers + footer (~50% de los 8.459 líneas es boilerplate compartido).
- Unificar barras de gráficos: una firma addBarCell(table, label, value, max, color) en InstitutionalPdfHeader (hoy 5+ copias).
- Eliminar los 2 PageEventHelper privados → usar el compartido.
- Sin sobreingeniería: no abstraer el contenido (cada reporte conserva su sección específica); solo el esqueleto.
-----
**8. FASE 6 — LIMPIEZA Y CALIDAD FINAL**

- Dependencias: decidir OCR sí/no para historiales escaneados. Si no se implementa: quitar tess4j (~100 MB) y poi-ooxml si no se usa en runtime; iText 5.5.13.3 (EOL) → evaluar migración a OpenPDF/PDFBox unificado (bajo riesgo, mecánico). Quitar spring-ai-bom del dependencyManagement si no hay uso (verificar).
- Seguridad residual: JwtAuthenticationFilter → 401 con WWW-Authenticate para token inválido/expirado; JwtService expiración a config (jwt.expiration); BlackListFilter + job de limpieza de tokens vencidos (o TTL) — Redis solo si el volumen lo exige.
- DataInitializer: idempotente (0.5) + README.md ya presente — documentar seed de prod.
- Dead code: NotificationTemplate, ExaminerEvaluation (si Fase 2.5 la confirma muerta), userHasStudentRole, imports sin usar (P en DataInitializer, ProgramAuthorityRepository), StudentService carpeta con lastName duplicado (bug 0.4).
- OpenAPI: corregir SwaggerConfig (auth en /auth/\*\*); documentar códigos de error del advice.
- Auditoría de deuda: grep final de patrones prohibidos (0 ResponseEntity en services, 0 jakarta.transaction, 0 @CrossOrigin("\*"), 0 catch(Exception) en controllers, 0 RuntimeException con mensaje directo al cliente).



PRIORIZACIÓN CONSOLIDADA

Prioridad	Ítems	Justificación

Crítica	0.1 (Arrays), 0.2 (Test endpoint, CORS, ownership, reset link), 0.3 (distinción=0, métricas falsas, filtros ignorados)	Rompen runtime, exponen datos o reportan información falsa

Alta	0.4 (eventos AFTER\_COMMIT), 1.1-1.7 (bases transversales), Fase 2 completa	Bloquean todo refactor posterior; la pérdida silenciosa de notificaciones es el peor síntoma de producción

Recomendada	Fase 3 (consultas), Fase 4 (god classes), Fase 5 (PDF), 0.5 (seed/EnvLoader)	Deuda estructural que degrada mantenibilidad y escalabilidad, sin falla funcional inmediata

Opcional	Fase 6 (deps, blacklist Redis, OpenAPI), paginación de listados, caché	Mejoras de calidad sin riesgo; deciden recursos disponibles



RESUMEN DE EJECUCIÓN

Fase	Alcance	Esfuerzo	Riesgo	Entregable verificable

0	10 bugs + 4 seguridad	M	Bajo (cambios puntuales)	Compile + smoke; baseline commit

1	common, advice, 124 firmas, validation, tx, eventos	L	Medio (advice cambia códigos; eventos cambian timing)	Compile + smoke flujo completo; contrato JSON idéntico

2	6 módulos	XL	Medio (lotes mecánicos con compile por módulo)	Compile por módulo + smoke por endpoint tocado

3	Queries report	M	Medio (cambio de consultas = cambio de comportamiento esperado, verificar datos)	# queries por request ↓80%, números idénticos

4	6 god classes	L	Medio (splits atómicos)	Compile + smoke por split

5	7 generators	M	Bajo (solo estructura)	PDFs byte-compatibles en contenido

6	Deps, limpieza	M	Bajo	Jar más chico; greps de patrones prohibidos = 0

Total estimado: 8-12 sesiones de trabajo. El orden garantiza que ningún archivo se toca dos veces salvo lo estrictamente necesario.
