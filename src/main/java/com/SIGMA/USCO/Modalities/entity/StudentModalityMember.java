package com.SIGMA.USCO.Modalities.entity;

import com.SIGMA.USCO.Modalities.entity.enums.MemberStatus;
import com.SIGMA.USCO.Users.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Entidad que representa la relación entre estudiantes y modalidades.
 * Permite que múltiples estudiantes trabajen en una misma modalidad (modalidad grupal).
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "student_modality_members", indexes = {
        @Index(name = "idx_member_modality_status", columnList = "student_modality_id, status"),
        @Index(name = "idx_member_student", columnList = "student_id")
})
public class StudentModalityMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_modality_id")
    private StudentModality studentModality;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Builder.Default
    @Column(nullable = false)
    @ToString.Include
    private Boolean isLeader = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    @ToString.Include
    private MemberStatus status;

    @Column(nullable = false)
    @ToString.Include
    private LocalDateTime joinedAt;

}

