package com.SIGMA.USCO.common.security;

/**
 * Permisos de autorización del sistema.
 *
 * El valor es la autoridad EXACTA que {@code User} concede vía
 * {@code new SimpleGrantedAuthority("PERM_" + permission.getName())}
 * y la que verifican las expresiones {@code hasAuthority(...)}.
 * NO incluye el nombre base sembrado en BD (sin el prefijo PERM_);
 * el seeder (DataInitializer, dev) lo deriva quitando el prefijo.
 */
public final class Permissions {

    private Permissions() {
    }

    // ===== Modalidades =====
    public static final String PERM_CREATE_MODALITY = "PERM_CREATE_MODALITY";
    public static final String PERM_UPDATE_MODALITY = "PERM_UPDATE_MODALITY";
    public static final String PERM_DESACTIVE_MODALITY = "PERM_DESACTIVE_MODALITY";
    public static final String PERM_DELETE_MODALITY_REQUIREMENT = "PERM_DELETE_MODALITY_REQUIREMENT";
    public static final String PERM_VIEW_MODALITY = "PERM_VIEW_MODALITY";
    public static final String PERM_VIEW_ALL_MODALITIES = "PERM_VIEW_ALL_MODALITIES";
    public static final String PERM_APPROVE_MODALITY = "PERM_APPROVE_MODALITY";
    public static final String PERM_APPROVE_MODALITY_BY_COMMITTEE = "PERM_APPROVE_MODALITY_BY_COMMITTEE";
    public static final String PERM_REJECT_MODALITY_BY_COMMITTEE = "PERM_REJECT_MODALITY_BY_COMMITTEE";
    public static final String PERM_APPROVE_MODALITY_BY_EXAMINER = "PERM_APPROVE_MODALITY_BY_EXAMINER";
    public static final String PERM_VIEW_EXAMINER_MODALITIES = "PERM_VIEW_EXAMINER_MODALITIES";

    // ===== Documentos =====
    public static final String PERM_CREATE_REQUIRED_DOCUMENT = "PERM_CREATE_REQUIRED_DOCUMENT";
    public static final String PERM_UPDATE_REQUIRED_DOCUMENT = "PERM_UPDATE_REQUIRED_DOCUMENT";
    public static final String PERM_DELETE_REQUIRED_DOCUMENT = "PERM_DELETE_REQUIRED_DOCUMENT";
    public static final String PERM_VIEW_REQUIRED_DOCUMENT = "PERM_VIEW_REQUIRED_DOCUMENT";
    public static final String PERM_REVIEW_DOCUMENTS = "PERM_REVIEW_DOCUMENTS";
    public static final String PERM_VIEW_DOCUMENTS = "PERM_VIEW_DOCUMENTS";
    public static final String PERM_REVIEW_DOCUMENT_COMMITTEE = "PERM_REVIEW_DOCUMENT_COMMITTEE";

    // ===== Cancelaciones =====
    public static final String PERM_APPROVE_CANCELLATION = "PERM_APPROVE_CANCELLATION";
    public static final String PERM_APPROVE_CANCELLATION_DIRECTOR = "PERM_APPROVE_CANCELLATION_DIRECTOR";
    public static final String PERM_REJECT_CANCELLATION = "PERM_REJECT_CANCELLATION";
    public static final String PERM_VIEW_CANCELLATIONS = "PERM_VIEW_CANCELLATIONS";

    // ===== Director de proyecto =====
    public static final String PERM_ASSIGN_PROJECT_DIRECTOR = "PERM_ASSIGN_PROJECT_DIRECTOR";
    public static final String PERM_VIEW_PROJECT_DIRECTOR = "PERM_VIEW_PROJECT_DIRECTOR";

    // ===== Sustentación (defensa) =====
    public static final String PERM_PROPOSE_DEFENSE = "PERM_PROPOSE_DEFENSE";
    public static final String PERM_SCHEDULE_DEFENSE = "PERM_SCHEDULE_DEFENSE";
    public static final String PERM_EVALUATE_DEFENSE = "PERM_EVALUATE_DEFENSE";
    public static final String PERM_VIEW_FINAL_DEFENSE_RESULT = "PERM_VIEW_FINAL_DEFENSE_RESULT";

    // ===== Usuarios, roles y asignaciones =====
    public static final String PERM_CREATE_USER = "PERM_CREATE_USER";
    public static final String PERM_VIEW_USER = "PERM_VIEW_USER";
    public static final String PERM_ACTIVATE_OR_DEACTIVATE_USER = "PERM_ACTIVATE_OR_DEACTIVATE_USER";
    public static final String PERM_CREATE_ROLE = "PERM_CREATE_ROLE";
    public static final String PERM_UPDATE_ROLE = "PERM_UPDATE_ROLE";
    public static final String PERM_ASSIGN_ROLE = "PERM_ASSIGN_ROLE";
    public static final String PERM_VIEW_ROLE = "PERM_VIEW_ROLE";
    public static final String PERM_CREATE_PERMISSION = "PERM_CREATE_PERMISSION";
    public static final String PERM_VIEW_PERMISSION = "PERM_VIEW_PERMISSION";
    public static final String PERM_ASSIGN_EXAMINER = "PERM_ASSIGN_EXAMINER";
    public static final String PERM_ASSIGN_PROGRAM_HEAD = "PERM_ASSIGN_PROGRAM_HEAD";
    public static final String PERM_VIEW_EXAMINER = "PERM_VIEW_EXAMINER";
    public static final String PERM_VIEW_PROGRAM_HEAD = "PERM_VIEW_PROGRAM_HEAD";
    public static final String PERM_VIEW_COMMITTEE = "PERM_VIEW_COMMITTEE";
    public static final String PERM_VIEW_MODALITIES_ADMIN = "PERM_VIEW_MODALITIES_ADMIN";

    // ===== Académico (facultades, programas) =====
    public static final String PERM_CREATE_FACULTY = "PERM_CREATE_FACULTY";
    public static final String PERM_UPDATE_FACULTY = "PERM_UPDATE_FACULTY";
    public static final String PERM_DELETE_FACULTY = "PERM_DELETE_FACULTY";
    public static final String PERM_VIEW_FACULTIES = "PERM_VIEW_FACULTIES";
    public static final String PERM_CREATE_PROGRAM = "PERM_CREATE_PROGRAM";
    public static final String PERM_UPDATE_PROGRAM = "PERM_UPDATE_PROGRAM";
    public static final String PERM_VIEW_PROGRAMS = "PERM_VIEW_PROGRAMS";
    public static final String PERM_CREATE_PROGRAM_DEGREE_MODALITY = "PERM_CREATE_PROGRAM_DEGREE_MODALITY";
    public static final String PERM_UPDATE_PROGRAM_DEGREE_MODALITY = "PERM_UPDATE_PROGRAM_DEGREE_MODALITY";
    public static final String PERM_VIEW_PROGRAM_DEGREE_MODALITY = "PERM_VIEW_PROGRAM_DEGREE_MODALITY";

    // ===== Seminarios =====
    public static final String PERM_CREATE_SEMINAR = "PERM_CREATE_SEMINAR";

    // ===== Reportes =====
    public static final String PERM_VIEW_REPORT = "PERM_VIEW_REPORT";

    // ===== Otros =====
    public static final String PERM_STUDENT_LIST = "PERM_STUDENT_LIST";

    // ===== Estudiante (informativo; la autorización real es por ROLE_STUDENT, no por permiso) =====
    public static final String PERM_START_MODALITY = "PERM_START_MODALITY";
    public static final String PERM_UPLOAD_DOCUMENT = "PERM_UPLOAD_DOCUMENT";
    public static final String PERM_REQUEST_CANCELLATION = "PERM_REQUEST_CANCELLATION";
    public static final String PERM_REQUEST_EDIT = "PERM_REQUEST_EDIT";
    public static final String PERM_VIEW_RESULT = "PERM_VIEW_RESULT";
}