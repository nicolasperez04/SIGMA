package com.SIGMA.USCO.Users.entity;

import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "program_authorities",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "academic_program_id", "role"}
        ))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "academic_program_id")
    private AcademicProgram academicProgram;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    @ToString.Include
    private ProgramRole role;

}
