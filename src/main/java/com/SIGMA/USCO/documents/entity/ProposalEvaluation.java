package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.documents.entity.enums.ProposalAspectGrade;
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
@Table(name = "proposal_evaluations")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_document_id")
    private StudentDocument studentDocument;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "examiner_id")
    private User examiner;


    @Enumerated(EnumType.STRING)
    @Column(name = "summary", nullable = false)
    @ToString.Include
    private ProposalAspectGrade summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "background_justification", nullable = false)
    @ToString.Include
    private ProposalAspectGrade backgroundJustification;

    @Enumerated(EnumType.STRING)
    @Column(name = "problem_statement", nullable = false)
    @ToString.Include
    private ProposalAspectGrade problemStatement;

    @Enumerated(EnumType.STRING)
    @Column(name = "objectives", nullable = false)
    @ToString.Include
    private ProposalAspectGrade objectives;

    @Enumerated(EnumType.STRING)
    @Column(name = "methodology", nullable = false)
    @ToString.Include
    private ProposalAspectGrade methodology;

    @Enumerated(EnumType.STRING)
    @Column(name = "bibliography_references", nullable = false)
    @ToString.Include
    private ProposalAspectGrade bibliographyReferences;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_organization", nullable = false)
    @ToString.Include
    private ProposalAspectGrade documentOrganization;


    @Column(name = "evaluated_at", nullable = false)
    @ToString.Include
    private LocalDateTime evaluatedAt;
}
