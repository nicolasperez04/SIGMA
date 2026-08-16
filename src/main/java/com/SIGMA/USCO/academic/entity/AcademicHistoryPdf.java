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

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "academic_history_pdfs")
public class AcademicHistoryPdf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_profile_id")
    private StudentProfile studentProfile;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User uploadedBy;

    @Column(nullable = false)
    @ToString.Include
    private String filePath;

    @Column(nullable = false)
    @ToString.Include
    private String originalFileName;

    // Datos extraídos del PDF para búsqueda y auditoría
    @Column(name = "extracted_program_name")
    @ToString.Include
    private String extractedProgramName;

    @Column(name = "extracted_approved_credits")
    @ToString.Include
    private Long extractedApprovedCredits;

    @Column(name = "extracted_total_credits")
    @ToString.Include
    private Long extractedTotalCredits;

    @Column(name = "extracted_gpa")
    @ToString.Include
    private Double extractedGpa;

    // Metadata
    @Column(name = "file_size_bytes")
    @ToString.Include
    private Long fileSizeBytes;

    @Column(length = 500)
    @ToString.Include
    private String notes;

    @Column(nullable = false, updatable = false)
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

