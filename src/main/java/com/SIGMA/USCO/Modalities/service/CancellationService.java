package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.ModalityProcessStatusHistory;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.ModalityProcessStatusHistoryRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.CancellationList;
import com.SIGMA.USCO.Modalities.dto.response.CancellationRejectedByDirectorResponse;
import com.SIGMA.USCO.Modalities.dto.response.CancellationRejectedResponse;
import com.SIGMA.USCO.Modalities.dto.response.CancellationRequestResponse;
import com.SIGMA.USCO.Modalities.dto.response.DirectorAssignmentResponse;
import com.SIGMA.USCO.Modalities.dto.response.DirectorChangeResponse;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.common.security.Roles;
import com.SIGMA.USCO.common.web.OperationResultResponse;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.Modalities.event.ModalityEvent;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final ModalityStatusTransition modalityStatusTransition;

    @Transactional
    public CancellationRequestResponse requestCancellation(Long studentModalityId, User student) {

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
        // ponytail: CANCELLATION_REJECTED* no necesita bloqueo — rejectModalityCancellationByDirector/rejectCancellation
        // restauran el estado previo a la solicitud, por lo que nunca queda un estado de cancelación vivo.
        if (studentModality.getStatus() == ModalityProcessStatus.CANCELLATION_REQUESTED ||
                studentModality.getStatus() == ModalityProcessStatus.CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR ||
                studentModality.getStatus() == ModalityProcessStatus.MODALITY_CANCELLED) {

            throw new ValidationException(
                    "La modalidad ya tiene una solicitud de cancelación"
            );
        }

        // Validar que la modalidad no esté en un estado final
        if (studentModality.getStatus() == ModalityProcessStatus.GRADED_APPROVED ||
                studentModality.getStatus() == ModalityProcessStatus.GRADED_FAILED ||
                studentModality.getStatus() == ModalityProcessStatus.MODALITY_CLOSED ||
                studentModality.getStatus() == ModalityProcessStatus.SEMINAR_CANCELED ||
                studentModality.getStatus() == ModalityProcessStatus.CANCELLED_BY_CORRECTION_TIMEOUT ||
                studentModality.getStatus() == ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL ||
                studentModality.getStatus() == ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW) {

            throw new ValidationException(
                    "La modalidad está en un estado final y no puede solicitarse su cancelación"
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

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CANCELLATION_REQUESTED, student,
                "Solicitud de cancelación enviada por el estudiante con documento justificativo");

        // Notificar a las partes interesadas
        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CANCELLATION_REQUESTED, studentModality.getId(), student.getId(), Map.of(ModalityEvent.KEY_STUDENT_ID, student.getId()))
        );

        return new CancellationRequestResponse(
                true,
                "Solicitud de cancelación enviada correctamente",
                studentModalityId,
                ModalityProcessStatus.CANCELLATION_REQUESTED
        );
    }

    public Resource getCancellationDocumentResource(StudentDocument document) throws MalformedURLException {
        Path filePath = Paths.get(document.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException("No se pudo leer el archivo");
        }
        return resource;
    }

    @Transactional
    public OperationResultResponse approveModalityCancellationByDirector(Long studentModalityId, User projectDirector) {

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

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR, projectDirector,
                "El director de proyecto aprobó la solicitud de cancelación. Pendiente de revisión del comité de currículo");

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CANCELLATION_APPROVED, studentModality.getId(), projectDirector.getId(), Map.of(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, projectDirector.getId()))
        );

        return new OperationResultResponse(
                true,
                "Solicitud de cancelación aprobada. Será enviada al comité de currículo para aprobación final"
        );
    }

    @Transactional
    public CancellationRejectedByDirectorResponse rejectModalityCancellationByDirector(Long studentModalityId, String reason, User projectDirector) {

        if (reason == null || reason.isBlank()) {
            throw new ValidationException(
                    "Debe indicar el motivo del rechazo de la cancelación"
            );
        }

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

        // ===== OBTENER ESTADO PREVIO A LA SOLICITUD DE CANCELACIÓN =====
        // Mismo patrón determinista que rejectCancellation (comité): restaurar el
        // último estado del historial que NO sea de cancelación, en lugar de asumir
        // history[-2] (frágil: depende del orden y del contenido exacto del historial).
        List<ModalityProcessStatusHistory> history = historyRepository
                .findByStudentModalityIdOrderByChangeDateAsc(studentModalityId);

        List<ModalityProcessStatus> cancellationStatuses = List.of(
                ModalityProcessStatus.CANCELLATION_REQUESTED,
                ModalityProcessStatus.CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR,
                ModalityProcessStatus.CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR,
                ModalityProcessStatus.CANCELLATION_REJECTED
        );

        ModalityProcessStatus previousStatus = history.stream()
                .filter(h -> !cancellationStatuses.contains(h.getStatus()))
                .sorted((h1, h2) -> h2.getChangeDate().compareTo(h1.getChangeDate()))
                .findFirst()
                .map(ModalityProcessStatusHistory::getStatus)
                .orElse(ModalityProcessStatus.MODALITY_SELECTED);

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR, projectDirector,
                "El director de proyecto rechazó la solicitud de cancelación. Motivo: " + reason);

        modalityStatusTransition.transition(studentModality, previousStatus, projectDirector,
                "Modalidad restaurada al estado anterior tras rechazo de cancelación");

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CANCELLATION_REJECTED, studentModality.getId(), projectDirector.getId(), Map.of(ModalityEvent.KEY_REASON, reason, ModalityEvent.KEY_COMMITTEE_MEMBER_ID, projectDirector.getId()))
        );

        return new CancellationRejectedByDirectorResponse(
                true,
                "Solicitud de cancelación rechazada. La modalidad continúa en proceso normal",
                previousStatus
        );
    }

    @Transactional
    public OperationResultResponse approveCancellation(Long studentModalityId, User committeeMember) {

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

        modalityStatusTransition.transition(modality, ModalityProcessStatus.MODALITY_CANCELLED, committeeMember,
                "Cancelación aprobada por el comité de currículo del programa");

        // Limpiar defensa/nota para evitar "defensas fantasma" en listados y reportes.
        // No se borran miembros: se conserva trazabilidad.
        modality.setDefenseDate(null);
        modality.setDefenseLocation(null);
        modality.setFinalGrade(null);
        modality.setAcademicDistinction(null);
        modality.setCorrectionRequestDate(null);
        modality.setCorrectionDeadline(null);

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CANCELLATION_APPROVED, modality.getId(), committeeMember.getId(), Map.of(ModalityEvent.KEY_COMMITTEE_MEMBER_ID, committeeMember.getId()))
        );

        return new OperationResultResponse(
                true,
                "La modalidad fue cancelada correctamente"
        );
    }

    @Transactional
    public CancellationRejectedResponse rejectCancellation(Long studentModalityId, String reason, User committeeMember) {

        if (reason == null || reason.isBlank()) {
            throw new ValidationException(
                    "Debe indicar el motivo del rechazo"
            );
        }

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
        modalityStatusTransition.transition(modality, ModalityProcessStatus.CANCELLATION_REJECTED, committeeMember,
                "Solicitud de cancelación rechazada por el comité de currículo. Motivo: " + reason);

        // 2. Restaurar automáticamente al estado previo a la solicitud de cancelación
        modalityStatusTransition.transition(modality, stateToRestore, committeeMember,
                "Modalidad restaurada automáticamente al estado previo a la solicitud de cancelación: " +
                        stateToRestore.name() + ". La modalidad continúa su proceso normal.");

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_CANCELLATION_REJECTED, modality.getId(), committeeMember.getId(), Map.of(ModalityEvent.KEY_REASON, reason, ModalityEvent.KEY_COMMITTEE_MEMBER_ID, committeeMember.getId()))
        );

        return new CancellationRejectedResponse(
                true,
                "Solicitud de cancelación rechazada. La modalidad ha sido restaurada a su estado previo.",
                stateToRestore.name(),
                ModalityServiceUtils.describeModalityStatus(stateToRestore)
        );
    }

    @Transactional(readOnly = true)
    public List<CancellationList> getPendingCancellations(User committeeMember) {

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
    public DirectorAssignmentResponse assignProjectDirector(Long studentModalityId, Long directorId, User committeeMember) {

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
                        .anyMatch(role -> role.getName().equalsIgnoreCase(Roles.ROLE_PROJECT_DIRECTOR));

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

        String observation =
                previousDirector == null
                        ? "Director asignado: " + director.getEmail()
                        : "Cambio de Director: " +
                        previousDirector.getEmail() +
                        " → " +
                        director.getEmail();

        modalityStatusTransition.transition(studentModality, ModalityProcessStatus.READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE, committeeMember, observation);

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DIRECTOR_ASSIGNED, studentModality.getId(), committeeMember.getId(), Map.of(
                        ModalityEvent.KEY_DIRECTOR_ID, director.getId()
                ))
        );

        return new DirectorAssignmentResponse(
                true,
                studentModality.getId(),
                director.getEmail(),
                "Director asignado correctamente a la modalidad"
        );
    }

    @Transactional
    public DirectorChangeResponse changeProjectDirector(Long studentModalityId, Long newDirectorId, String reason, User committeeMember) {

        if (reason == null || reason.isBlank()) {
            throw new ValidationException(
                    "Debe proporcionar una razón para el cambio de director"
            );
        }

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
                .anyMatch(role -> role.getName().equalsIgnoreCase(Roles.ROLE_PROJECT_DIRECTOR));

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

        modalityStatusTransition.recordHistory(studentModality, studentModality.getStatus(), committeeMember, observation);

        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.DIRECTOR_ASSIGNED, studentModality.getId(), committeeMember.getId(), Map.of(
                        ModalityEvent.KEY_DIRECTOR_ID, newDirector.getId()
                ))
        );

        return new DirectorChangeResponse(
                true,
                studentModality.getId(),
                new DirectorChangeResponse.DirectorInfo(
                        currentDirector.getId(),
                        currentDirector.getEmail(),
                        currentDirector.getName() + " " + currentDirector.getLastName()
                ),
                new DirectorChangeResponse.DirectorInfo(
                        newDirector.getId(),
                        newDirector.getEmail(),
                        newDirector.getName() + " " + newDirector.getLastName()
                ),
                reason,
                "Director de proyecto cambiado exitosamente"
        );
    }
}
