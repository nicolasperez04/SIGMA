package com.SIGMA.USCO.documents.entity;

import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.documents.entity.enums.DocumentEditRequestStatus;
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
 * Registra la solicitud de un estudiante para editar/resubir un documento
 * que ya fue previamente aprobado por los jurados evaluadores.
 */
@Entity
@Table(name = "document_edit_requests")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEditRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    /** Documento aprobado sobre el que se solicita la edición */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_document_id", nullable = false)
    private StudentDocument studentDocument;

    /** Estudiante que solicita la edición */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** Motivo o justificación de la solicitud de edición */
    @Column(name = "reason", nullable = false, length = 200000)
    @ToString.Include
    private String reason;

    /** Estado actual de la solicitud */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    @ToString.Include
    private DocumentEditRequestStatus status = DocumentEditRequestStatus.PENDING;

    /** Jurado o responsable que resolvió la solicitud */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy;

    /** Notas del jurado al aprobar o rechazar la solicitud */
    @Column(name = "resolution_notes", length = 2000)
    @ToString.Include
    private String resolutionNotes;

    /** Fecha en que se creó la solicitud */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    @ToString.Include
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Fecha en que fue resuelta (aprobada o rechazada) */
    @Column(name = "resolved_at")
    @ToString.Include
    private LocalDateTime resolvedAt;
}

