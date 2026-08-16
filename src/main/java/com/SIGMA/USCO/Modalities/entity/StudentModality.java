package com.SIGMA.USCO.Modalities.entity;

import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityType;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_modalities")
public class StudentModality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    @ToString.Include
    private ModalityType modalityType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "leader_id")
    private User leader;


    @OneToMany(mappedBy = "studentModality", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StudentModalityMember> members;


    @OneToMany(mappedBy = "studentModality", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ModalityInvitation> invitations;

    @ManyToOne(optional = false)
    @JoinColumn(name = "academic_program_id")
    private AcademicProgram academicProgram;

    @ManyToOne(optional = false)
    @JoinColumn(name = "program_degree_modality_id")
    private ProgramDegreeModality programDegreeModality;

    @OneToMany(mappedBy = "studentModality", cascade = CascadeType.ALL)
    private List<ModalityProcessStatusHistory> statusHistory;


    @Enumerated(EnumType.STRING)
    @Column(length = 100, nullable = false)
    @ToString.Include
    private ModalityProcessStatus status;

    @ToString.Include
    private LocalDateTime selectionDate;
    @ToString.Include
    private LocalDateTime updatedAt;



    @ManyToOne
    private User projectDirector;


    @OneToMany(mappedBy = "studentModality", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DefenseExaminer> defenseExaminers;


    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    @ToString.Include
    private AcademicDistinction academicDistinction;


    @ToString.Include
    private Double finalGrade;

    @ToString.Include
    private LocalDateTime defenseDate;

    @ToString.Include
    private String defenseLocation;


    @ToString.Include
    private LocalDateTime correctionRequestDate;
    @ToString.Include
    private LocalDateTime correctionDeadline;
    @ToString.Include
    private Boolean correctionReminderSent;


    @Column(nullable = false)
    @Builder.Default
    @ToString.Include
    private Integer correctionAttempts = 0;

    /**
     * Título del proyecto de grado.
     * Se extrae automáticamente de los PDFs/plantillas que sube el estudiante.
     * Ej: "SIGMA (sistema interno de gestión de modalidades académicas)"
     */
    @Column(length = 500)
    @ToString.Include
    private String modalityTitle;

}
