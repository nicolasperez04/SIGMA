package com.SIGMA.USCO.Modalities.entity;

import com.SIGMA.USCO.Modalities.entity.enums.ExaminerType;
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



@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "defense_examiners", indexes = {
        @Index(name = "idx_defense_examiner_modality", columnList = "student_modality_id")
})
public class DefenseExaminer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_modality_id", nullable = false)
    private StudentModality studentModality;

    @ManyToOne(optional = false)
    @JoinColumn(name = "examiner_id", nullable = false)
    private User examiner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @ToString.Include
    private ExaminerType examinerType;

    @Column(nullable = false)
    @ToString.Include
    private LocalDateTime assignmentDate;

    @ManyToOne
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedBy;


}

