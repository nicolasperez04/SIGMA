package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.InvitationStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.dto.groups.EligibleStudentDTO;
import com.SIGMA.USCO.Modalities.Entity.ModalityInvitation;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Repository.*;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.FacultyRepository;
import com.SIGMA.USCO.academic.repository.ProgramDegreeModalityRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.common.exception.BusinessException;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentStatusHistoryRepository;
import com.SIGMA.USCO.notifications.event.ModalityEvent;
import com.SIGMA.USCO.notifications.entity.enums.NotificationType;
import com.SIGMA.USCO.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModalityGroupService {

    private final DegreeModalityRepository degreeModalityRepository;
    private final ModalityRequirementsRepository modalityRequirementsRepository;
    private final RequiredDocumentRepository requiredDocumentRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final ModalityInvitationRepository modalityInvitationRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final ModalityStatusTransition modalityStatusTransition;
    private final StudentDocumentStatusHistoryRepository documentHistoryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final FacultyRepository facultyRepository;
    private final ProgramDegreeModalityRepository programDegreeModalityRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;
    private final DefenseExaminerRepository defenseExaminerRepository;


    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public Map<String, Object> startStudentModalityGroup(Long modalityId) {

        User student = SecurityUtils.getCurrentUser();

        StudentProfile profile = studentProfileRepository.findByUserId(student.getId())
                .orElseThrow(() -> new ValidationException("Debe completar su perfil académico antes de seleccionar una modalidad"));


        com.SIGMA.USCO.Modalities.Entity.DegreeModality modality = degreeModalityRepository.findById(modalityId)
                .orElseThrow(() -> new NotFoundException("La modalidad con ID " + modalityId + " no existe"));


        com.SIGMA.USCO.academic.entity.ProgramDegreeModality programDegreeModality =
                programDegreeModalityRepository.findByAcademicProgramIdAndDegreeModalityIdAndActiveTrue(
                        profile.getAcademicProgram().getId(),
                        modalityId
                ).orElseThrow(() -> new ValidationException("La modalidad no está habilitada para tu programa académico"));


        // Verificar si el estudiante tiene modalidades activas (en proceso)
        // Estados finalizados que SÍ permiten iniciar nueva modalidad: MODALITY_CLOSED, MODALITY_CANCELLED, GRADED_APPROVED, GRADED_FAILED
        List<ModalityProcessStatus> finalizedStatuses = List.of(
                ModalityProcessStatus.MODALITY_CLOSED,
                ModalityProcessStatus.MODALITY_CANCELLED,
                ModalityProcessStatus.GRADED_FAILED
        );

        // Obtener todas las modalidades del estudiante como miembro activo
        List<StudentModalityMember> activeMembers = studentModalityMemberRepository.findByStudentIdAndStatus(
                student.getId(),
                MemberStatus.ACTIVE
        );

        // Verificar si alguna de esas modalidades NO está finalizada
        for (StudentModalityMember member : activeMembers) {
            ModalityProcessStatus currentStatus = member.getStudentModality().getStatus();
            if (!finalizedStatuses.contains(currentStatus)) {
                throw new ValidationException("Ya tienes una modalidad de grado en curso. No puedes iniciar otra.");
            }
        }

        // Verificar si el estudiante tiene una modalidad CERRADA (MODALITY_CLOSED)
        // Si tiene una modalidad cerrada, NO puede volver a iniciar la MISMA modalidad
        List<StudentModality> closedModalities = studentModalityRepository.findByLeaderIdAndStatus(
                student.getId(),
                ModalityProcessStatus.MODALITY_CLOSED
        );

        for (StudentModality closedModality : closedModalities) {
            if (closedModality.getProgramDegreeModality().getDegreeModality().getId().equals(modalityId)) {
                throw new ValidationException("No puedes volver a iniciar esta modalidad. Debes seleccionar una modalidad diferente.");
            }
        }


        List<com.SIGMA.USCO.Modalities.Entity.ModalityRequirements> requirements =
                modalityRequirementsRepository.findByModalityIdAndActiveTrue(modalityId);

        List<com.SIGMA.USCO.Modalities.dto.ValidationItemDTO> results = new ArrayList<>();
        boolean allValid = true;

        for (com.SIGMA.USCO.Modalities.Entity.ModalityRequirements req : requirements) {

            if (req.getRuleType() != com.SIGMA.USCO.Modalities.Entity.enums.RuleType.NUMERIC) {
                continue;
            }

            boolean fulfilled = true;
            String studentValue = "";


            if (req.getRequirementName().toLowerCase().contains("crédito")) {
                double percentageRequired = Double.parseDouble(req.getExpectedValue());
                long totalCredits = profile.getAcademicProgram().getTotalCredits();
                long requiredCredits = Math.round(totalCredits * percentageRequired);

                fulfilled = profile.getApprovedCredits() >= requiredCredits;
                studentValue = profile.getApprovedCredits() + " / " + requiredCredits;
            }


            if (req.getRequirementName().toLowerCase().contains("promedio")) {
                fulfilled = profile.getGpa() >= Double.parseDouble(req.getExpectedValue());
                studentValue = String.valueOf(profile.getGpa());
            }

            results.add(
                    com.SIGMA.USCO.Modalities.dto.ValidationItemDTO.builder()
                            .requirementName(req.getRequirementName())
                            .expectedValue(req.getExpectedValue())
                            .studentValue(studentValue)
                            .fulfilled(fulfilled)
                            .build()
            );

            if (!fulfilled) {
                allValid = false;
            }
        }

        if (!allValid) {
            // ponytail: message only; frontend falls back to .message, per-requirement .results dropped
            throw new ValidationException("No cumples los requisitos académicos para esta modalidad");
        }


        StudentModality studentModality = StudentModality.builder()
                .leader(student)
                .modalityType(com.SIGMA.USCO.Modalities.Entity.enums.ModalityType.GROUP)
                .academicProgram(profile.getAcademicProgram())
                .programDegreeModality(programDegreeModality)
                .status(com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus.MODALITY_SELECTED)
                .selectionDate(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        studentModalityRepository.save(studentModality);


        com.SIGMA.USCO.Modalities.Entity.StudentModalityMember member =
                com.SIGMA.USCO.Modalities.Entity.StudentModalityMember.builder()
                        .studentModality(studentModality)
                        .student(student)
                        .isLeader(true)
                        .status(com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus.ACTIVE)
                        .joinedAt(java.time.LocalDateTime.now())
                        .build();

        studentModalityMemberRepository.save(member);


        modalityStatusTransition.recordHistory(studentModality,
                com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus.MODALITY_SELECTED, student,
                "Modalidad grupal iniciada por el líder del grupo");


        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_STARTED, studentModality.getId(), student.getId(), Map.of())
        );

        return Map.of(
                "eligible", true,
                "studentModalityId", studentModality.getId(),
                "studentModalityName", modality.getName(),
                "modalityType", "GROUP",
                "message", "Modalidad grupal iniciada correctamente. Ahora puedes invitar a otros estudiantes (máximo 2 adicionales)."
        );
    }

    @Transactional(readOnly = true)
    public List<EligibleStudentDTO> getEligibleStudentsForInvitation(String nameFilter) {

        User leader = SecurityUtils.getCurrentUser();

        StudentProfile leaderProfile = studentProfileRepository.findByUserId(leader.getId())
                .orElseThrow(() -> new ValidationException("Debe completar su perfil académico antes de invitar estudiantes"));

        Long leaderProgramId = leaderProfile.getAcademicProgram().getId();


        List<StudentProfile> studentProfiles = studentProfileRepository
                .findByAcademicProgramId(leaderProgramId);

        List<EligibleStudentDTO> eligibleStudents = new ArrayList<>();

        for (StudentProfile profile : studentProfiles) {
            User student = profile.getUser();


            if (student.getId().equals(leader.getId())) {
                continue;
            }


            boolean isStudent = student.getRoles().stream()
                    .anyMatch(role -> role.getName().equals("STUDENT"));

            if (!isStudent) {
                continue;
            }


            if (!"ACTIVE".equals(student.getStatus().name())) {
                continue;
            }


            boolean hasAnyModalityAsLeader = studentModalityRepository
                    .existsByLeaderId(student.getId());


            boolean isAnyModalityMember = studentModalityMemberRepository
                    .existsByStudentId(student.getId());


            if (hasAnyModalityAsLeader || isAnyModalityMember) {
                continue;
            }


            String fullName = student.getName() + " " + student.getLastName();
            if (nameFilter != null && !nameFilter.isBlank()) {
                if (!fullName.toLowerCase().contains(nameFilter.toLowerCase())) {
                    continue;
                }
            }


            eligibleStudents.add(
                    EligibleStudentDTO.builder()
                            .userId(student.getId())
                            .fullName(fullName)
                            .academicProgramName(profile.getAcademicProgram().getName())
                            .currentSemester(profile.getSemester())
                            .build()
            );
        }

        return eligibleStudents;
    }


    public Map<String, Object> inviteStudentToModality(Long studentModalityId, Long inviteeId) {

        User inviter = SecurityUtils.getCurrentUser();


        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));





        User invitee = userRepository.findById(inviteeId)
                .orElseThrow(() -> new NotFoundException("Estudiante a invitar no encontrado"));


        if (invitee.getId().equals(inviter.getId())) {
            throw new ValidationException("No puedes invitarte a ti mismo");
        }


        StudentProfile inviteeProfile = studentProfileRepository.findByUserId(inviteeId)
                .orElse(null);

        if (inviteeProfile == null) {
            throw new ValidationException("El estudiante no tiene perfil académico registrado");
        }


        StudentProfile inviterProfile = studentProfileRepository.findByUserId(inviter.getId())
                .orElseThrow(() -> new NotFoundException("Perfil del líder no encontrado"));

        if (!inviteeProfile.getAcademicProgram().getId()
                .equals(inviterProfile.getAcademicProgram().getId())) {
            throw new ValidationException("El estudiante debe pertenecer al mismo programa académico");
        }


        boolean hasModalityAsLeader = studentModalityRepository.existsByLeaderId(inviteeId);
        boolean isModalityMember = studentModalityMemberRepository.existsByStudentId(inviteeId);

        if (hasModalityAsLeader || isModalityMember) {
            throw new ValidationException("El estudiante ya tiene una modalidad asociada");
        }


        boolean hasPendingInvitation = modalityInvitationRepository
                .hasPendingInvitation(studentModalityId, inviteeId);

        if (hasPendingInvitation) {
            throw new ValidationException("Ya existe una invitación pendiente para este estudiante");
        }


        long currentMembersCount = studentModalityMemberRepository
                .countByStudentModalityIdAndStatus(studentModalityId,
                        com.SIGMA.USCO.Modalities.Entity.enums.MemberStatus.ACTIVE);


        long pendingInvitationsCount = modalityInvitationRepository
                .countByStudentModalityIdAndStatus(studentModalityId,
                        com.SIGMA.USCO.Modalities.Entity.enums.InvitationStatus.PENDING);


        final int MAX_GROUP_SIZE = 3;


        long totalProjected = currentMembersCount + pendingInvitationsCount + 1;

        if (totalProjected > MAX_GROUP_SIZE) {
            throw new ValidationException("El grupo ya ha alcanzado el límite máximo de " + MAX_GROUP_SIZE +
                                       " miembros (actuales: " + currentMembersCount +
                                       ", invitaciones pendientes: " + pendingInvitationsCount + ")");
        }


        ModalityInvitation invitation = ModalityInvitation.builder()
                .studentModality(studentModality)
                .inviter(inviter)
                .invitee(invitee)
                .status(com.SIGMA.USCO.Modalities.Entity.enums.InvitationStatus.PENDING)
                .invitedAt(java.time.LocalDateTime.now())
                .build();

        modalityInvitationRepository.save(invitation);


        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_INVITATION_RECEIVED, studentModalityId, inviter.getId(), Map.of(
                        ModalityEvent.KEY_INVITATION_ID, invitation.getId(),
                        ModalityEvent.KEY_INVITEE_ID, inviteeId,
                        ModalityEvent.KEY_INVITER_ID, inviter.getId(),
                        ModalityEvent.KEY_MODALITY_NAME, studentModality.getProgramDegreeModality().getDegreeModality().getName(),
                        ModalityEvent.KEY_INVITER_NAME, inviter.getName() + " " + inviter.getLastName()
                ))
        );

        return Map.of(
                "success", true,
                "invitationId", invitation.getId(),
                "message", "Invitación enviada exitosamente a " + invitee.getName() + " " + invitee.getLastName()
        );
    }


    @Transactional
    public Map<String, Object> acceptInvitation(Long invitationId) {

        User student = SecurityUtils.getCurrentUser();


        ModalityInvitation invitation = modalityInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitación no encontrada"));


        if (!invitation.getInvitee().getId().equals(student.getId())) {
            throw new ForbiddenException("No tienes permiso para aceptar esta invitación");
        }


        if (invitation.getStatus() != com.SIGMA.USCO.Modalities.Entity.enums.InvitationStatus.PENDING) {
            throw new ValidationException("Esta invitación ya fue procesada anteriormente");
        }

        StudentModality studentModality = invitation.getStudentModality();


        boolean hasModalityAsLeader = studentModalityRepository.existsByLeaderId(student.getId());
        boolean isModalityMember = studentModalityMemberRepository.existsByStudentId(student.getId());

        if (hasModalityAsLeader || isModalityMember) {
            throw new ValidationException("Ya tienes una modalidad de grado en curso");
        }


        StudentProfile profile = studentProfileRepository.findByUserId(student.getId())
                .orElseThrow(() -> new ValidationException("Debe completar su perfil académico"));

        List<com.SIGMA.USCO.Modalities.Entity.ModalityRequirements> requirements =
                modalityRequirementsRepository.findByModalityIdAndActiveTrue(
                        studentModality.getProgramDegreeModality().getDegreeModality().getId()
                );

        List<com.SIGMA.USCO.Modalities.dto.ValidationItemDTO> results = new ArrayList<>();
        boolean allValid = true;

        for (com.SIGMA.USCO.Modalities.Entity.ModalityRequirements req : requirements) {

            if (req.getRuleType() != com.SIGMA.USCO.Modalities.Entity.enums.RuleType.NUMERIC) {
                continue;
            }

            boolean fulfilled = true;
            String studentValue = "";

            if (req.getRequirementName().toLowerCase().contains("crédito")) {
                double percentageRequired = Double.parseDouble(req.getExpectedValue());
                long totalCredits = profile.getAcademicProgram().getTotalCredits();
                long requiredCredits = Math.round(totalCredits * percentageRequired);

                fulfilled = profile.getApprovedCredits() >= requiredCredits;
                studentValue = profile.getApprovedCredits() + " / " + requiredCredits;
            }

            if (req.getRequirementName().toLowerCase().contains("promedio")) {
                fulfilled = profile.getGpa() >= Double.parseDouble(req.getExpectedValue());
                studentValue = String.valueOf(profile.getGpa());
            }

            results.add(
                    com.SIGMA.USCO.Modalities.dto.ValidationItemDTO.builder()
                            .requirementName(req.getRequirementName())
                            .expectedValue(req.getExpectedValue())
                            .studentValue(studentValue)
                            .fulfilled(fulfilled)
                            .build()
            );

            if (!fulfilled) {
                allValid = false;
            }
        }

        if (!allValid) {
            throw new ValidationException("No cumples los requisitos académicos para unirte a esta modalidad");
        }


        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        modalityInvitationRepository.save(invitation);


        StudentModalityMember member = StudentModalityMember.builder()
                        .studentModality(studentModality)
                        .student(student)
                        .isLeader(false)
                        .status(MemberStatus.ACTIVE)
                        .joinedAt(LocalDateTime.now())
                        .build();

        studentModalityMemberRepository.save(member);



        modalityStatusTransition.recordHistory(studentModality, studentModality.getStatus(), student,
                "El estudiante " + student.getName() + " " + student.getLastName() +
                        " aceptó la invitación y se unió al grupo");


        long pendingInvitations = modalityInvitationRepository
                .countByStudentModalityIdAndStatus(
                        studentModality.getId(),
                        com.SIGMA.USCO.Modalities.Entity.enums.InvitationStatus.PENDING
                );


        if (pendingInvitations == 0) {
            modalityStatusTransition.recordHistory(studentModality, studentModality.getStatus(), student,
                    "Todas las invitaciones han sido respondidas. El grupo está formado y puede comenzar a trabajar en los documentos.");
        }


        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_INVITATION_ACCEPTED, studentModality.getId(), student.getId(), Map.of(
                        ModalityEvent.KEY_INVITATION_ID, invitationId,
                        ModalityEvent.KEY_ACCEPTED_BY_ID, student.getId(),
                        ModalityEvent.KEY_LEADER_ID, studentModality.getLeader().getId(),
                        ModalityEvent.KEY_ACCEPTED_BY_NAME, student.getName() + " " + student.getLastName(),
                        ModalityEvent.KEY_MODALITY_NAME, studentModality.getProgramDegreeModality().getDegreeModality().getName()
                ))
        );

        return Map.of(
                "success", true,
                "studentModalityId", studentModality.getId(),
                "message", "Te has unido exitosamente al grupo. ¡Bienvenido!",
                "modalityName", studentModality.getProgramDegreeModality().getDegreeModality().getName(),
                "pendingInvitations", pendingInvitations
        );
    }


    @Transactional
    public Map<String, Object> rejectInvitation(Long invitationId) {

        User student = SecurityUtils.getCurrentUser();


        ModalityInvitation invitation = modalityInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitación no encontrada"));


        if (!invitation.getInvitee().getId().equals(student.getId())) {
            throw new ForbiddenException("No tienes permiso para rechazar esta invitación");
        }


        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ValidationException("Esta invitación ya fue procesada anteriormente");
        }

        StudentModality studentModality = invitation.getStudentModality();


        invitation.setStatus(InvitationStatus.REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());
        modalityInvitationRepository.save(invitation);


        modalityStatusTransition.recordHistory(studentModality, studentModality.getStatus(), student,
                "El estudiante " + student.getName() + " " + student.getLastName() +
                        " rechazó la invitación para unirse al grupo");


        applicationEventPublisher.publishEvent(
                new ModalityEvent(NotificationType.MODALITY_INVITATION_REJECTED, studentModality.getId(), student.getId(), Map.of(
                        ModalityEvent.KEY_INVITATION_ID, invitationId,
                        ModalityEvent.KEY_REJECTED_BY_ID, student.getId(),
                        ModalityEvent.KEY_LEADER_ID, studentModality.getLeader().getId(),
                        ModalityEvent.KEY_REJECTED_BY_NAME, student.getName() + " " + student.getLastName(),
                        ModalityEvent.KEY_MODALITY_NAME, studentModality.getProgramDegreeModality().getDegreeModality().getName()
                ))
        );

        return Map.of(
                "success", true,
                "message", "Has rechazado la invitación exitosamente"
        );
    }


}
