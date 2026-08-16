package com.SIGMA.USCO.Modalities.entity;

import com.SIGMA.USCO.Modalities.entity.enums.CertificateStatus;
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
 * Acta de participación del jurado en la modalidad de grado.
 * Registra la participación, evaluaciones y culminación del proceso por parte del jurado.
 */
@Entity
@Table(name = "examiner_certificates")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExaminerCertificate {

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "defense_examiner_id", nullable = false)
    private DefenseExaminer defenseExaminer;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String certificateNumber;

    @Column(nullable = false)
    @ToString.Include
    private LocalDateTime issueDate;

    @Column(nullable = false)
    @ToString.Include
    private String filePath;

    @Column(nullable = false)
    @ToString.Include
    private String fileHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private CertificateStatus status;

    @Column
    @ToString.Include
    private LocalDateTime createdAt;

    @Column
    @ToString.Include
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

