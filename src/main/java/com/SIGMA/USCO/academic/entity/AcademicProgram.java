package com.SIGMA.USCO.academic.entity;

import com.SIGMA.USCO.Modalities.Entity.DegreeModality;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "academic_programs")
@Entity
public class AcademicProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 5000)
    private String description;

    @Column(nullable = false, unique = true, length = 30)
    private String code;
    // Ej: ING_SOFTWARE, ING_CIVIL, MEDICINA

    @Column(name = "total_credits", nullable = false)
    private Long totalCredits;

    @ManyToOne(optional = false)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @OneToMany(mappedBy = "academicProgram")
    private List<ProgramDegreeModality> programModalities;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "AcademicProgram{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", totalCredits=" + totalCredits +
                ", active=" + active +
                '}';
    }
}
