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

@Entity
@Builder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "program_degree_modalities")
public class ProgramDegreeModality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "academic_program_id")
    private AcademicProgram academicProgram;

    @ManyToOne(optional = false)
    @JoinColumn(name = "degree_modality_id")
    private DegreeModality degreeModality;

    @ToString.Include
    private Long creditsRequired;

    @ToString.Include
    private boolean active = true;

    /**
     * Indica si esta modalidad requiere el proceso completo de sustentación:
     * director de proyecto, asignación de jurados, sustentación y evaluación.
     * Cuando es false, el comité simplemente aprueba o rechaza la modalidad
     * directamente una vez subidos todos los documentos.
     * Por defecto es true para mantener compatibilidad con el flujo existente.
     */
    @Column(name = "requires_defense_process", nullable = false)
    @Builder.Default
    @ToString.Include
    private boolean requiresDefenseProcess = true;

    @ToString.Include
    private LocalDateTime createdAt;
    @ToString.Include
    private LocalDateTime updatedAt;

}
