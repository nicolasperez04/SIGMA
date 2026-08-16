package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.documents.entity.enums.EditRequestVoteDecision;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Registra el voto individual de cada jurado sobre una solicitud de edición de documento.
 * Sigue la misma lógica de consenso que ExaminerDocumentReview:
 * - Ambos jurados primarios aprueban → solicitud APPROVED
 * - Ambos rechazan → solicitud REJECTED
 * - Uno aprueba y otro rechaza → se requiere jurado de desempate (TIEBREAKER_REQUIRED)
 */
@Entity
@Table(
        name = "document_edit_request_votes",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"edit_request_id", "examiner_id"},
                name = "uk_edit_request_vote"
        )
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEditRequestVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    /** Solicitud de edición sobre la que se vota */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "edit_request_id", nullable = false)
    private DocumentEditRequest editRequest;

    /** Jurado que emite el voto */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "examiner_id", nullable = false)
    private User examiner;

    /** Decisión del jurado: APPROVED o REJECTED */
    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 30)
    @ToString.Include
    private EditRequestVoteDecision decision;

    /** Notas del jurado al votar */
    @Column(name = "notes", length = 2000)
    @ToString.Include
    private String notes;

    /** Indica si es un voto de desempate */
    @Column(name = "is_tiebreaker_vote", nullable = false)
    @Builder.Default
    @ToString.Include
    private Boolean isTiebreakerVote = false;

    /** Fecha en que se registró el voto */
    @Column(name = "voted_at", nullable = false)
    @Builder.Default
    @ToString.Include
    private LocalDateTime votedAt = LocalDateTime.now();
}

