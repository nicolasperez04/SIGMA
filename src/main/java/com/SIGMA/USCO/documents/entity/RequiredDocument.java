package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "modality_id")
    private DegreeModality modality;

    @Column(nullable = false)
    @ToString.Include
    private String documentName;

    @ToString.Include
    private String allowedFormat;

    @ToString.Include
    private Integer maxFileSizeMB;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    @ToString.Include
    private DocumentType documentType;

    @Column(length = 5000)
    @ToString.Include
    private String description;

    @ToString.Include
    private boolean active = true;

    /**
     * Indica si este documento requiere que el jurado complete la evaluación
     * detallada de propuesta (resumen, antecedentes, objetivos, etc.)
     * mediante ProposalEvaluation.
     *
     * Solo debe ser true para el documento de propuesta de grado.
     * Documentos MANDATORY que no son propuesta (contratos, formularios, etc.)
     * deben tener este campo en false.
     */
    @Column(name = "requires_proposal_evaluation", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @ToString.Include
    private boolean requiresProposalEvaluation = false;

    @ToString.Include
    private LocalDateTime createdAt;
    @ToString.Include
    private LocalDateTime updatedAt;

}
