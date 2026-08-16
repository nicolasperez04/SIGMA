package com.SIGMA.USCO.Modalities.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de getAllEditRequestsForExaminer: claves exactas del Map anterior
 * {success, examiner, modality, summary, editRequests}.
 */
public record ExaminerEditRequestsResponse(
        boolean success,
        ExaminerContext examiner,
        ModalityContext modality,
        EditRequestSummary summary,
        List<EditRequestListItem> editRequests) {

    public record ExaminerContext(
            Long examinerId,
            String examinerName,
            String examinerEmail,
            String examinerType,
            String examinerTypeLabel,
            boolean isTiebreaker) {
    }

    public record ModalityContext(
            Long studentModalityId,
            String modalityName,
            String academicProgram,
            String currentModalityStatus,
            List<String> students) {
    }

    public record EditRequestSummary(
            long total,
            long pending,
            long tiebreakerRequired,
            long approved,
            long rejected) {
    }

    /**
     * myVote se serializa SIEMPRE (null cuando el jurado aún no votó), igual que el Map anterior.
     */
    public record EditRequestListItem(
            Long editRequestId,
            Long documentId,
            String documentName,
            String documentType,
            String currentDocumentStatus,
            String requesterName,
            String requesterEmail,
            String reason,
            String status,
            String statusDescription,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt,
            String finalResolutionNotes,
            int totalVotes,
            List<EditRequestVoteInfo> votes,
            boolean authenticatedExaminerAlreadyVoted,
            boolean authenticatedExaminerCanVote,
            EditRequestMyVote myVote) {
    }

    public record EditRequestVoteInfo(
            String examinerName,
            String examinerEmail,
            String examinerTypeLabel,
            String decision,
            String decisionLabel,
            String notes,
            Boolean isTiebreakerVote,
            LocalDateTime votedAt) {
    }

    public record EditRequestMyVote(
            String decision,
            String decisionLabel,
            String notes,
            LocalDateTime votedAt) {
    }
}