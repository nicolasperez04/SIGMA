package com.SIGMA.USCO.academic.entity;

import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "academic_programs")
@Entity
public class AcademicProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    @ToString.Include
    private String name;

    @Column(length = 5000)
    @ToString.Include
    private String description;

    @Column(nullable = false, unique = true, length = 30)
    @ToString.Include
    private String code;
    // Ej: ING_SOFTWARE, ING_CIVIL, MEDICINA

    @Column(name = "total_credits", nullable = false)
    @ToString.Include
    private Long totalCredits;

    @ManyToOne(optional = false)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @OneToMany(mappedBy = "academicProgram")
    private List<ProgramDegreeModality> programModalities;

    @Column(nullable = false)
    @ToString.Include
    private boolean active = true;

    @ToString.Include
    private LocalDateTime createdAt;
    @ToString.Include
    private LocalDateTime updatedAt;

}
