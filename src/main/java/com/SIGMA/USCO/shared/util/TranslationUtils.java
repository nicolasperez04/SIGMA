package com.SIGMA.USCO.shared.util;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.ExaminerType;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TranslationUtils {

    private TranslationUtils() {}

    public static String translateModalityProcessStatus(ModalityProcessStatus status) {
        if (status == null) return "N/A";
        return switch (status) {
            case MODALITY_SELECTED -> "Modalidad seleccionada";
            case UNDER_REVIEW_PROGRAM_HEAD -> "En revisión por Jefatura de programa y/o coordinación de modalidades";
            case CORRECTIONS_REQUESTED_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura";
            case CORRECTIONS_SUBMITTED -> "Correcciones enviadas";
            case CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD -> "Correcciones enviadas a Jefatura de Programa y/o coordinación de modalidades";
            case CORRECTIONS_SUBMITTED_TO_COMMITTEE -> "Correcciones enviadas al Comité de Currículo";
            case CORRECTIONS_SUBMITTED_TO_EXAMINERS -> "Correcciones enviadas a los Jurados";
            case CORRECTIONS_APPROVED -> "Correcciones aprobadas";
            case CORRECTIONS_REJECTED_FINAL -> "Correcciones rechazadas (final)";
            case READY_FOR_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para Comité de Currículo";
            case UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE -> "En revisión por Comité de Currículo";
            case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité de Currículo";
            case READY_FOR_DIRECTOR_ASSIGNMENT -> "Lista para asignación de Director de Proyecto";
            case READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para aprobación por Comité de Currículo";
            case APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Aprobado por Comité de Currículo";
            case PROPOSAL_APPROVED -> "Propuesta aprobada";
            case PENDING_PROGRAM_HEAD_FINAL_REVIEW -> "Pendiente revisión final por Jefatura de Programa";
            case APPROVED_BY_PROGRAM_HEAD_FINAL_REVIEW -> "Documentos finales aprobados por Jefatura de Programa";
            case DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR -> "Sustentación solicitada por Director";
            case DEFENSE_SCHEDULED -> "Sustentación programada";
            case EXAMINERS_ASSIGNED -> "Jurados asignados";
            case READY_FOR_EXAMINERS -> "Lista para Jurados";
            case DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos de propuesta aprobados por los jurados";
            case SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos finales aprobados por los jurados";
            case DOCUMENT_REVIEW_TIEBREAKER_REQUIRED -> "Revisión de documentos con desempate requerida";
            case EDIT_REQUESTED_BY_STUDENT -> "Edición de documento solicitado por estudiante";
            case CORRECTIONS_REQUESTED_EXAMINERS -> "Correcciones solicitadas por Jurados";
            case READY_FOR_DEFENSE -> "Lista para sustentación";
            case FINAL_REVIEW_COMPLETED -> "Revisión final completada";
            case DEFENSE_COMPLETED -> "Sustentación realizada";
            case UNDER_EVALUATION_PRIMARY_EXAMINERS -> "En evaluación por jurados principales";
            case DISAGREEMENT_REQUIRES_TIEBREAKER -> "Desacuerdo, requiere desempate";
            case UNDER_EVALUATION_TIEBREAKER -> "En evaluación por jurado de desempate";
            case EVALUATION_COMPLETED -> "Evaluación completada";
            case PENDING_DISTINCTION_COMMITTEE_REVIEW -> "Aprobado - Distinción honorífica pendiente de revisión por el Comité";
            case GRADED_APPROVED -> "Aprobado";
            case GRADED_FAILED -> "Reprobado";
            case MODALITY_CLOSED -> "Modalidad cerrada";
            case SEMINAR_CANCELED -> "Seminario cancelado";
            case MODALITY_CANCELLED -> "Modalidad cancelada";
            case CANCELLATION_REQUESTED -> "Cancelación solicitada";
            case CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR -> "Cancelación aprobada por Director";
            case CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR -> "Cancelación rechazada por Director";
            case CANCELLED_WITHOUT_REPROVAL -> "Cancelada sin reprobación";
            case CANCELLATION_REJECTED -> "Cancelación rechazada";
            case CANCELLED_BY_CORRECTION_TIMEOUT -> "Cancelada por tiempo de corrección";
        };
    }

    public static String translateAcademicDistinction(AcademicDistinction distinction) {
        if (distinction == null) return "Ninguna";
        return switch (distinction) {
            case NO_DISTINCTION -> "Sin distinción";
            case AGREED_APPROVED -> "Aprobado";
            case AGREED_MERITORIOUS -> "Meritorio";
            case AGREED_LAUREATE -> "Laureado";
            case AGREED_REJECTED -> "Reprobado";
            case DISAGREEMENT_PENDING_TIEBREAKER -> "Desacuerdo, pendiente desempate";
            case TIEBREAKER_APPROVED -> "Aprobado por desempate";
            case TIEBREAKER_MERITORIOUS -> "Meritorio por desempate";
            case TIEBREAKER_LAUREATE -> "Laureado por desempate";
            case TIEBREAKER_REJECTED -> "Reprobado por desempate";
            case REJECTED_BY_COMMITTEE -> "Rechazado por comité";
            case PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria propuesta (pendiente del comité)";
            case PENDING_COMMITTEE_LAUREATE -> "Mención Laureada propuesta (pendiente del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS -> "Mención Meritoria por desempate (pendiente del comité)";
            case TIEBREAKER_PENDING_COMMITTEE_LAUREATE -> "Mención Laureada por desempate (pendiente del comité)";
        };
    }

    public static String translateDocumentStatus(DocumentStatus status) {
        if (status == null) return "N/A";
        return switch (status) {
            case PENDING -> "Pendiente";
            case ACCEPTED_FOR_PROGRAM_HEAD_REVIEW -> "Aceptado por Jefatura de Programa";
            case REJECTED_FOR_PROGRAM_HEAD_REVIEW -> "Rechazado por Jefatura de Programa";
            case CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura de Programa";
            case CORRECTION_RESUBMITTED -> "Corrección reenviada";
            case ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW -> "Aceptado por Comité de Currículo";
            case REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW -> "Rechazado por Comité de Currículo";
            case CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité de Currículo";
            case ACCEPTED_FOR_EXAMINER_REVIEW -> "Aceptado por revisión de Jurado";
            case REJECTED_FOR_EXAMINER_REVIEW -> "Rechazado por Jurado";
            case CORRECTIONS_REQUESTED_BY_EXAMINER -> "Correcciones solicitadas por Jurado";
            case EDIT_REQUESTED -> "Solicitud de edición pendiente de revisión por Jurado";
            case EDIT_REQUEST_APPROVED -> "Solicitud de edición aprobada por Jurado (el estudiante puede resubir el documento)";
            case EDIT_REQUEST_REJECTED -> "Solicitud de edición rechazada por Jurado";
        };
    }

    public static String translateExaminerType(ExaminerType type) {
        if (type == null) return "Jurado";
        return switch (type) {
            case PRIMARY_EXAMINER_1 -> "Jurado Principal 1";
            case PRIMARY_EXAMINER_2 -> "Jurado Principal 2";
            case TIEBREAKER_EXAMINER -> "Jurado de Desempate";
        };
    }

    public static String getStudentList(StudentModality modality) {
        return getStudentList(modality, true);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a", Locale.ENGLISH)
                .format(dateTime).toLowerCase();
    }

    public static String getStudentList(StudentModality modality, boolean includeEmail) {
        if (modality.getMembers() == null || modality.getMembers().isEmpty()) {
            return "Sin estudiantes asociados";
        }
        return modality.getMembers().stream()
            .map(m -> includeEmail
                    ? m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")"
                    : m.getStudent().getName() + " " + m.getStudent().getLastName())
            .collect(Collectors.joining(", "));
    }

    public static String localizeObservations(String observations) {
        if (observations == null) return "Ninguna";

        Map<String, String> translations = Map.ofEntries(
            Map.entry("PRIMARY_EXAMINER_1", "Jurado Principal 1"),
            Map.entry("PRIMARY_EXAMINER_2", "Jurado Principal 2"),
            Map.entry("TIEBREAKER_EXAMINER", "Jurado de Desempate"),
            Map.entry("PENDING_COMMITTEE_MERITORIOUS", "Mención Meritoria propuesta (pendiente del comité)"),
            Map.entry("PENDING_COMMITTEE_LAUREATE", "Mención Laureada propuesta (pendiente del comité)"),
            Map.entry("TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS", "Mención Meritoria por desempate (pendiente del comité)"),
            Map.entry("TIEBREAKER_PENDING_COMMITTEE_LAUREATE", "Mención Laureada por desempate (pendiente del comité)"),
            Map.entry("NO_DISTINCTION", "Sin distinción"),
            Map.entry("AGREED_APPROVED", "Aprobado"),
            Map.entry("AGREED_MERITORIOUS", "Meritorio"),
            Map.entry("AGREED_LAUREATE", "Laureado"),
            Map.entry("AGREED_REJECTED", "Reprobado"),
            Map.entry("DISAGREEMENT_PENDING_TIEBREAKER", "Desacuerdo, pendiente desempate"),
            Map.entry("TIEBREAKER_APPROVED", "Aprobado por desempate"),
            Map.entry("TIEBREAKER_MERITORIOUS", "Meritorio por desempate"),
            Map.entry("TIEBREAKER_LAUREATE", "Laureado por desempate"),
            Map.entry("TIEBREAKER_REJECTED", "Reprobado por desempate"),
            Map.entry("REJECTED_BY_COMMITTEE", "Rechazado por comité")
        );

        // Sustitución en UN solo pase con regex alternado ordenado por longitud
        // DESCENDENTE (el token más largo gana): evita el bug de prefijos cuando un
        // token es prefijo de otro en el texto de observaciones (p.ej. AGREED_APPROVED
        // vs AGREED_APPROVED_X), que el replace() secuencial por orden no garantizaba.
        String tokens = translations.keySet().stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        Matcher matcher = Pattern.compile(tokens).matcher(observations);
        String result = matcher.replaceAll(m -> translations.get(m.group()));

        if (result.contains("Distinción propuesta:") && result.contains("Distinción confirmada:")) {
            try {
                String regex = "Distinción propuesta: ([A-Z_]+) → Distinción confirmada: ([A-Z_]+)";
                Matcher dMatcher = Pattern.compile(regex).matcher(result);
                if (dMatcher.find()) {
                    String propuesta = dMatcher.group(1);
                    String confirmada = dMatcher.group(2);
                    AcademicDistinction propuestaEnum = null;
                    AcademicDistinction confirmadaEnum = null;
                    try {
                        propuestaEnum = AcademicDistinction.valueOf(propuesta);
                        confirmadaEnum = AcademicDistinction.valueOf(confirmada);
                    } catch (IllegalArgumentException ignored) {
                    }

                    String propuestaLabel = propuestaEnum != null ? translateAcademicDistinction(propuestaEnum) : propuesta;
                    String confirmadaLabel = confirmadaEnum != null ? translateAcademicDistinction(confirmadaEnum) : confirmada;
                    result = result.replace(
                        "Distinción propuesta: " + propuesta + " → Distinción confirmada: " + confirmada,
                        "Distinción propuesta: " + propuestaLabel + " → Distinción confirmada: " + confirmadaLabel
                    );
                }
            } catch (IllegalArgumentException e) {
                // Ignorar error de formateo
            }
        }

        return result;
    }

    /**
     * Nombre de carpeta por estudiante para archivos subidos: {name}_{lastName}_{id}.
     * Reemplaza el patrón pre-fase 6 que duplicaba el lastName y omitía el separador entre name y lastName.
     * La escritura siempre genera esta carpeta; la lectura usa la ruta absoluta almacenada en la BD,
     * por lo que los archivos previos (patrón antiguo) siguen siendo legibles.
     */
    public static String studentFolder(String name, String lastName, Long id) {
        return (name + "_" + lastName + "_" + id).replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Sanea un nombre de archivo para usarlo como segmento de ruta.
     * Permite letras, dígitos, punto, guion y guion bajo.
     */
    public static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Overload con regex personalizado: para sitios que también deben quitar puntos
     * (p.ej. nombres de carpeta de modalidad usan "[^a-zA-Z0-9]").
     */
    public static String sanitizeFileName(String name, String regex) {
        return name.replaceAll(regex, "_");
    }
}