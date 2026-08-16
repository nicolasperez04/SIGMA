package com.SIGMA.USCO.academic.entity;

import com.SIGMA.USCO.Users.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_profiles")
public class StudentProfile {
    @Id
    @Column(name = "user_id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "academic_program_id")
    private AcademicProgram academicProgram;

    @ManyToOne(optional = false)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;


    @ToString.Include
    private Long approvedCredits;
    @ToString.Include
    private Double gpa;            // Grade Point Average (promedio)
    @ToString.Include
    private Long semester;
    @ToString.Include
    private String studentCode;

}
