package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.documents.entity.enums.FinalDocumentRubricType;
import com.SIGMA.USCO.documents.entity.enums.ProposalAspectGrade;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "final_document_evaluations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalDocumentEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_document_id")
    private StudentDocument studentDocument;

    @ManyToOne(optional = false)
    @JoinColumn(name = "examiner_id")
    private User examiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "rubric_type")
    private FinalDocumentRubricType rubricType;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary", nullable = false)
    private ProposalAspectGrade summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "introduction", nullable = false)
    private ProposalAspectGrade introduction;

    @Enumerated(EnumType.STRING)
    @Column(name = "materials_and_methods", nullable = false)
    private ProposalAspectGrade materialsAndMethods;

    @Enumerated(EnumType.STRING)
    @Column(name = "results_and_discussion", nullable = false)
    private ProposalAspectGrade resultsAndDiscussion;

    @Enumerated(EnumType.STRING)
    @Column(name = "conclusions", nullable = false)
    private ProposalAspectGrade conclusions;

    @Enumerated(EnumType.STRING)
    @Column(name = "bibliography_and_references", nullable = false)
    private ProposalAspectGrade bibliographyReferences;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_organization", nullable = false)
    private ProposalAspectGrade documentOrganization;

    @Enumerated(EnumType.STRING)
    @Column(name = "prototype_or_software")
    private ProposalAspectGrade prototypeOrSoftware;

    @Enumerated(EnumType.STRING)
    @Column(name = "general_objective")
    private ProposalAspectGrade generalObjective;

    @Enumerated(EnumType.STRING)
    @Column(name = "activities_objective_coherence")
    private ProposalAspectGrade activitiesObjectiveCoherence;

    @Enumerated(EnumType.STRING)
    @Column(name = "critical_activities_description")
    private ProposalAspectGrade criticalActivitiesDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "practice_compliance_evidence")
    private ProposalAspectGrade practiceComplianceEvidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_and_writing")
    private ProposalAspectGrade organizationAndWriting;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;
}

