package com.SIGMA.USCO.Modalities.entity;

import com.SIGMA.USCO.Modalities.entity.enums.CertificateStatus;
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
@Table(name = "academic_certificates")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @OneToOne
    @JoinColumn(name = "student_modality_id", nullable = false)
    private StudentModality studentModality;

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

