package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Modalities.Entity.DegreeModality;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "modality_id")
    private DegreeModality modality;

    @Column(nullable = false)
    private String documentName;

    private String allowedFormat;

    private Integer maxFileSizeMB;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(length = 5000)
    private String description;

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
    private boolean requiresProposalEvaluation = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
