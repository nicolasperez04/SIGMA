package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Users.entity.User;
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
@Table(name = "student_document_status_history")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDocumentStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private StudentDocument studentDocument;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    @ToString.Include
    private DocumentStatus status;

    @ToString.Include
    private LocalDateTime changeDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private User responsible;

    @Column(length = 5000)
    @ToString.Include
    private String observations;

}
