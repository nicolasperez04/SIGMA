package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;

import java.util.Arrays;
import java.util.List;

/**
 * Utilidades compartidas para los servicios de reportes
 */
public class ReportUtils {

    private static final List<ModalityProcessStatus> ACTIVE_STATUSES = Arrays.asList(
            ModalityProcessStatus.MODALITY_SELECTED,
            ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD,
            ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD,
            ModalityProcessStatus.CORRECTIONS_SUBMITTED,
            ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD,
            ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_COMMITTEE,
            ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS,
            ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT,
            ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.PROPOSAL_APPROVED,
            ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR,
            ModalityProcessStatus.DEFENSE_SCHEDULED,
            ModalityProcessStatus.EXAMINERS_ASSIGNED,
            ModalityProcessStatus.READY_FOR_EXAMINERS,
            ModalityProcessStatus.DOCUMENT_REVIEW_TIEBREAKER_REQUIRED,
            ModalityProcessStatus.READY_FOR_DEFENSE,
            ModalityProcessStatus.FINAL_REVIEW_COMPLETED,
            ModalityProcessStatus.DEFENSE_COMPLETED,
            ModalityProcessStatus.UNDER_EVALUATION_PRIMARY_EXAMINERS,
            ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER,
            ModalityProcessStatus.UNDER_EVALUATION_TIEBREAKER,
            ModalityProcessStatus.EVALUATION_COMPLETED);

    public static List<ModalityProcessStatus> getActiveStatuses() {
        return ACTIVE_STATUSES;
    }

    public static String describeModalityStatus(ModalityProcessStatus status) {
        return switch (status) {
            // Estados de selección y revisión inicial
            case MODALITY_SELECTED -> "Modalidad Seleccionada";
            case UNDER_REVIEW_PROGRAM_HEAD -> "En Revisión - Jefe de Programa";
            case CORRECTIONS_REQUESTED_PROGRAM_HEAD -> "Correcciones Solicitadas - Jefe";
            case CORRECTIONS_SUBMITTED -> "Correcciones Entregadas";
            case CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD -> "Correcciones Entregadas - Jefatura de Programa y/o coordinación de modalidad";
            case CORRECTIONS_SUBMITTED_TO_COMMITTEE -> "Correcciones Entregadas - Comité";
            case CORRECTIONS_SUBMITTED_TO_EXAMINERS -> "Correcciones Entregadas - Jurados";
            case CORRECTIONS_APPROVED -> "Correcciones Aprobadas";
            case CORRECTIONS_REJECTED_FINAL -> "Correcciones Rechazadas (Final)";

            // Estados de revisión de comité
            case READY_FOR_PROGRAM_CURRICULUM_COMMITTEE -> "Listo para Comité";
            case UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE -> "En Revisión - Comité";
            case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones Solicitadas - Comité";
            case READY_FOR_DIRECTOR_ASSIGNMENT -> "Listo para Asignar Director";
            case READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Listo para Aprobación por Comité";
            case APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Aprobado por Comité";
            case PROPOSAL_APPROVED -> "Propuesta Aprobada";

            // Estados de revisión final por jefatura (paso intermedio antes de jurados)
            case PENDING_PROGRAM_HEAD_FINAL_REVIEW -> "Pendiente Revisión Final - Jefatura";
            case APPROVED_BY_PROGRAM_HEAD_FINAL_REVIEW -> "Aprobado por Jefatura - Notificando Jurados";

            // Estados de programación de sustentación
            case DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR -> "Sustentación Solicitada";
            case DEFENSE_SCHEDULED -> "Sustentación Programada";
            case EXAMINERS_ASSIGNED -> "Jurados Asignados";
            case READY_FOR_EXAMINERS -> "Listo para Revisión por Jurados";
            case CORRECTIONS_REQUESTED_EXAMINERS -> "Correcciones Solicitadas - Jurados";
            case READY_FOR_DEFENSE -> "Listo para Sustentar";
            case DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos Aprobados por Jurados";
            case SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos Finales Aprobados por Jurados";
            case DOCUMENT_REVIEW_TIEBREAKER_REQUIRED -> "Requiere Jurado de Desempate (Documento)";
            case FINAL_REVIEW_COMPLETED -> "Revisión Final Completada";
            case EDIT_REQUESTED_BY_STUDENT -> "Solicitud de Edición por Estudiante";
            case SEMINAR_CANCELED -> "Seminario Cancelado";

            // Estados de sustentación y evaluación
            case DEFENSE_COMPLETED -> "Sustentación Completada";
            case UNDER_EVALUATION_PRIMARY_EXAMINERS -> "En Evaluación - Jurados Principales";
            case DISAGREEMENT_REQUIRES_TIEBREAKER -> "Requiere Jurado de Desempate";
            case UNDER_EVALUATION_TIEBREAKER -> "En Evaluación - Desempate";
            case EVALUATION_COMPLETED -> "Evaluación Completada";

            // Estados finales de resultado
            case PENDING_DISTINCTION_COMMITTEE_REVIEW -> "Aprobado – Distinción Honorífica Pendiente del Comité";
            case GRADED_APPROVED -> "Aprobado";
            case GRADED_FAILED -> "Reprobado";
            case MODALITY_CLOSED -> "Modalidad Cerrada";

            // Estados de cancelación
            case MODALITY_CANCELLED -> "Cancelado";
            case CANCELLATION_REQUESTED -> "Cancelación Solicitada";
            case CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR -> "Cancelación Aprobada por Director";
            case CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR -> "Cancelación Rechazada por Director";
            case CANCELLED_WITHOUT_REPROVAL -> "Cancelado sin Reprobación";
            case CANCELLATION_REJECTED -> "Cancelación Rechazada";
            case CANCELLED_BY_CORRECTION_TIMEOUT -> "Cancelado por Tiempo Expirado";

            default -> status.name();
        };
    }

    public static boolean isPendingStatus(ModalityProcessStatus status) {
        return status == ModalityProcessStatus.MODALITY_SELECTED ||
               status == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD ||
               status == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE ||
               status == ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS ||
               status == ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE ||
               status == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD ||
               status == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE ||
               status == ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR ||
               status == ModalityProcessStatus.EDIT_REQUESTED_BY_STUDENT;
    }

    public static boolean isAdvancedStatus(ModalityProcessStatus status) {
        return status == ModalityProcessStatus.PROPOSAL_APPROVED ||
               status == ModalityProcessStatus.DEFENSE_SCHEDULED ||
               status == ModalityProcessStatus.EXAMINERS_ASSIGNED ||
               status == ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR ||
               status == ModalityProcessStatus.READY_FOR_DEFENSE ||
               status == ModalityProcessStatus.FINAL_REVIEW_COMPLETED ||
               status == ModalityProcessStatus.DEFENSE_COMPLETED ||
               status == ModalityProcessStatus.UNDER_EVALUATION_PRIMARY_EXAMINERS ||
               status == ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER ||
               status == ModalityProcessStatus.UNDER_EVALUATION_TIEBREAKER ||
               status == ModalityProcessStatus.EVALUATION_COMPLETED;
    }
}

