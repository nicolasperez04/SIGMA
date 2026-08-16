package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.documents.entity.enums.FinalDocumentRubricType;
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
@Table(name = "final_document_evaluations")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalDocumentEvaluation {

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
    @Column(name = "rubric_type")
    @ToString.Include
    private FinalDocumentRubricType rubricType;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary", nullable = false)
    @ToString.Include
    private ProposalAspectGrade summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "introduction", nullable = false)
    @ToString.Include
    private ProposalAspectGrade introduction;

    @Enumerated(EnumType.STRING)
    @Column(name = "materials_and_methods", nullable = false)
    @ToString.Include
    private ProposalAspectGrade materialsAndMethods;

    @Enumerated(EnumType.STRING)
    @Column(name = "results_and_discussion", nullable = false)
    @ToString.Include
    private ProposalAspectGrade resultsAndDiscussion;

    @Enumerated(EnumType.STRING)
    @Column(name = "conclusions", nullable = false)
    @ToString.Include
    private ProposalAspectGrade conclusions;

    @Enumerated(EnumType.STRING)
    @Column(name = "bibliography_and_references", nullable = false)
    @ToString.Include
    private ProposalAspectGrade bibliographyReferences;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_organization", nullable = false)
    @ToString.Include
    private ProposalAspectGrade documentOrganization;

    @Enumerated(EnumType.STRING)
    @Column(name = "prototype_or_software")
    @ToString.Include
    private ProposalAspectGrade prototypeOrSoftware;

    @Enumerated(EnumType.STRING)
    @Column(name = "general_objective")
    @ToString.Include
    private ProposalAspectGrade generalObjective;

    @Enumerated(EnumType.STRING)
    @Column(name = "activities_objective_coherence")
    @ToString.Include
    private ProposalAspectGrade activitiesObjectiveCoherence;

    @Enumerated(EnumType.STRING)
    @Column(name = "critical_activities_description")
    @ToString.Include
    private ProposalAspectGrade criticalActivitiesDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "practice_compliance_evidence")
    @ToString.Include
    private ProposalAspectGrade practiceComplianceEvidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_and_writing")
    @ToString.Include
    private ProposalAspectGrade organizationAndWriting;

    @Column(name = "evaluated_at", nullable = false)
    @ToString.Include
    private LocalDateTime evaluatedAt;
}

