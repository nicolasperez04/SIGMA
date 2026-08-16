package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
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
@Table(name = "student_documents", indexes = {
        @Index(name = "idx_student_doc_modality_config", columnList = "student_modality_id, document_config_id")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private StudentModality studentModality;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private RequiredDocument documentConfig;

    @ToString.Include
    private String fileName;
    @ToString.Include
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(length = 60)
    @ToString.Include
    private DocumentStatus status;

    @Column(length = 3000)
    @ToString.Include
    private String notes;

    @ToString.Include
    private LocalDateTime uploadDate;

}
