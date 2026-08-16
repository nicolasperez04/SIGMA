package com.SIGMA.USCO.Modalities.entity;

import com.SIGMA.USCO.Modalities.entity.enums.ModalityStatus;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.Faculty;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
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

@Entity
@Builder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "degree_modalities")
public class DegreeModality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(unique = true, nullable = false)
    @ToString.Include
    private String name;

    @Column(length = 4000)
    @ToString.Include
    private String description;

    @Enumerated(EnumType.STRING)
    @ToString.Include
    private ModalityStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @OneToMany(mappedBy = "degreeModality")
    private List<ProgramDegreeModality> programConfigurations;


    @ToString.Include
    private LocalDateTime createdAt;
    @ToString.Include
    private LocalDateTime updatedAt;

}
