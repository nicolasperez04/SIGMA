package com.SIGMA.USCO.Modalities.entity;

import com.SIGMA.USCO.Modalities.entity.enums.SeminarStatus;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "seminars")
public class Seminar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "academic_program_id")
    private AcademicProgram academicProgram;

    @Column(nullable = false, length = 200)
    @ToString.Include
    private String name;

    @Column(length = 4000)
    @ToString.Include
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    @ToString.Include
    private BigDecimal totalCost;

    @Column(nullable = false)
    @ToString.Include
    private Integer minParticipants;

    @Column(nullable = false)
    @ToString.Include
    private Integer maxParticipants;

    @Builder.Default
    @Column(nullable = false)
    @ToString.Include
    private Integer currentParticipants = 0;

    @Builder.Default
    @Column(nullable = false)
    @ToString.Include
    private Integer totalHours = 160;

    @Builder.Default
    @Column(nullable = false)
    @ToString.Include
    private boolean active = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @ToString.Include
    private SeminarStatus status = SeminarStatus.OPEN;

    @ToString.Include
    private LocalDateTime startDate;

    @ToString.Include
    private LocalDateTime endDate;

    @ToString.Include
    private LocalDateTime createdAt;

    @ToString.Include
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "seminar_students",
        joinColumns = @JoinColumn(name = "seminar_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Builder.Default
    private Set<StudentProfile> enrolledStudents = new HashSet<>();

}
