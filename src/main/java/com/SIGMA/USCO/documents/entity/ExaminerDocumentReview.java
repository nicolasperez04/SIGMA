package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.documents.entity.enums.ExaminerDocumentDecision;
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
 * Registra la decisión individual de cada jurado sobre un documento específico.
 * Permite rastrear el voto de cada jurado (PRIMARY_EXAMINER_1, PRIMARY_EXAMINER_2, TIEBREAKER_EXAMINER)
 * sobre cada documento de la modalidad.
 */
@Entity
@Table(
    name = "examiner_document_reviews",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"student_document_id", "examiner_id"},
        name = "uk_examiner_document_review"
    )
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExaminerDocumentReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_document_id", nullable = false)
    private StudentDocument studentDocument;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "examiner_id", nullable = false)
    private User examiner;

    /**
     * Decisión del jurado: ACCEPTED, CORRECTIONS_REQUESTED, REJECTED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    @ToString.Include
    private ExaminerDocumentDecision decision;

    @Column(length = 3000)
    @ToString.Include
    private String notes;

    @Column(nullable = false)
    @ToString.Include
    private LocalDateTime reviewedAt;

    /**
     * Indica si este es un voto de desempate
     */
    @Column(nullable = false)
    @ToString.Include
    private Boolean isTiebreakerVote;
}


