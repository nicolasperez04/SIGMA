package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.ModalityRequirements;
import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.entity.enums.RuleType;
import com.SIGMA.USCO.Modalities.dto.ValidationItemDTO;
import com.SIGMA.USCO.Modalities.dto.response.FinalEvaluationInfo;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.common.exception.RequirementsValidationException;
import com.SIGMA.USCO.documents.entity.FinalDocumentEvaluation;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.ExaminerDocumentDecision;
import com.SIGMA.USCO.documents.entity.enums.FinalDocumentRubricType;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ModalityServiceUtils {

    private ModalityServiceUtils() {
    }

    public static final String ENTREPRENEURSHIP_MODALITY_NAME = "Emprendimiento y fortalecimiento de empresa";

    public static String describeModalityStatus(ModalityProcessStatus status) {
        return switch (status) {
            case MODALITY_SELECTED ->
                    "Haz seleccionado una modalidad de grado. Debes cargar los documentos requeridos para esta modalidad.";
            case UNDER_REVIEW_PROGRAM_HEAD ->
                    "La jefatura del programa y/o coordinación de modalidades está revisando la modalidad de grado. Asegúrate de que todos los documentos obligatorios estén cargados.";
            case CORRECTIONS_REQUESTED_PROGRAM_HEAD ->
                    "La jefatura del programa y/o coordinación de modalidades solicitó correcciones. Debes ajustar la información requerida.";
            case CORRECTIONS_SUBMITTED ->
                    "Las correcciones solicitadas han sido enviadas. Pendiente de aprobación o nuevas observaciones.";
            case CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD ->
                    "Las correcciones han sido enviadas a la Jefatura de Programa y/o coordinador de modalidades para su revisión.";
            case CORRECTIONS_SUBMITTED_TO_COMMITTEE ->
                    "Las correcciones han sido enviadas al Comité de Currículo de Programa para su revisión.";
            case CORRECTIONS_SUBMITTED_TO_EXAMINERS ->
                    "Las correcciones han sido enviadas a los Jurados evaluadores para su revisión.";
            case CORRECTIONS_APPROVED ->
                    "Las correcciones enviadas han sido aprobadas por la jefatura del programa. El proceso continúa con la siguiente etapa.";
            case CORRECTIONS_REJECTED_FINAL ->
                    "Uno o más documentos no fueron aprobados y/o agotaste el límite de intentos (3). El proceso ha sido cerrado o cancelado.";
            case READY_FOR_PROGRAM_CURRICULUM_COMMITTEE ->
                    "La jefatura de programa y/o cordinación de modalidades aprobó la modalidad de grado. Está pendiente de revisión por el comité de currículo de programa.";
            case UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE ->
                    "El comité de currículo de programa está revisando la modalidad de grado.";
            case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE ->
                    "El comité de currículo de programa solicitó correcciones. Debes ajustar la información requerida.";
            case READY_FOR_DIRECTOR_ASSIGNMENT ->
                    "Todos los documentos obligatorios han sido aprobados por el Comité de Currículo. El comité procederá a asignar el Director de Proyecto (si la modalidad lo requiere).";
            case READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE ->
                    "El Director de Proyecto ha sido asignado. El comité de currículo puede proceder con la aprobación formal de la propuesta.";
            case APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE ->
                    "EL comité de currículo de programa ha aprobado los documentos iniciales, por lo que la modalidad avanza a la siguiente etapa del proceso. El estudiante puede continuar con el proceso de la modalidad de grado.";
            case PROPOSAL_APPROVED ->
                    "La modalidad fue aprobada por el comité de currículo de programa y los jurados asignados. Por favor, continua con el desarrollo normal de tu modalidad de grado.";
            case PENDING_PROGRAM_HEAD_FINAL_REVIEW ->
                    "El director de proyecto notificó a jefatura de programa que los documentos finales están listos. Jefatura revisará los documentos antes de notificar a los jurados.";
            case APPROVED_BY_PROGRAM_HEAD_FINAL_REVIEW ->
                    "Jefatura de programa aprobó los documentos finales. Próximamente los jurados serán notificados para revisión de la sustentación.";
            case DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR ->
                    "El director de proyecto ha propuesto fecha y lugar de sustentación. Pendiente de confirmación...";
            case DEFENSE_SCHEDULED ->
                    "La sustentación ha sido programada por el director de proyecto";
            case EXAMINERS_ASSIGNED ->
                    "Los jurados han sido asignados a la modalidad. Próximo paso: revisión de documentos por parte de los jurados.";
            case READY_FOR_EXAMINERS ->
                    "La modalidad está lista para asignar a los jurados asignados. Próximo paso: Revisión de documentos por parte de los jurados.";
            case DOCUMENTS_APPROVED_BY_EXAMINERS ->
                    "Todos los documentos obligatorios de la propuesta han sido aprobados por los jurados. La modalidad avanza a aprobación de propuesta.";
            case SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS ->
                    "Todos los documentos finales han sido aprobados por los jurados. La modalidad avanza a revisión final completada.";
            case DOCUMENT_REVIEW_TIEBREAKER_REQUIRED ->
                    "Los jurados principales tienen decisiones divididas sobre un documento. Se requiere un jurado de desempate para resolver el conflicto.";
            case EDIT_REQUESTED_BY_STUDENT ->
                    "Has solicitado la edición de un documento previamente aprobado. Los jurados evaluadores están evaluando la solicitud. Recibirás una notificación con el resultado.";
            case CORRECTIONS_REQUESTED_EXAMINERS ->
                    "Uno o más jurados solicitaron correcciones en la documentación. Por favor, ajusta los documentos según las observaciones recibidas.";
            case READY_FOR_DEFENSE ->
                    "Jefatura de programa y/o el coordinador de modalidades ha marcado la modalidad como lista para sustentar. Esperando que los jurados designados, revisen los documentos finales y den su aprobación.";
            case FINAL_REVIEW_COMPLETED ->
                    "La revisión final de los jurados ha sido completada. Próximo paso: Director de proyecto programa la sustentación.";
            case DEFENSE_COMPLETED ->
                    "La sustentación se ha completado. Pendiente de calificación final.";
            case UNDER_EVALUATION_PRIMARY_EXAMINERS ->
                    "Los jurados principales están evaluando la sustentación. Cada jurado registra su calificación y decisión de forma independiente.";
            case DISAGREEMENT_REQUIRES_TIEBREAKER ->
                    "Los jurados principales no llegaron a un acuerdo. Se requiere asignar un tercer jurado (desempate).";
            case UNDER_EVALUATION_TIEBREAKER ->
                    "El jurado de desempate está evaluando la sustentación. Su decisión será definitiva.";
            case EVALUATION_COMPLETED ->
                    "La evaluación de la sustentación ha sido completada por los jurados. Próximo paso: resultado final.";
            case PENDING_DISTINCTION_COMMITTEE_REVIEW ->
                    "La modalidad ha sido APROBADA en calificación. Los jurados han propuesto una distinción honorífica (Meritoria o Laureada). El Comité de Currículo debe revisar y decidir si acepta o rechaza la distinción propuesta.";
            case GRADED_APPROVED ->
                    "¡Felicitaciones! Tu modalidad de grado ha sido aprobada.";
            case GRADED_FAILED ->
                    "La modalidad de grado no fue aprobada.";
            case MODALITY_CLOSED ->
                    "La modalidad fue cerrada.";
            case SEMINAR_CANCELED ->
                    "El seminario asociado a la modalidad fue cancelado por la jefatura o el comité correspondiente.";
            case MODALITY_CANCELLED ->
                    "La modalidad fue cancelada.";
            case CANCELLATION_REQUESTED ->
                    "Solicitud de cancelación enviada. Pendiente de revisión.";
            case CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR ->
                    "La solicitud de cancelación fue aprobada por el director de proyecto. Pendiente de revisión por el comité de currículo.";
            case CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR ->
                    "La solicitud de cancelación fue rechazada por el director de proyecto.";
            case CANCELLED_WITHOUT_REPROVAL ->
                    "La modalidad fue cancelada sin reprobación.";
            case CANCELLATION_REJECTED ->
                    "La solicitud de cancelación fue rechazada por el comité de currículo.";
            case CANCELLED_BY_CORRECTION_TIMEOUT ->
                    "La modalidad fue cancelada automáticamente por no entregar las correcciones en el plazo establecido.";
            default ->
                    "Estado del proceso no definido.";
        };
    }

    public static String describeDocumentStatus(DocumentStatus status) {
        return switch (status) {
            case PENDING ->
                    "El documento ha sido cargado y está pendiente de revisión.";
            case ACCEPTED_FOR_PROGRAM_HEAD_REVIEW ->
                    "El documento fue aceptado para revisión por la jefatura del programa.";
            case REJECTED_FOR_PROGRAM_HEAD_REVIEW ->
                    "El documento fue rechazado por la jefatura del programa. Revisa las observaciones.";
            case CORRECTIONS_REQUESTED_BY_PROGRAM_HEAD ->
                    "La jefatura del programa solicitó correcciones. Revisa las observaciones y carga una nueva versión.";
            case CORRECTION_RESUBMITTED ->
                    "La corrección ha sido reenviada y está pendiente de revisión.";
            case ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW ->
                    "El documento fue aceptado para revisión por el comité de currículo del programa.";
            case REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW ->
                    "El documento fue rechazado por el comité de currículo del programa. Revisa las observaciones.";
            case CORRECTIONS_REQUESTED_BY_PROGRAM_CURRICULUM_COMMITTEE ->
                    "El comité de currículo del programa solicitó correcciones. Revisa las observaciones y carga una nueva versión.";
            case ACCEPTED_FOR_EXAMINER_REVIEW ->
                    "El documento fue aceptado para revisión por los jurados evaluadores.";
            case REJECTED_FOR_EXAMINER_REVIEW ->
                    "El documento fue rechazado por los jurados evaluadores. Revisa las observaciones.";
            case CORRECTIONS_REQUESTED_BY_EXAMINER ->
                    "Los jurados evaluadores solicitaron correcciones. Revisa las observaciones y carga una nueva versión.";
            case EDIT_REQUESTED ->
                    "Se ha solicitado la edición del documento. La solicitud está pendiente de revisión por los jurados evaluadores.";
            case EDIT_REQUEST_APPROVED ->
                    "La solicitud de edición fue aprobada por los jurados evaluadores. Puedes cargar una nueva versión del documento.";
            case EDIT_REQUEST_REJECTED ->
                    "La solicitud de edición fue rechazada por los jurados evaluadores.";
            default ->
                    "Estado del documento no definido.";
        };
    }

    /**
     * Valida los requisitos académicos numéricos (crédito/promedio) de una modalidad.
     * Lanza ValidationException si alguno no se cumple; si no, devuelve los ítems de validación.
     */
    public static List<ValidationItemDTO> validateNumericRequirements(StudentProfile profile,
                                                                      List<ModalityRequirements> requirements,
                                                                      String failureMessage) {
        List<ValidationItemDTO> results = new ArrayList<>();
        boolean allValid = true;

        for (ModalityRequirements req : requirements) {

            if (req.getRuleType() != RuleType.NUMERIC) {
                continue;
            }

            boolean fulfilled = true;
            String studentValue = "";

            if (req.getRequirementName().toLowerCase().contains("crédito")) {
                double percentageRequired = Double.parseDouble(req.getExpectedValue());
                long totalCredits = profile.getAcademicProgram().getTotalCredits();
                long requiredCredits = Math.round(totalCredits * percentageRequired);

                fulfilled = profile.getApprovedCredits() >= requiredCredits;
                studentValue = profile.getApprovedCredits() + " / " + requiredCredits;
            }

            if (req.getRequirementName().toLowerCase().contains("promedio")) {
                fulfilled = profile.getGpa() >= Double.parseDouble(req.getExpectedValue());
                studentValue = String.valueOf(profile.getGpa());
            }

            results.add(
                    ValidationItemDTO.builder()
                            .requirementName(req.getRequirementName())
                            .requiredValue(req.getExpectedValue())
                            .studentValue(studentValue)
                            .fulfilled(fulfilled)
                            .build()
            );

            if (!fulfilled) {
                allValid = false;
            }
        }

        if (!allValid) {
            throw new RequirementsValidationException(failureMessage, results);
        }

        return results;
    }

    public static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /**
     * Traduce el nombre interno de la distinción a una etiqueta legible en español.
     */
    public static String translateProposedDistinction(AcademicDistinction distinction) {
        if (distinction == null) return "Sin distinción";
        return switch (distinction) {
            case PENDING_COMMITTEE_MERITORIOUS, AGREED_MERITORIOUS -> "Meritoria";
            case PENDING_COMMITTEE_LAUREATE, AGREED_LAUREATE -> "Laureada";
            case TIEBREAKER_PENDING_COMMITTEE_MERITORIOUS, TIEBREAKER_MERITORIOUS -> "Meritoria (desempate)";
            case TIEBREAKER_PENDING_COMMITTEE_LAUREATE, TIEBREAKER_LAUREATE -> "Laureada (desempate)";
            case AGREED_APPROVED -> "Aprobada";
            case TIEBREAKER_APPROVED -> "Aprobada (desempate)";
            case AGREED_REJECTED, TIEBREAKER_REJECTED -> "Reprobada";
            case REJECTED_BY_COMMITTEE -> "Rechazada por el comité";
            default -> distinction.name();
        };
    }

    /**
     * Traduce el enum ExaminerDocumentDecision al español para mejor legibilidad.
     */
    public static String translateExaminerDocumentDecision(ExaminerDocumentDecision decision) {
        if (decision == null) return "Sin decisión";
        return switch (decision) {
            case ACCEPTED -> "Aprobado";
            case REJECTED -> "Rechazado";
            case CORRECTIONS_REQUESTED -> "Correcciones Solicitadas";
        };
    }

    /**
     * Construye la información de una evaluación de documento final.
     */
    public static FinalEvaluationInfo buildFinalEvaluationInfo(FinalDocumentEvaluation evaluation) {
        FinalDocumentRubricType rubricType = evaluation.getRubricType() != null
                ? evaluation.getRubricType()
                : FinalDocumentRubricType.STANDARD;

        FinalEvaluationInfo.FinalEvaluationInfoBuilder builder = FinalEvaluationInfo.builder()
                .id(evaluation.getId())
                .rubricType(rubricType.name())
                .summary(evaluation.getSummary())
                .introduction(evaluation.getIntroduction())
                .materialsAndMethods(evaluation.getMaterialsAndMethods())
                .resultsAndDiscussion(evaluation.getResultsAndDiscussion())
                .conclusions(evaluation.getConclusions())
                .bibliographyReferences(evaluation.getBibliographyReferences())
                .documentOrganization(evaluation.getDocumentOrganization())
                .prototypeOrSoftware(evaluation.getPrototypeOrSoftware())
                .evaluatedAt(evaluation.getEvaluatedAt());

        if (rubricType == FinalDocumentRubricType.PROFESSIONAL_PRACTICE) {
            builder.generalObjective(evaluation.getGeneralObjective())
                    .activitiesObjectiveCoherence(evaluation.getActivitiesObjectiveCoherence())
                    .criticalActivitiesDescription(evaluation.getCriticalActivitiesDescription())
                    .practiceComplianceEvidence(evaluation.getPracticeComplianceEvidence())
                    .organizationAndWriting(evaluation.getOrganizationAndWriting());
        }

        return builder.build();
    }
}
