package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.DefenseEvaluationCriteria;
import com.SIGMA.USCO.Modalities.entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.entity.enums.ExaminerType;
import com.SIGMA.USCO.Modalities.entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.DefenseEvaluationCriteriaRepository;
import com.SIGMA.USCO.Modalities.repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.repository.ModalityProcessStatusHistoryRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.ModalityListDTO;
import com.SIGMA.USCO.Modalities.dto.ModalityMemberDTO;
import com.SIGMA.USCO.Modalities.dto.ModalityStatusHistoryDTO;
import com.SIGMA.USCO.Modalities.dto.StudentModalityDTO;
import com.SIGMA.USCO.Modalities.dto.StudentModalityHistoryDTO;
import com.SIGMA.USCO.Modalities.dto.response.ProgramStudentsResponse;
import com.SIGMA.USCO.Modalities.dto.response.StudentModalityExaminerDTO;
import com.SIGMA.USCO.Users.entity.ProgramAuthority;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.entity.AcademicHistoryPdf;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.common.exception.BusinessException;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.InternalException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import com.SIGMA.USCO.documents.dto.DetailDocumentDTO;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.enums.DocumentEditRequestStatus;
import com.SIGMA.USCO.documents.entity.enums.FinalDocumentRubricType;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.entity.enums.EditRequestVoteDecision;
import com.SIGMA.USCO.documents.entity.enums.ExaminerDocumentDecision;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentModalityListingService {

    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final ModalityProcessStatusHistoryRepository historyRepository;
    private final DefenseExaminerRepository defenseExaminerRepository;
    private final DefenseEvaluationCriteriaRepository defenseEvaluationCriteriaRepository;
    private final RequiredDocumentRepository requiredDocumentRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;

    private record StatusFlags(boolean canUploadDocuments, boolean canRequestCancellation,
                               boolean canSubmitCorrections, boolean hasDefenseScheduled,
                               boolean requiresAction) {
    }

    private Long calculateDaysRemaining(StudentModality studentModality) {
        if (studentModality.getCorrectionDeadline() == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(LocalDateTime.now(), studentModality.getCorrectionDeadline());
    }

    private StatusFlags computeStatusFlags(StudentModality studentModality) {
        ModalityProcessStatus status = studentModality.getStatus();
        boolean canUploadDocuments = status == ModalityProcessStatus.MODALITY_SELECTED ||
                status == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD ||
                status == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD ||
                status == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE;

        // ponytail: duplicado deliberado de CancellationService.requestCancellation (M3/M4) —
        // los guards están partidos en 2 ifs con mensajes distintos; una constante compartida
        // obligaría a fusionarlos. Mantener ambas listas sincronizadas al editar cualquiera.
        boolean canRequestCancellation = status != ModalityProcessStatus.CANCELLATION_REQUESTED &&
                status != ModalityProcessStatus.CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR &&
                status != ModalityProcessStatus.MODALITY_CANCELLED &&
                status != ModalityProcessStatus.SEMINAR_CANCELED &&
                status != ModalityProcessStatus.CANCELLED_WITHOUT_REPROVAL &&
                status != ModalityProcessStatus.CANCELLED_BY_CORRECTION_TIMEOUT &&
                status != ModalityProcessStatus.CORRECTIONS_REJECTED_FINAL &&
                status != ModalityProcessStatus.GRADED_APPROVED &&
                status != ModalityProcessStatus.GRADED_FAILED &&
                status != ModalityProcessStatus.MODALITY_CLOSED &&
                status != ModalityProcessStatus.PENDING_DISTINCTION_COMMITTEE_REVIEW;

        boolean canSubmitCorrections = (status == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD ||
                status == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE) &&
                studentModality.getCorrectionDeadline() != null &&
                LocalDateTime.now().isBefore(studentModality.getCorrectionDeadline());

        boolean hasDefenseScheduled = studentModality.getDefenseDate() != null;

        boolean requiresAction = canUploadDocuments || canSubmitCorrections ||
                status == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD ||
                status == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE;

        return new StatusFlags(canUploadDocuments, canRequestCancellation, canSubmitCorrections,
                hasDefenseScheduled, requiresAction);
    }

    private long countApprovedDocs(List<StudentDocument> documents) {
        return documents.stream()
                .filter(d -> d.getStatus() == DocumentStatus.ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW)
                .count();
    }

    private long countPendingDocs(List<StudentDocument> documents) {
        return documents.stream()
                .filter(d -> d.getStatus() == DocumentStatus.PENDING ||
                        d.getStatus() == DocumentStatus.ACCEPTED_FOR_PROGRAM_HEAD_REVIEW ||
                        d.getStatus() == DocumentStatus.CORRECTION_RESUBMITTED)
                .count();
    }

    private long countRejectedDocs(List<StudentDocument> documents) {
        return documents.stream()
                .filter(d -> d.getStatus() == DocumentStatus.REJECTED_FOR_PROGRAM_HEAD_REVIEW ||
                        d.getStatus() == DocumentStatus.REJECTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW)
                .count();
    }

    private List<ModalityStatusHistoryDTO> buildStatusHistory(Long studentModalityId) {
        return historyRepository
                .findByStudentModalityIdOrderByChangeDateAsc(studentModalityId)
                .stream()
                .map(h -> ModalityStatusHistoryDTO.builder()
                        .status(h.getStatus().name())
                        .description(ModalityServiceUtils.describeModalityStatus(h.getStatus()))
                        .changeDate(h.getChangeDate())
                        .responsible(
                                h.getResponsible() != null
                                        ? h.getResponsible().getEmail()
                                        : "Sistema"
                        )
                        .observations(TranslationUtils.localizeObservations(h.getObservations()))
                        .build()
                )
                .sorted((h1, h2) -> h2.getChangeDate().compareTo(h1.getChangeDate())) // Ordenar de más reciente a más antiguo
                .toList();
    }

    private List<DetailDocumentDTO> buildDetailDocuments(List<RequiredDocument> requiredDocuments,
                                                         List<StudentDocument> uploadedDocuments) {
        Map<Long, StudentDocument> uploadedMap =
                uploadedDocuments.stream()
                        .collect(Collectors.toMap(
                                d -> d.getDocumentConfig().getId(),
                                d -> d
                        ));

        return requiredDocuments.stream()
                .map(req -> {
                    StudentDocument uploaded = uploadedMap.get(req.getId());

                    return DetailDocumentDTO.builder()
                            .requiredDocumentId(req.getId())
                            .studentDocumentId(
                                    uploaded != null ? uploaded.getId() : null
                            )
                            .documentName(req.getDocumentName())
                            .documentType(req.getDocumentType())
                            .uploaded(uploaded != null)
                            .status(
                                    uploaded != null
                                            ? uploaded.getStatus().name()
                                            : "NOT_UPLOADED"
                            )
                            .statusDescription(
                                    uploaded != null
                                            ? ModalityServiceUtils.describeDocumentStatus(uploaded.getStatus())
                                            : "Documento aún no cargado por el estudiante."
                            )
                            .notes(
                                    uploaded != null ? uploaded.getNotes() : null
                            )
                            .lastUpdate(
                                    uploaded != null ? uploaded.getUploadDate() : null
                            )
                            .build();
                })
                .toList();
    }

    private List<ModalityMemberDTO> mapMembers(List<StudentModalityMember> activeMembers) {
        List<Long> userIds = activeMembers.stream()
                .map(member -> member.getStudent().getId())
                .distinct()
                .toList();
        Map<Long, StudentProfile> profilesByUserId = userIds.isEmpty() ? Map.of()
                : studentProfileRepository.findAllByUserIdIn(userIds)
                        .stream()
                        .collect(Collectors.toMap(StudentProfile::getId, p -> p));

        return activeMembers.stream()
                .map(member -> {
                    StudentProfile memberProfile = profilesByUserId.get(member.getStudent().getId());

                    return ModalityMemberDTO.builder()
                            .memberId(member.getId())
                            .studentId(member.getStudent().getId())
                            .studentName(member.getStudent().getName())
                            .studentLastName(member.getStudent().getLastName())
                            .studentEmail(member.getStudent().getEmail())
                            .studentCode(memberProfile != null ? memberProfile.getStudentCode() : null)
                            .approvedCredits(memberProfile != null ? (memberProfile.getApprovedCredits() != null ? memberProfile.getApprovedCredits().intValue() : null) : null)
                            .gpa(memberProfile != null ? (memberProfile.getGpa() != null ? memberProfile.getGpa().doubleValue() : null) : null)
                            .semester(memberProfile != null ? (memberProfile.getSemester() != null ? memberProfile.getSemester().toString() : null) : null)
                            .isLeader(member.getIsLeader())
                            .status(member.getStatus().name())
                            .joinedAt(member.getJoinedAt())
                            .build();
                })
                .toList();
    }

    private StudentModalityDTO buildStudentModalityDTO(StudentModality studentModality,
                                                       DegreeModality modality,
                                                       AcademicProgram academicProgram,
                                                       User student,
                                                       StudentProfile studentProfile,
                                                       List<ModalityMemberDTO> memberDTOs,
                                                       List<ModalityStatusHistoryDTO> history,
                                                       List<DetailDocumentDTO> documents,
                                                       List<StudentDocument> uploadedDocuments,
                                                       String defenseProposedBy) {
        Long daysRemaining = calculateDaysRemaining(studentModality);

        StatusFlags flags = computeStatusFlags(studentModality);

        long approvedDocs = countApprovedDocs(uploadedDocuments);
        long pendingDocs = countPendingDocs(uploadedDocuments);
        long rejectedDocs = countRejectedDocs(uploadedDocuments);

        User projectDirector = studentModality.getProjectDirector();
        ModalityProcessStatus status = studentModality.getStatus();

        return StudentModalityDTO.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .studentLastName(student.getLastName())
                .studentEmail(student.getEmail())
                .studentCode(studentProfile != null ? studentProfile.getStudentCode() : null)
                .approvedCredits(studentProfile != null ? studentProfile.getApprovedCredits() : null)
                .gpa(studentProfile != null ? studentProfile.getGpa() : null)
                .semester(studentProfile != null ? studentProfile.getSemester() : null)

                .facultyName(academicProgram.getFaculty().getName())
                .academicProgramName(academicProgram.getName())

                .studentModalityId(studentModality.getId())
                .modalityName(modality.getName())
                .modalityDescription(modality.getDescription())
                .creditsRequired(studentModality.getProgramDegreeModality().getCreditsRequired())
                .modalityType(studentModality.getModalityType() != null
                        ? studentModality.getModalityType().name()
                        : null)
                .members(memberDTOs)

                .currentStatus(status.name())
                .currentStatusDescription(ModalityServiceUtils.describeModalityStatus(status))
                .selectionDate(studentModality.getSelectionDate())
                .lastUpdatedAt(studentModality.getUpdatedAt())

                .projectDirectorId(projectDirector != null ? projectDirector.getId() : null)
                .projectDirectorName(projectDirector != null
                        ? projectDirector.getName() + " " + projectDirector.getLastName()
                        : null)
                .projectDirectorEmail(projectDirector != null ? projectDirector.getEmail() : null)

                .defenseDate(studentModality.getDefenseDate())
                .defenseLocation(studentModality.getDefenseLocation())
                .defenseProposedByProjectDirector(defenseProposedBy)

                .academicDistinction(studentModality.getAcademicDistinction() != null
                        ? studentModality.getAcademicDistinction().name()
                        : null)

                .correctionRequestDate(studentModality.getCorrectionRequestDate())
                .correctionDeadline(studentModality.getCorrectionDeadline())
                .correctionReminderSent(studentModality.getCorrectionReminderSent())
                .daysRemainingForCorrection(daysRemaining)

                .documents(documents)
                .totalDocuments(uploadedDocuments.size())
                .approvedDocuments((int) approvedDocs)
                .pendingDocuments((int) pendingDocs)
                .rejectedDocuments((int) rejectedDocs)

                .history(history)

                .canUploadDocuments(flags.canUploadDocuments())
                .canRequestCancellation(flags.canRequestCancellation())
                .canSubmitCorrections(flags.canSubmitCorrections())
                .hasDefenseScheduled(flags.hasDefenseScheduled())
                .requiresAction(flags.requiresAction())

                .build();
    }

    @Transactional(readOnly = true)
    public StudentModalityDTO getCurrentStudentModality(User student) {

        StudentModality studentModality = studentModalityRepository
                .findTopByStudentIdOrderByUpdatedAtDesc(student.getId())
                .orElseThrow(() ->
                        new NotFoundException("Not current modality found for the student")
                );

        DegreeModality modality = studentModality.getProgramDegreeModality().getDegreeModality();

        StudentProfile studentProfile = studentProfileRepository.findByUserId(student.getId())
                .orElse(null);

        List<ModalityStatusHistoryDTO> history = buildStatusHistory(studentModality.getId());

        List<StudentDocument> documents = studentDocumentRepository
                .findByStudentModalityId(studentModality.getId());

        List<DetailDocumentDTO> documentDTOs = documents.stream()
                .map(doc -> DetailDocumentDTO.builder()
                        .requiredDocumentId(doc.getDocumentConfig().getId())
                        .studentDocumentId(doc.getId())
                        .documentName(doc.getDocumentConfig().getDocumentName())
                        .documentType(doc.getDocumentConfig().getDocumentType())
                        .status(doc.getStatus().name())
                        .statusDescription(ModalityServiceUtils.describeDocumentStatus(doc.getStatus()))
                        .notes(doc.getNotes())
                        .lastUpdate(doc.getUploadDate())
                        .uploaded(true)
                        .build()
                )
                .toList();

        long approvedDocs = countApprovedDocs(documents);
        long pendingDocs = countPendingDocs(documents);
        long rejectedDocs = countRejectedDocs(documents);

        Long daysRemaining = calculateDaysRemaining(studentModality);

        StatusFlags flags = computeStatusFlags(studentModality);

        ModalityProcessStatus status = studentModality.getStatus();
        User projectDirector = studentModality.getProjectDirector();
        String defenseProposedBy = null;
        if (status == ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR) {
            defenseProposedBy = "El director de proyecto ha propuesto una fecha de sustentación";
        }

        return StudentModalityDTO.builder()

                        .studentId(student.getId())
                        .studentName(student.getName())
                        .studentLastName(student.getLastName())
                        .studentEmail(student.getEmail())
                        .studentCode(studentProfile != null ? studentProfile.getStudentCode() : null)
                        .approvedCredits(studentProfile != null ? studentProfile.getApprovedCredits() : null)
                        .gpa(studentProfile != null ? studentProfile.getGpa() : null)
                        .semester(studentProfile != null ? studentProfile.getSemester() : null)

                        .facultyName(studentModality.getProgramDegreeModality()
                                .getAcademicProgram().getFaculty().getName())
                        .academicProgramName(studentModality.getProgramDegreeModality()
                                .getAcademicProgram().getName())

                        .studentModalityId(studentModality.getId())
                        .modalityId(modality.getId())
                        .modalityName(modality.getName())
                        .modalityDescription(modality.getDescription())
                        .creditsRequired(studentModality.getProgramDegreeModality()
                                .getCreditsRequired())
                        .modalityType(studentModality.getModalityType() != null
                                ? studentModality.getModalityType().name()
                                : null)

                        .currentStatus(status.name())
                        .currentStatusDescription(ModalityServiceUtils.describeModalityStatus(status))
                        .selectionDate(studentModality.getSelectionDate())
                        .lastUpdatedAt(studentModality.getUpdatedAt())

                        .projectDirectorId(projectDirector != null ? projectDirector.getId() : null)
                        .projectDirectorName(projectDirector != null
                                ? projectDirector.getName() + " " + projectDirector.getLastName()
                                : null)
                        .projectDirectorEmail(projectDirector != null ? projectDirector.getEmail() : null)

                        .defenseDate(studentModality.getDefenseDate())
                        .defenseLocation(studentModality.getDefenseLocation())
                        .defenseProposedByProjectDirector(defenseProposedBy)

                        .academicDistinction(studentModality.getAcademicDistinction() != null
                                ? studentModality.getAcademicDistinction().name()
                                : null)

                        .correctionRequestDate(studentModality.getCorrectionRequestDate())
                        .correctionDeadline(studentModality.getCorrectionDeadline())
                        .correctionReminderSent(studentModality.getCorrectionReminderSent())
                        .daysRemainingForCorrection(daysRemaining)

                        .documents(documentDTOs)
                        .totalDocuments(documents.size())
                        .approvedDocuments((int) approvedDocs)
                        .pendingDocuments((int) pendingDocs)
                        .rejectedDocuments((int) rejectedDocs)

                        .history(history)

                        .canUploadDocuments(flags.canUploadDocuments())
                        .canRequestCancellation(flags.canRequestCancellation())
                        .canSubmitCorrections(flags.canSubmitCorrections())
                        .hasDefenseScheduled(flags.hasDefenseScheduled())
                        .requiresAction(flags.requiresAction())

                        .build();
    }

    @Transactional(readOnly = true)
    public List<StudentModalityHistoryDTO> getCompletedModalitiesHistory(User student) {
        return studentModalityRepository
                .findHistoryByStudentIdOrderBySelectionDateDesc(student.getId())
                .stream()
                .map(sm -> new StudentModalityHistoryDTO(
                        sm.getId(),
                        sm.getProgramDegreeModality().getDegreeModality().getId(),
                        sm.getProgramDegreeModality().getDegreeModality().getName(),
                        sm.getStatus().name(),
                        sm.getSelectionDate()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ModalityListDTO> getAllStudentModalitiesForProgramHead(List<ModalityProcessStatus> statuses, String name, User programHead) {

        List<Long> programIds = programAuthorityRepository
                        .findByUser_Id(programHead.getId())
                        .stream()
                        .filter(pa -> pa.getRole() == ProgramRole.PROGRAM_HEAD)
                        .map(pa -> pa.getAcademicProgram().getId())
                        .toList();

        if (programIds.isEmpty()) {
            return List.of();
        }

        boolean hasStatusFilter = statuses != null && !statuses.isEmpty();
        boolean hasNameFilter = name != null && !name.isBlank();

        List<StudentModality> modalities;

        if (hasStatusFilter && hasNameFilter) {

            modalities =
                    studentModalityRepository.findForProgramHeadWithStatusAndName(programIds, statuses, name);

        } else if (hasStatusFilter) {

            modalities =
                    studentModalityRepository
                            .findForProgramHeadWithStatus(programIds, statuses);

        } else if (hasNameFilter) {

            modalities =
                    studentModalityRepository
                            .findForProgramHeadWithName(programIds, name);

        } else {

            modalities =
                    studentModalityRepository
                            .findForProgramHead(programIds);
        }

        Map<Long, List<StudentModalityMember>> membersByModality = loadActiveMembersByModalities(modalities);

        List<ModalityListDTO> response =
                modalities.stream()
                        .map(sm -> {

                            ModalityProcessStatus status = sm.getStatus();

                            boolean pending =
                                    status == ModalityProcessStatus.MODALITY_SELECTED ||
                                            status == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD;

                            return toModalityList(sm, status, pending,
                                    membersByModality.getOrDefault(sm.getId(), List.of()));
                        })
                        .sorted(Comparator.comparing(ModalityListDTO::getLastUpdatedAt).reversed())
                        .toList();

        return response;
    }

    @Transactional(readOnly = true)
    public List<ModalityListDTO> getAllStudentModalitiesForProgramCurriculumCommittee(List<ModalityProcessStatus> statuses, String name, User committeeMember) {

        List<Long> programIds = programAuthorityRepository
                .findByUser_Id(committeeMember.getId())
                .stream()
                .filter(pa -> pa.getRole() == ProgramRole.PROGRAM_CURRICULUM_COMMITTEE)
                .map(pa -> pa.getAcademicProgram().getId())
                .toList();

        if (programIds.isEmpty()) {
            return List.of();
        }

        List<ModalityProcessStatus> committeeRelevantStatuses = Arrays.asList(ModalityProcessStatus.values());

        List<ModalityProcessStatus> finalStatuses;
        if (statuses != null && !statuses.isEmpty()) {

            finalStatuses = statuses.stream()
                    .filter(committeeRelevantStatuses::contains)
                    .toList();

            if (finalStatuses.isEmpty()) {
                return List.of();
            }
        } else {

            finalStatuses = committeeRelevantStatuses;
        }

        boolean hasNameFilter = name != null && !name.isBlank();

        List<StudentModality> modalities;

        if (hasNameFilter) {
            modalities = studentModalityRepository
                    .findForProgramHeadWithStatusAndName(programIds, finalStatuses, name);
        } else {
            modalities = studentModalityRepository
                    .findForProgramHeadWithStatus(programIds, finalStatuses);
        }

        Map<Long, List<StudentModalityMember>> membersByModality = loadActiveMembersByModalities(modalities);

        List<ModalityListDTO> response =
                modalities.stream()
                        .map(sm -> {

                            ModalityProcessStatus status = sm.getStatus();

                            boolean pending =
                                    status == ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE ||
                                            status == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE ||
                                            status == ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR;

                            return toModalityList(sm, status, pending,
                                    membersByModality.getOrDefault(sm.getId(), List.of()));
                        })
                        .sorted(Comparator.comparing(ModalityListDTO::getLastUpdatedAt).reversed())
                        .toList();

        return response;
    }

    @Transactional(readOnly = true)
    public List<ModalityListDTO> getAllStudentModalitiesForProjectDirector(List<ModalityProcessStatus> statuses, String name, User projectDirector) {

        List<ProgramAuthority> directorAuthorities = programAuthorityRepository
                .findByUser_Id(projectDirector.getId())
                .stream()
                .filter(pa -> pa.getRole() == ProgramRole.PROJECT_DIRECTOR)
                .toList();

        if (directorAuthorities.isEmpty()) {
            throw new ForbiddenException("El usuario no tiene el rol de PROJECT_DIRECTOR");
        }

        boolean hasStatusFilter = statuses != null && !statuses.isEmpty();
        boolean hasNameFilter = name != null && !name.isBlank();

        List<StudentModality> modalities;

        if (hasStatusFilter && hasNameFilter) {
            modalities = studentModalityRepository
                    .findForProjectDirectorWithStatusAndName(projectDirector.getId(), statuses, name);
        } else if (hasStatusFilter) {
            modalities = studentModalityRepository
                    .findForProjectDirectorWithStatus(projectDirector.getId(), statuses);
        } else if (hasNameFilter) {
            modalities = studentModalityRepository
                    .findForProjectDirectorWithName(projectDirector.getId(), name);
        } else {
            modalities = studentModalityRepository
                    .findForProjectDirector(projectDirector.getId());
        }

        Map<Long, List<StudentModalityMember>> membersByModality = loadActiveMembersByModalities(modalities);

        List<ModalityListDTO> response = modalities.stream()
                .map(sm -> {
                    ModalityProcessStatus status = sm.getStatus();

                    boolean pending =
                            status == ModalityProcessStatus.PROPOSAL_APPROVED ||
                            status == ModalityProcessStatus.CANCELLATION_REQUESTED;

                    return toModalityList(sm, status, pending,
                            membersByModality.getOrDefault(sm.getId(), List.of()));
                })
                .sorted(Comparator.comparing(ModalityListDTO::getLastUpdatedAt).reversed())
                .toList();

        return response;
    }

    @Transactional(readOnly = true)
    public List<ModalityListDTO> getAllStudentModalitiesForExaminer(List<ModalityProcessStatus> statuses, String name, User examiner) {

        List<DefenseExaminer> examinerAssignments = defenseExaminerRepository
                .findByExaminerId(examiner.getId());

        if (examinerAssignments.isEmpty()) {
            return List.of();
        }

        boolean hasStatusFilter = statuses != null && !statuses.isEmpty();
        boolean hasNameFilter = name != null && !name.isBlank();

        List<StudentModality> modalities;

        if (hasStatusFilter && hasNameFilter) {
            modalities = studentModalityRepository
                    .findForExaminerWithStatusAndName(examiner.getId(), statuses, name);
        } else if (hasStatusFilter) {
            modalities = studentModalityRepository
                    .findForExaminerWithStatus(examiner.getId(), statuses);
        } else if (hasNameFilter) {
            modalities = studentModalityRepository
                    .findForExaminerWithName(examiner.getId(), name);
        } else {
            modalities = studentModalityRepository
                    .findForExaminer(examiner.getId());
        }

        Map<Long, List<StudentModalityMember>> membersByModality = loadActiveMembersByModalities(modalities);

        List<ModalityListDTO> response = modalities.stream()
                .map(sm -> {
                    ModalityProcessStatus status = sm.getStatus();

                    boolean pending =
                            status == ModalityProcessStatus.DEFENSE_SCHEDULED;

                    return toModalityList(sm, status, pending,
                            membersByModality.getOrDefault(sm.getId(), List.of()));
                })
                .sorted(Comparator.comparing(ModalityListDTO::getLastUpdatedAt).reversed())
                .toList();

        return response;
    }

    @Transactional(readOnly = true)
    public StudentModalityDTO getStudentModalityDetailForProgramHead(Long studentModalityId, User programHead) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        AcademicProgram academicProgram = studentModality.getProgramDegreeModality().getAcademicProgram();

        boolean authorized =
                programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                        programHead.getId(),
                        academicProgram.getId(),
                        ProgramRole.PROGRAM_HEAD
                );

        if (!authorized) {
            throw new ForbiddenException("No tiene permiso para ver esta modalidad");
        }

        List<StudentModalityMember> activeMembers =
            studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                studentModalityId,
                MemberStatus.ACTIVE
            );

        User student = studentModality.getLeader();
        DegreeModality modality =
                studentModality.getProgramDegreeModality().getDegreeModality();

        StudentProfile studentProfile = studentProfileRepository.findByUserId(student.getId())
                .orElse(null);

        List<RequiredDocument> requiredDocuments =
                requiredDocumentRepository
                        .findByModalityIdAndActiveTrue(modality.getId());

        List<StudentDocument> uploadedDocuments =
                studentDocumentRepository
                        .findByStudentModalityId(studentModalityId);

        return buildStudentModalityDTO(
                        studentModality,
                        modality,
                        academicProgram,
                        student,
                        studentProfile,
                        mapMembers(activeMembers),
                        buildStatusHistory(studentModalityId),
                        buildDetailDocuments(requiredDocuments, uploadedDocuments),
                        uploadedDocuments,
                        "El director de proyecto ha propuesto una fecha de sustentación"
                );
    }

    @Transactional(readOnly = true)
    public StudentModalityDTO getStudentModalityDetailForCommittee(Long studentModalityId, User committeeMember) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        AcademicProgram academicProgram = studentModality.getProgramDegreeModality().getAcademicProgram();

        boolean authorized =
                programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRole(
                        committeeMember.getId(),
                        academicProgram.getId(),
                        ProgramRole.PROGRAM_CURRICULUM_COMMITTEE
                );

        if (!authorized) {
            throw new ForbiddenException("No tiene permiso para ver esta modalidad");
        }

        List<StudentModalityMember> activeMembers =
            studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                studentModalityId,
                MemberStatus.ACTIVE
            );

        User student = studentModality.getLeader();
        DegreeModality modality =
                studentModality.getProgramDegreeModality().getDegreeModality();

        StudentProfile studentProfile = studentProfileRepository.findByUserId(student.getId())
                .orElse(null);

        List<RequiredDocument> requiredDocuments =
                requiredDocumentRepository
                        .findByModalityIdAndActiveTrue(modality.getId());

        List<StudentDocument> uploadedDocuments =
                studentDocumentRepository
                        .findByStudentModalityId(studentModalityId);

        return buildStudentModalityDTO(
                        studentModality,
                        modality,
                        academicProgram,
                        student,
                        studentProfile,
                        mapMembers(activeMembers),
                        buildStatusHistory(studentModalityId),
                        buildDetailDocuments(requiredDocuments, uploadedDocuments),
                        uploadedDocuments,
                        "El director de proyecto ha propuesto una fecha de sustentación"
                );
    }

    @Transactional(readOnly = true)
    public StudentModalityDTO getStudentModalityDetailForProjectDirector(Long studentModalityId, User projectDirector) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        if (studentModality.getProjectDirector() == null ||
                !studentModality.getProjectDirector().getId().equals(projectDirector.getId())) {
            throw new ForbiddenException("No tiene permiso para ver esta modalidad. No es el director asignado.");
        }

        List<StudentModalityMember> activeMembers =
            studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                studentModalityId,
                MemberStatus.ACTIVE
            );

        User student = studentModality.getLeader();

        AcademicProgram academicProgram = studentModality.getProgramDegreeModality().getAcademicProgram();

        DegreeModality modality = studentModality.getProgramDegreeModality().getDegreeModality();

        StudentProfile studentProfile = studentProfileRepository.findByUserId(student.getId())
                .orElse(null);

        List<RequiredDocument> requiredDocuments =
                requiredDocumentRepository
                        .findByModalityIdAndActiveTrue(modality.getId());

        List<StudentDocument> uploadedDocuments =
                studentDocumentRepository
                        .findByStudentModalityId(studentModalityId);

        return buildStudentModalityDTO(
                        studentModality,
                        modality,
                        academicProgram,
                        student,
                        studentProfile,
                        mapMembers(activeMembers),
                        buildStatusHistory(studentModalityId),
                        buildDetailDocuments(requiredDocuments, uploadedDocuments),
                        uploadedDocuments,
                        "Usted ha propuesto una fecha de sustentación. Pendiente de aprobación del comité."
                );
    }

    @Transactional(readOnly = true)
    public StudentModalityExaminerDTO getStudentModalityDetailForExaminer(Long studentModalityId, User examiner) {

        StudentModality studentModality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        // Verificar si el usuario es un examinador asignado a esta modalidad
        DefenseExaminer defenseExaminer = defenseExaminerRepository
                .findByStudentModalityIdAndExaminerId(studentModalityId, examiner.getId())
                .orElse(null);

        if (defenseExaminer == null) {
            throw new ForbiddenException("No tiene permiso para ver esta modalidad. No está asignado como examinador.");
        }

        // Obtener todos los miembros activos de la modalidad
        List<StudentModalityMember> activeMembers =
            studentModalityMemberRepository.findByStudentModalityIdAndStatus(
                studentModalityId,
                MemberStatus.ACTIVE
            );

        AcademicProgram academicProgram = studentModality.getProgramDegreeModality().getAcademicProgram();

        // Usar el líder como estudiante principal
        User student = studentModality.getLeader();
        DegreeModality modality = studentModality.getProgramDegreeModality().getDegreeModality();

        // Información del perfil del estudiante
        StudentProfile studentProfile = studentProfileRepository.findByUserId(student.getId())
                .orElse(null);

        // Documentos requeridos y cargados
        // Para la vista del examinador, solo se muestran documentos (MANDATORY o SECONDARY) que requieren evaluación de propuesta
        List<RequiredDocument> requiredDocuments =
                requiredDocumentRepository
                        .findByModalityIdAndActiveTrue(modality.getId())
                        .stream()
                        .filter(req -> (req.getDocumentType() == DocumentType.MANDATORY || req.getDocumentType() == DocumentType.SECONDARY)
                                && Boolean.TRUE.equals(req.isRequiresProposalEvaluation()))
                        .toList();

        List<StudentDocument> uploadedDocuments =
                studentDocumentRepository
                        .findByStudentModalityId(studentModalityId);

        List<DetailDocumentDTO> documents =
                buildDetailDocuments(requiredDocuments, uploadedDocuments);

        // Historial de estados
        List<ModalityStatusHistoryDTO> history = buildStatusHistory(studentModalityId);

        // Estadísticas de documentos (solo sobre los documentos evaluables por el jurado)
        List<StudentDocument> evaluableUploadedDocs = uploadedDocuments.stream()
                .filter(d -> {
                    RequiredDocument reqDoc = d.getDocumentConfig();
                    return (reqDoc.getDocumentType() == DocumentType.MANDATORY || reqDoc.getDocumentType() == DocumentType.SECONDARY)
                            && Boolean.TRUE.equals(reqDoc.isRequiresProposalEvaluation());
                })
                .toList();

        long approvedDocs = evaluableUploadedDocs.stream()
                .filter(d -> d.getStatus() == DocumentStatus.ACCEPTED_FOR_EXAMINER_REVIEW)
                .count();
        long pendingDocs = evaluableUploadedDocs.stream()
                .filter(d -> d.getStatus() == DocumentStatus.PENDING ||
                        d.getStatus() == DocumentStatus.ACCEPTED_FOR_PROGRAM_HEAD_REVIEW ||
                        d.getStatus() == DocumentStatus.ACCEPTED_FOR_PROGRAM_CURRICULUM_COMMITTEE_REVIEW ||
                        d.getStatus() == DocumentStatus.CORRECTION_RESUBMITTED)
                .count();
        long rejectedDocs = evaluableUploadedDocs.stream()
                .filter(d -> d.getStatus() == DocumentStatus.REJECTED_FOR_EXAMINER_REVIEW ||
                        d.getStatus() == DocumentStatus.CORRECTIONS_REQUESTED_BY_EXAMINER)
                .count();

        // Obtener todos los examinadores asignados
        List<DefenseExaminer> allExaminers = defenseExaminerRepository
                .findByStudentModalityId(studentModalityId);

        List<StudentModalityExaminerDTO.ExaminerInfo> examinersList = allExaminers.stream()
                .map(de -> {
                    // Verificar si este examinador ya evaluó
                    boolean hasEvaluated = defenseEvaluationCriteriaRepository
                            .findByDefenseExaminerId(de.getId())
                            .isPresent();

                    return StudentModalityExaminerDTO.ExaminerInfo.builder()
                            .examinerId(de.getExaminer().getId())
                            .examinerName(de.getExaminer().getName() + " " + de.getExaminer().getLastName())
                            .examinerEmail(de.getExaminer().getEmail())
                            .examinerType(de.getExaminerType().name())
                            .assignmentDate(de.getAssignmentDate())
                            .hasEvaluated(hasEvaluated)
                            .build();
                })
                .toList();

        // Obtener la evaluación del examinador actual (si existe)
        StudentModalityExaminerDTO.ExaminerEvaluationInfo myEvaluationInfo = null;
        boolean hasEvaluated = false;

        DefenseEvaluationCriteria myEvaluation = defenseEvaluationCriteriaRepository
                .findByDefenseExaminerId(defenseExaminer.getId())
                .orElse(null);

        if (myEvaluation != null) {
            hasEvaluated = true;
            myEvaluationInfo = StudentModalityExaminerDTO.ExaminerEvaluationInfo.builder()
                    .evaluationId(myEvaluation.getId())
                    .grade(myEvaluation.getGrade())
                    .decision(myEvaluation.getGrade() != null ? (myEvaluation.getGrade() >= 3.5 ? "APPROVED" : "REJECTED") : null)
                    .observations(myEvaluation.getObservations())
                    .evaluationDate(myEvaluation.getEvaluatedAt())
                    .isFinalDecision(myEvaluation.getIsFinalDecision())
                    .build();
        }

        // Determinar permisos y acciones
        ModalityProcessStatus status = studentModality.getStatus();

        // El examinador puede evaluar si la defensa está programada o si es jurado de desempate y necesita evaluar
        boolean canEvaluate = (status == ModalityProcessStatus.DEFENSE_SCHEDULED && !hasEvaluated) ||
                             (defenseExaminer.getExaminerType() == ExaminerType.TIEBREAKER_EXAMINER && !hasEvaluated);

        boolean requiresAction = canEvaluate;

        boolean defenseCompleted = status == ModalityProcessStatus.GRADED_APPROVED ||
                                  status == ModalityProcessStatus.GRADED_FAILED;

        // Director del proyecto
        User projectDirector = studentModality.getProjectDirector();

        return StudentModalityExaminerDTO.builder()
                        // Información del estudiante
                        .studentId(student.getId())
                        .studentName(student.getName())
                        .studentLastName(student.getLastName())
                        .studentEmail(student.getEmail())
                        .studentCode(studentProfile != null ? studentProfile.getStudentCode() : null)
                        .approvedCredits(studentProfile != null ? studentProfile.getApprovedCredits() : null)
                        .gpa(studentProfile != null ? studentProfile.getGpa() : null)
                        .semester(studentProfile != null ? studentProfile.getSemester() : null)

                        // Información académica
                        .facultyName(academicProgram.getFaculty().getName())
                        .academicProgramName(academicProgram.getName())

                        // Información de la modalidad
                        .studentModalityId(studentModality.getId())
                        .modalityName(modality.getName())
                        .modalityDescription(modality.getDescription())
                        .creditsRequired(studentModality.getProgramDegreeModality().getCreditsRequired())
                        .modalityType(studentModality.getModalityType() != null
                                ? studentModality.getModalityType().name()
                                : null)
                        .members(mapMembers(activeMembers))

                        // Estado actual
                        .currentStatus(status.name())
                        .currentStatusDescription(ModalityServiceUtils.describeModalityStatus(status))
                        .selectionDate(studentModality.getSelectionDate())
                        .lastUpdatedAt(studentModality.getUpdatedAt())

                        // Director del proyecto
                        .projectDirectorId(projectDirector != null ? projectDirector.getId() : null)
                        .projectDirectorName(projectDirector != null
                                ? projectDirector.getName() + " " + projectDirector.getLastName()
                                : null)
                        .projectDirectorEmail(projectDirector != null ? projectDirector.getEmail() : null)

                        // Información de la defensa
                        .defenseDate(studentModality.getDefenseDate())
                        .defenseLocation(studentModality.getDefenseLocation())

                        // Examinadores
                        .examiners(examinersList)
                        .myEvaluation(myEvaluationInfo)

                        // Distinción académica y calificación final
                        .academicDistinction(studentModality.getAcademicDistinction() != null
                                ? studentModality.getAcademicDistinction().name()
                                : null)
                        .finalGrade(studentModality.getFinalGrade())

                        // Documentos (solo los evaluables por el jurado: MANDATORY con requiresProposalEvaluation=true)
                        .documents(documents)
                        .totalDocuments(evaluableUploadedDocs.size())
                        .approvedDocuments((int) approvedDocs)
                        .pendingDocuments((int) pendingDocs)
                        .rejectedDocuments((int) rejectedDocs)

                        // Historial
                        .history(history)

                        // Permisos y acciones
                        .canEvaluate(canEvaluate)
                        .hasEvaluated(hasEvaluated)
                        .requiresAction(requiresAction)
                        .defenseCompleted(defenseCompleted)

                        .build();
    }

    /**
     * Devuelve la lista completa de estudiantes del programa académico al que
     * pertenece el comité autenticado, con filtro opcional por nombre del estudiante.
     * El listado se ordena por ID de usuario DESC (más reciente primero).
     *
     * El usuario autenticado se resuelve desde el SecurityContext (mismo patrón
     * que el resto del servicio), por lo que el controller no necesita extraer
     * ni pasar ningún objeto User.
     *
     * GET /modalities/committee/program-students?studentName=raul
     *
     * @param studentName (opcional) filtro parcial por nombre, apellido o nombre completo
     */
@Transactional(readOnly = true)
    public ProgramStudentsResponse getProgramStudentsForCommittee(String studentName, User currentUser) {
        try {
            // 1. Resolver usuario autenticado desde el contexto de seguridad

            // 2. Verificar que tiene rol COMMITTEE en al menos un programa
            List<ProgramAuthority> authorities = programAuthorityRepository
                    .findByUser_IdAndRole(currentUser.getId(), ProgramRole.PROGRAM_CURRICULUM_COMMITTEE);

            if (authorities.isEmpty()) {
                throw new ForbiddenException("El usuario no pertenece a ningún comité de programa académico.");
            }

            // 3. Obtener el programa académico del comité
            // Selección determinista (patrón ReportUtils.getAuthenticatedUserProgram):
            // único → ese; múltiples → prefiere PROGRAM_HEAD si hay exactamente 1; si no → error.
            AcademicProgram program;
            if (authorities.size() == 1) {
                program = authorities.get(0).getAcademicProgram();
            } else {
                List<ProgramAuthority> programHeads = authorities.stream()
                        .filter(pa -> pa.getRole() == ProgramRole.PROGRAM_HEAD)
                        .toList();
                if (programHeads.size() == 1) {
                    program = programHeads.get(0).getAcademicProgram();
                } else {
                    throw new ForbiddenException("El usuario tiene más de un programa académico asignado; no se puede determinar el programa");
                }
            }

            // 4. Obtener todos los perfiles de estudiantes del programa
            List<StudentProfile> profiles = studentProfileRepository
                    .findByAcademicProgramId(program.getId());

            // 4.1 Filtrar por nombre si se proporciona (nombre, apellido o nombre completo)
            java.util.stream.Stream<StudentProfile> profileStream = profiles.stream();
            if (studentName != null && !studentName.trim().isEmpty()) {
                String nameLower = studentName.trim().toLowerCase();
                profileStream = profileStream.filter(sp -> {
                    User u = sp.getUser();
                    String fullName = (u.getName() + " " + u.getLastName()).toLowerCase();
                    return u.getName().toLowerCase().contains(nameLower)
                            || u.getLastName().toLowerCase().contains(nameLower)
                            || fullName.contains(nameLower);
                });
            }

            // 5. Construir la respuesta — ordenado por ID de usuario DESC (más reciente arriba)
            List<ProgramStudentsResponse.StudentSummary> students = profileStream
                    .sorted(Comparator.comparing((StudentProfile sp) -> sp.getUser().getId()).reversed())
                    .map(sp -> {
                        User u = sp.getUser();

                        // Modalidades donde el estudiante es líder
                        List<StudentModality> leaderModalities =
                                studentModalityRepository.findByLeaderId(sp.getId());

                        // Modalidades donde es miembro (grupales)
                        List<StudentModality> memberModalities =
                                studentModalityMemberRepository.findActiveModalitiesByUserId(u.getId());

                        // Unión sin duplicados
                        Set<Long> seen = new HashSet<>();
                        List<StudentModality> allModalities = new ArrayList<>();
                        for (StudentModality sm : leaderModalities) {
                            if (seen.add(sm.getId())) allModalities.add(sm);
                        }
                        for (StudentModality sm : memberModalities) {
                            if (seen.add(sm.getId())) allModalities.add(sm);
                        }

                        // Modalidad activa más reciente (si existe)
                        StudentModality activeModality = allModalities.stream()
                                .filter(sm -> sm.getStatus() != ModalityProcessStatus.MODALITY_CLOSED
                                        && sm.getStatus() != ModalityProcessStatus.MODALITY_CANCELLED
                                        && sm.getStatus() != ModalityProcessStatus.GRADED_APPROVED
                                        && sm.getStatus() != ModalityProcessStatus.GRADED_FAILED)
                                .max(Comparator.comparing(StudentModality::getUpdatedAt,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                                .orElse(null);

                        return new ProgramStudentsResponse.StudentSummary(
                                u.getId(),
                                sp.getStudentCode(),
                                u.getName(),
                                u.getLastName(),
                                u.getName() + " " + u.getLastName(),
                                u.getEmail(),
                                sp.getSemester(),
                                sp.getGpa(),
                                sp.getApprovedCredits(),
                                allModalities.size(),
                                activeModality != null ? activeModality.getId() : null,
                                activeModality != null
                                        ? activeModality.getProgramDegreeModality().getDegreeModality().getName()
                                        : null,
                                activeModality != null ? activeModality.getStatus().name() : null,
                                activeModality != null
                                        ? ModalityServiceUtils.describeModalityStatus(activeModality.getStatus())
                                        : null,
                                activeModality != null && activeModality.getProjectDirector() != null
                                        ? activeModality.getProjectDirector().getName() + " "
                                          + activeModality.getProjectDirector().getLastName()
                                        : null
                        );
                    })
                    .toList();

            return new ProgramStudentsResponse(
                    true,
                    program.getId(),
                    program.getName(),
                    students.size(),
                    students
            );

        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Error al obtener estudiantes del programa para comité: {}", e.getMessage(), e);
            throw new InternalException("Error al obtener estudiantes del programa", e);
        } catch (Exception e) {
            log.error("Error inesperado al obtener estudiantes del programa: {}", e.getMessage(), e);
            throw new InternalException("Error al obtener los estudiantes", e);
        }
    }

    private ModalityListDTO toModalityList(StudentModality sm, ModalityProcessStatus status, boolean pending,
                                           List<StudentModalityMember> activeMembers) {
        String studentNames = activeMembers.stream()
                .map(member -> member.getStudent().getName() + " " + member.getStudent().getLastName())
                .collect(Collectors.joining(", "));

        String studentEmails = activeMembers.stream()
                .map(member -> member.getStudent().getEmail())
                .collect(Collectors.joining(", "));

        String studentLastNames = activeMembers.stream()
                .map(member -> member.getStudent().getLastName())
                .collect(Collectors.joining(", "));

        return ModalityListDTO.builder()
                .studentModalityId(sm.getId())
                .studentName(studentNames)
                .studentLastName(studentLastNames)
                .studentEmail(studentEmails)
                .modalityName(sm.getProgramDegreeModality().getDegreeModality().getName())
                .currentStatus(status.name())
                .currentStatusDescription(ModalityServiceUtils.describeModalityStatus(status))
                .lastUpdatedAt(sm.getUpdatedAt())
                .hasPendingActions(pending)
                .build();
    }

    private Map<Long, List<StudentModalityMember>> loadActiveMembersByModalities(List<StudentModality> modalities) {
        List<Long> modalityIds = modalities.stream()
                .map(StudentModality::getId)
                .toList();
        return studentModalityMemberRepository
                .findByStudentModalityIdInAndStatus(modalityIds, MemberStatus.ACTIVE)
                .stream()
                .collect(Collectors.groupingBy(m -> m.getStudentModality().getId()));
    }

}
