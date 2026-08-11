package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.Entity.ModalityProcessStatusHistory;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.ModalityProcessStatusHistoryRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.CancellationList;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.security.SecurityUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CancellationService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final ModalityProcessStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Map<String, Object> requestCancellation(Long studentModalityId) {

        User student = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        // Validar que el usuario sea miembro activo de la modalidad
        boolean isActiveMember = studentModalityMemberRepository.isActiveMember(
                studentModalityId,
                student.getId()
        );

        if (!isActiveMember) {
            throw new ForbiddenException("No autorizado para solicitar cancelación de esta modalidad");
        }

        // Validar que tenga director de proyecto asignado
        if (studentModality.getProgramDegreeModality().isRequiresDefenseProcess() &&  studentModality.getProjectDirector() == null) {
            throw new ValidationException(
                    "No puede solicitar la cancelación aún. Debe tener un director de proyecto asignado a su modalidad antes de solicitar la cancelación."
            );
        }

        // Validar que la modalidad no esté ya en proceso de cancelación
        if (studentModality.getStatus() == ModalityProcessStatus.CANCELLATION_REQUESTED ||
                studentModality.getStatus() == ModalityProcessStatus.MODALITY_CANCELLED) {

            throw new ValidationException(
                    "La modalidad ya tiene una solicitud de cancelación"
            );
        }

        // CORRECCIÓN CRÍTICA: Validar específicamente documento de tipo CANCELLATION
        List<StudentDocument> documents =
                studentDocumentRepository.findByStudentModalityId(studentModalityId);

        boolean hasCancellationDocument = documents.stream()
                .anyMatch(doc -> doc.getDocumentConfig().getDocumentType() == DocumentType.CANCELLATION);

        if (!hasCancellationDocument) {
            throw new ValidationException(
                    "Debe subir el documento de justificación de cancelación antes de solicitar la cancelación de la modalidad"
            );
        }

        // Validar que el documento de cancelación esté en estado válido
        Optional<StudentDocument> cancellationDoc = documents.stream()
                .filter(doc -> doc.getDocumentConfig().getDocumentType() == DocumentType.CANCELLATION)
                .findFirst();

        if (cancellationDoc.isPresent()) {
            DocumentStatus docStatus = cancellationDoc.get().getStatus();
            if (docStatus == DocumentStatus.REJECTED_FOR_PROGRAM_HEAD_REVIEW ||
                docStatus == DocumentStatus.REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW) {
                throw new ValidationException(
                        "El documento de cancelación fue rechazado. Debe subir una nueva versión antes de solicitar la cancelación."
                );
            }
        }

        // Cambiar estado de la modalidad
        studentModality.setStatus(ModalityProcessStatus.CANCELLATION_REQUESTED);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        // Registrar en historial
        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.CANCELLATION_REQUESTED)
                        .changeDate(LocalDateTime.now())
                        .responsible(student)
                        .observations("Solicitud de cancelación enviada por el estudiante con documento justificativo")
                        .build()
        );

        // Notificar a las partes interesadas
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CANCELLATION_REQUESTED, studentModality.getId(), student.getId(), Map.of(ModalityEvent.KEY_STUDENT_ID, student.getId()))
        );

        return Map.of(
                        "success", true,
                        "message", "Solicitud de cancelación enviada correctamente",
                        "studentModalityId", studentModalityId,
                        "newStatus", ModalityProcessStatus.CANCELLATION_REQUESTED
                );
    }

    @Transactional
    public Map<String, Object> approveModalityCancellationByDirector(Long studentModalityId) {

        User projectDirector = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        if (studentModality.getProjectDirector() == null ||
                !studentModality.getProjectDirector().getId().equals(projectDirector.getId())) {
            throw new ForbiddenException("No tiene permiso para aprobar la cancelación. No es el director asignado a esta modalidad");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.CANCELLATION_REQUESTED) {
            throw new ValidationException(
                    "La modalidad no tiene una solicitud de cancelación pendiente"
            );
        }

        studentModality.setStatus(ModalityProcessStatus.CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR)
                        .changeDate(LocalDateTime.now())
                        .responsible(projectDirector)
                        .observations("El director de proyecto aprobó la solicitud de cancelación. Pendiente de revisión del comité de currículo")
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CANCELLATION_APPROVED, studentModality.getId(), projectDirector.getId(), Map.of(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, projectDirector.getId()))
        );

        return Map.of(
                        "success", true,
                        "message", "Solicitud de cancelación aprobada. Será enviada al comité de currículo para aprobación final"
                );
    }

    @Transactional
    public Map<String, Object> rejectModalityCancellationByDirector(Long studentModalityId, String reason) {

        if (reason == null || reason.isBlank()) {
            throw new ValidationException(
                    "Debe indicar el motivo del rechazo de la cancelación"
            );
        }

        User projectDirector = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        if (studentModality.getProjectDirector() == null ||
                !studentModality.getProjectDirector().getId().equals(projectDirector.getId())) {
            throw new ForbiddenException("No tiene permiso para rechazar la cancelación. No es el director asignado a esta modalidad");
        }

        if (studentModality.getStatus() != ModalityProcessStatus.CANCELLATION_REQUESTED) {
            throw new ValidationException(
                    "La modalidad no tiene una solicitud de cancelación pendiente"
            );
        }

        ModalityProcessStatus previousStatus = ModalityProcessStatus.PROPOSAL_APPROVED;

        List<ModalityProcessStatusHistory> history = historyRepository
                .findByStudentModalityIdOrderByChangeDateAsc(studentModalityId);

        if (history.size() >= 2) {

            previousStatus = history.get(history.size() - 2).getStatus();
        }

        studentModality.setStatus(ModalityProcessStatus.CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR)
                        .changeDate(LocalDateTime.now())
                        .responsible(projectDirector)
                        .observations("El director de proyecto rechazó la solicitud de cancelación. Motivo: " + reason)
                        .build()
        );

        studentModality.setStatus(previousStatus);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(previousStatus)
                        .changeDate(LocalDateTime.now())
                        .responsible(projectDirector)
                        .observations("Modalidad restaurada al estado anterior tras rechazo de cancelación")
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CANCELLATION_REJECTED, studentModality.getId(), projectDirector.getId(), Map.of(ModalityEvent.KEY_REASON, reason, ModalityEvent.KEY_COMMITTEE_MEMBER_ID, projectDirector.getId()))
        );

        return Map.of(
                        "success", true,
                        "message", "Solicitud de cancelación rechazada. La modalidad continúa en proceso normal",
                        "restoredStatus", previousStatus
                );
    }

    @Transactional
    public Map<String, Object> approveCancellation(Long studentModalityId) {

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality modality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        AcademicProgram academicProgram = modality.getProgramDegreeModality().getAcademicProgram();

        boolean authorized =
                programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                        committeeMember.getId(),
                        academicProgram.getId(),
                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                );

        if (!authorized) {
            throw new ForbiddenException("No tiene permiso para aprobar la cancelación de esta modalidad");
        }

        if (modality.getStatus() != ModalityProcessStatus.CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR) {
            throw new ValidationException(
                    "La cancelación debe ser aprobada primero por el director de proyecto"
            );
        }

        modality.setStatus(ModalityProcessStatus.MODALITY_CANCELLED);
        modality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(modality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(modality)
                        .status(ModalityProcessStatus.MODALITY_CANCELLED)
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations("Cancelación aprobada por el comité de currículo del programa")
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CANCELLATION_APPROVED, modality.getId(), committeeMember.getId(), Map.of(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, committeeMember.getId()))
        );

        return Map.of(
                        "success", true,
                        "message", "La modalidad fue cancelada correctamente"
                );
    }

    @Transactional
    public Map<String, Object> rejectCancellation(Long studentModalityId, String reason) {

        if (reason == null || reason.isBlank()) {
            throw new ValidationException(
                    "Debe indicar el motivo del rechazo"
            );
        }

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality modality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        AcademicProgram academicProgram =
                modality
                        .getProgramDegreeModality()
                        .getAcademicProgram();

        boolean authorized =
                programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                        committeeMember.getId(),
                        academicProgram.getId(),
                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                );

        if (!authorized) {
            throw new ForbiddenException("No tiene permiso para rechazar la cancelación de esta modalidad");
        }

        if (modality.getStatus() != ModalityProcessStatus.CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR) {
            throw new ValidationException(
                    "Solo se pueden rechazar cancelaciones aprobadas por el director de proyecto"
            );
        }

        // ===== OBTENER ESTADO PREVIO A LA SOLICITUD DE CANCELACIÓN =====
        // El historial de cancelación tiene esta secuencia:
        //   CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR  ← estado actual
        //   CANCELLATION_REQUESTED
        //   <estado real previo a la cancelación>  ← este es el que queremos restaurar
        // Filtramos todos los estados de cancelación y tomamos el más reciente que NO sea de cancelación.
        List<ModalityProcessStatusHistory> fullHistory = historyRepository
                .findByStudentModalityIdOrderByChangeDateAsc(studentModalityId);

        List<ModalityProcessStatus> cancellationStatuses = List.of(
                ModalityProcessStatus.CANCELLATION_REQUESTED,
                ModalityProcessStatus.CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR,
                ModalityProcessStatus.CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR,
                ModalityProcessStatus.CANCELLATION_REJECTED
        );

        // Filtrar estados que no son de cancelación, del más reciente al más antiguo
        List<ModalityProcessStatusHistory> nonCancellationHistory = fullHistory.stream()
                .filter(h -> !cancellationStatuses.contains(h.getStatus()))
                .sorted((h1, h2) -> h2.getChangeDate().compareTo(h1.getChangeDate()))
                .toList();

        // El estado a restaurar es el último estado previo a cualquier estado de cancelación
        ModalityProcessStatus stateToRestore = nonCancellationHistory.isEmpty()
                ? ModalityProcessStatus.MODALITY_SELECTED  // fallback de seguridad
                : nonCancellationHistory.get(0).getStatus();

        // 1. Registrar el rechazo en el historial
        modality.setStatus(ModalityProcessStatus.CANCELLATION_REJECTED);
        modality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(modality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(modality)
                        .status(ModalityProcessStatus.CANCELLATION_REJECTED)
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations("Solicitud de cancelación rechazada por el comité de currículo. Motivo: " + reason)
                        .build()
        );

        // 2. Restaurar automáticamente al estado previo a la solicitud de cancelación
        modality.setStatus(stateToRestore);
        modality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(modality);

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(modality)
                        .status(stateToRestore)
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations("Modalidad restaurada automáticamente al estado previo a la solicitud de cancelación: " +
                                stateToRestore.name() + ". La modalidad continúa su proceso normal.")
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CANCELLATION_REJECTED, modality.getId(), committeeMember.getId(), Map.of(ModalityEvent.KEY_REASON, reason, ModalityEvent.KEY_COMMITTEE_MEMBER_ID, committeeMember.getId()))
        );

        return Map.of(
                        "success", true,
                        "message", "Solicitud de cancelación rechazada. La modalidad ha sido restaurada a su estado previo.",
                        "restoredStatus", stateToRestore.name(),
                        "restoredStatusDescription", ModalityServiceUtils.describeModalityStatus(stateToRestore)
                );
    }

    @Transactional(readOnly = true)
    public List<CancellationList> getPendingCancellations() {

        User committeeMember = SecurityUtils.getCurrentUser();

        List<Long> academicProgramIds =
                programAuthorityRepository
                        .findByUser_IdAndRole(
                                committeeMember.getId(),
                                ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                        )
                        .stream()
                        .map(pa -> pa.getAcademicProgram().getId())
                        .toList();

        if (academicProgramIds.isEmpty()) {
            return List.of();
        }

        List<StudentModality> modalities =
                studentModalityRepository
                        .findByStatusAndProgramDegreeModality_AcademicProgram_IdIn(
                                ModalityProcessStatus.CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR,
                                academicProgramIds
                        );

        return modalities.stream()
                .map(sm -> CancellationList.builder()
                        .studentModalityId(sm.getId())
                        .studentName(
                                sm.getLeader().getName() + " " +
                                        sm.getLeader().getLastName()
                        )
                        .email(sm.getLeader().getEmail())
                        .modalityName(
                                sm.getProgramDegreeModality()
                                        .getDegreeModality()
                                        .getName()
                        )
                        .requestDate(sm.getUpdatedAt())
                        .build()
                )
                .toList();
    }

    @Transactional
    public Map<String, Object> assignProjectDirector(Long studentModalityId, Long directorId) {

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad del estudiante no encontrada"));

        User director = userRepository.findById(directorId)
                .orElseThrow(() -> new NotFoundException("Director no encontrado"));

        Long academicProgramId =
                studentModality
                        .getProgramDegreeModality()
                        .getAcademicProgram()
                        .getId();

        boolean committeeAuthorized =
                programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                        committeeMember.getId(),
                        academicProgramId,
                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                );

        if (!committeeAuthorized) {
            throw new ForbiddenException("No tiene permiso para asignar director en este programa académico");
        }

        boolean hasDirectorRole =
                director.getRoles().stream()
                        .anyMatch(role -> role.getName().equalsIgnoreCase("PROJECT_DIRECTOR"));

        if (!hasDirectorRole) {
            throw new ValidationException(
                    "El usuario seleccionado no tiene rol de Director de Proyecto"
            );
        }

        boolean directorAuthorized =
                programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                        director.getId(),
                        academicProgramId,
                        ProgramRole.PROJECT_DIRECTOR
                );

        if (!directorAuthorized) {
            throw new ValidationException(
                    "El director no pertenece a este programa académico"
            );
        }

        if (studentModality.getStatus() != ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT &&
                studentModality.getStatus() != ModalityProcessStatus.CANCELLATION_REJECTED) {
            throw new ValidationException(
                    "No se puede asignar un Director de Proyecto en este momento. " +
                               "La modalidad debe estar en estado 'Listo para asignar Director' " +
                               "(todos los documentos obligatorios aprobados por el Comité de Currículo)."
            );
        }

        User previousDirector = studentModality.getProjectDirector();

        studentModality.setProjectDirector(director);
        studentModality.setUpdatedAt(LocalDateTime.now());
        // Cambiar estado a READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE
        studentModality.setStatus(ModalityProcessStatus.READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE);
        studentModalityRepository.save(studentModality);

        String observation =
                previousDirector == null
                        ? "Director asignado: " + director.getEmail()
                        : "Cambio de Director: " +
                        previousDirector.getEmail() +
                        " → " +
                        director.getEmail();

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(ModalityProcessStatus.READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE)
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations(observation)
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DIRECTOR_ASSIGNED, studentModality.getId(), committeeMember.getId(), Map.of(
                        ModalityEvent.KEY_DIRECTOR_ID, director.getId()
                ))
        );

        return Map.of(
                        "success", true,
                        "studentModalityId", studentModality.getId(),
                        "directorAssigned", director.getEmail(),
                        "message", "Director asignado correctamente a la modalidad"
                );
    }

    @Transactional
    public Map<String, Object> changeProjectDirector(Long studentModalityId, Long newDirectorId, String reason) {

        if (reason == null || reason.isBlank()) {
            throw new ValidationException(
                    "Debe proporcionar una razón para el cambio de director"
            );
        }

        User committeeMember = SecurityUtils.getCurrentUser();

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad del estudiante no encontrada"));

        User newDirector = userRepository.findById(newDirectorId)
                .orElseThrow(() -> new NotFoundException("Director no encontrado"));

        User currentDirector = studentModality.getProjectDirector();
        if (currentDirector == null) {
            throw new ValidationException(
                    "La modalidad no tiene un director asignado actualmente. Use el método de asignación inicial."
            );
        }

        if (currentDirector.getId().equals(newDirectorId)) {
            throw new ValidationException(
                    "El director seleccionado ya está asignado a esta modalidad"
            );
        }

        Long academicProgramId = studentModality.getProgramDegreeModality().getAcademicProgram().getId();

        boolean committeeAuthorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(committeeMember.getId(), academicProgramId,
                ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

        if (!committeeAuthorized) {
            throw new ForbiddenException("No tiene permiso para cambiar director en este programa académico");
        }

        boolean hasDirectorRole = newDirector.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("PROJECT_DIRECTOR"));

        if (!hasDirectorRole) {
            throw new ValidationException(
                    "El usuario seleccionado no tiene rol de Director de Proyecto"
            );
        }

        boolean newDirectorAuthorized = programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                newDirector.getId(),
                academicProgramId,
                ProgramRole.PROJECT_DIRECTOR
        );

        if (!newDirectorAuthorized) {
            throw new ValidationException(
                    "El nuevo director no pertenece a este programa académico"
            );
        }

        if (studentModality.getStatus() == ModalityProcessStatus.MODALITY_CLOSED ||
                studentModality.getStatus() == ModalityProcessStatus.MODALITY_CANCELLED ||
                studentModality.getStatus() == ModalityProcessStatus.GRADED_APPROVED ||
                studentModality.getStatus() == ModalityProcessStatus.GRADED_FAILED ||
                studentModality.getStatus() == ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL ||
                studentModality.getStatus() == ModalityProcessStatus.CANCELLATION_REJECTED) {

            throw new ValidationException(
                    "No se puede cambiar el director en modalidades finalizadas, cerradas o canceladas"
            );
        }

        studentModality.setProjectDirector(newDirector);
        studentModality.setUpdatedAt(LocalDateTime.now());
        studentModalityRepository.save(studentModality);

        String observation = String.format(
                "CAMBIO DE DIRECTOR DE PROYECTO. Director anterior: %s (%s %s). Nuevo director: %s (%s %s). Razón: %s",
                currentDirector.getEmail(),
                currentDirector.getName(),
                currentDirector.getLastName(),
                newDirector.getEmail(),
                newDirector.getName(),
                newDirector.getLastName(),
                reason
        );

        historyRepository.save(
                ModalityProcessStatusHistory.builder()
                        .studentModality(studentModality)
                        .status(studentModality.getStatus())
                        .changeDate(LocalDateTime.now())
                        .responsible(committeeMember)
                        .observations(observation)
                        .build()
        );

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DIRECTOR_ASSIGNED, studentModality.getId(), committeeMember.getId(), Map.of(
                        ModalityEvent.KEY_DIRECTOR_ID, newDirector.getId()
                ))
        );

        List<StudentModalityMember> activeMembers = studentModalityMemberRepository
                .findByStudentModalityIdAndStatus(studentModality.getId(), MemberStatus.ACTIVE);

        for (StudentModalityMember member : activeMembers) {
            /* DEAD: DirectorChangedEvent has no listener — no-op */
        }

        return Map.of(
                        "success", true,
                        "studentModalityId", studentModality.getId(),
                        "previousDirector", Map.of(
                                "id", currentDirector.getId(),
                                "email", currentDirector.getEmail(),
                                "name", currentDirector.getName() + " " + currentDirector.getLastName()
                        ),
                        "newDirector", Map.of(
                                "id", newDirector.getId(),
                                "email", newDirector.getEmail(),
                                "name", newDirector.getName() + " " + newDirector.getLastName()
                        ),
                        "reason", reason,
                        "message", "Director de proyecto cambiado exitosamente"
                );
    }
}
