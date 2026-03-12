package com.SIGMA.USCO.academic.entity;

import com.SIGMA.USCO.Modalities.Entity.DegreeModality;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "program_degree_modalities")
public class ProgramDegreeModality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "academic_program_id")
    private AcademicProgram academicProgram;

    @ManyToOne(optional = false)
    @JoinColumn(name = "degree_modality_id")
    private DegreeModality degreeModality;

    private Long creditsRequired;

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
    private boolean requiresDefenseProcess = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "ProgramDegreeModality{" +
                "id=" + id +
                ", creditsRequired=" + creditsRequired +
                ", active=" + active +
                ", requiresDefenseProcess=" + requiresDefenseProcess +
                '}';
    }
}
