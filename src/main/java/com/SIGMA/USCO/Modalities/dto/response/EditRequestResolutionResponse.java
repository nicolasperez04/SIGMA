package com.SIGMA.USCO.Modalities.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de resolveDocumentEditRequest (y sus helpers privados de consenso).
 * Cubre las 3 formas del Map anterior; los campos con @NON_NULL solo se serializan
 * cuando la rama del flujo los produce:
 * - esperando 2do voto: {success, editRequestId, message, votesReceived, votesRequired}
 * - desempate requerido: {success, editRequestId, newStatus, message, votes}
 * - decisión final:       {success, editRequestId, documentId, documentName, finalStatus,
 *                          newDocumentStatus, newModalityStatus, resolvedByTiebreaker, votes, message}
 */
public record EditRequestResolutionResponse(
        boolean success,
        Long editRequestId,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer votesReceived,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer votesRequired,
        @JsonInclude(JsonInclude.Include.NON_NULL) String newStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<EditVoteSummary> votes,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long documentId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String documentName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String finalStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) String newDocumentStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) String newModalityStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean resolvedByTiebreaker) {

    public record EditVoteSummary(
            String examinerName,
            String examinerEmail,
            String decision,
            String notes,
            Boolean isTiebreakerVote,
            LocalDateTime votedAt) {
    }
}