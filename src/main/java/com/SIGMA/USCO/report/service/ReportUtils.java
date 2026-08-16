package com.SIGMA.USCO.report.service;

import com.SIGMA.USCO.Modalities.entity.DefenseEvaluationCriteria;
import com.SIGMA.USCO.Modalities.entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.entity.enums.MemberStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityType;
import com.SIGMA.USCO.Modalities.repository.DefenseEvaluationCriteriaRepository;
import com.SIGMA.USCO.Modalities.repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Users.entity.ProgramAuthority;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.report.dto.StudentInfoDTO;
import com.SIGMA.USCO.security.SecurityUtils;
import com.itextpdf.text.BaseColor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utilidades compartidas para los servicios de reportes
 */
public class ReportUtils {

    private static final List<ModalityProcessStatus> ACTIVE_STATUSES = Arrays.asList(
            ModalityProcessStatus.MODALITY_SELECTED,
            ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD,
            ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD,
            ModalityProcessStatus.CORRECTIONS_SUBMITTED,
            ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD,
            ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_COMMITTEE,
            ModalityProcessStatus.CORRECTIONS_SUBMITTED_TO_EXAMINERS,
            ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.READY_FOR_DIRECTOR_ASSIGNMENT,
            ModalityProcessStatus.APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE,
            ModalityProcessStatus.PROPOSAL_APPROVED,
            ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR,
            ModalityProcessStatus.DEFENSE_SCHEDULED,
            ModalityProcessStatus.EXAMINERS_ASSIGNED,
            ModalityProcessStatus.READY_FOR_EXAMINERS,
            ModalityProcessStatus.DOCUMENT_REVIEW_TIEBREAKER_REQUIRED,
            ModalityProcessStatus.READY_FOR_DEFENSE,
            ModalityProcessStatus.FINAL_REVIEW_COMPLETED,
            ModalityProcessStatus.DEFENSE_COMPLETED,
            ModalityProcessStatus.UNDER_EVALUATION_PRIMARY_EXAMINERS,
            ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER,
            ModalityProcessStatus.UNDER_EVALUATION_TIEBREAKER,
            ModalityProcessStatus.EVALUATION_COMPLETED);

    public static List<ModalityProcessStatus> getActiveStatuses() {
        return ACTIVE_STATUSES;
    }

    public static boolean isPendingStatus(ModalityProcessStatus status) {
        return status == ModalityProcessStatus.MODALITY_SELECTED ||
               status == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_HEAD ||
               status == ModalityProcessStatus.CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE ||
               status == ModalityProcessStatus.CORRECTIONS_REQUESTED_EXAMINERS ||
               status == ModalityProcessStatus.READY_FOR_PROGRAM_CURRICULUM_COMMITTEE ||
               status == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_HEAD ||
               status == ModalityProcessStatus.UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE ||
               status == ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR ||
               status == ModalityProcessStatus.EDIT_REQUESTED_BY_STUDENT;
    }

    public static boolean isAdvancedStatus(ModalityProcessStatus status) {
        return status == ModalityProcessStatus.PROPOSAL_APPROVED ||
               status == ModalityProcessStatus.DEFENSE_SCHEDULED ||
               status == ModalityProcessStatus.EXAMINERS_ASSIGNED ||
               status == ModalityProcessStatus.DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR ||
               status == ModalityProcessStatus.READY_FOR_DEFENSE ||
               status == ModalityProcessStatus.FINAL_REVIEW_COMPLETED ||
               status == ModalityProcessStatus.DEFENSE_COMPLETED ||
               status == ModalityProcessStatus.UNDER_EVALUATION_PRIMARY_EXAMINERS ||
               status == ModalityProcessStatus.DISAGREEMENT_REQUIRES_TIEBREAKER ||
               status == ModalityProcessStatus.UNDER_EVALUATION_TIEBREAKER ||
               status == ModalityProcessStatus.EVALUATION_COMPLETED;
    }

    public static String translatePerformanceVerdict(String verdict) {
        if (verdict == null) return "Sin Evaluar";
        switch (verdict) {
            case "EXCELLENT": return "EXCELENTE";
            case "GOOD": return "BUENO";
            case "REGULAR": return "REGULAR";
            case "NEEDS_IMPROVEMENT": return "NECESITA MEJORA";
            default: return verdict;
        }
    }

    public static BaseColor getPerformanceColor(String verdict) {
        if (verdict == null) return InstitutionalPdfHeader.LIGHT_GOLD;
        switch (verdict) {
            case "EXCELLENT": return InstitutionalPdfHeader.INST_GOLD;
            case "GOOD": return InstitutionalPdfHeader.INST_GOLD;
            case "REGULAR": return InstitutionalPdfHeader.INST_RED;
            case "NEEDS_IMPROVEMENT": return InstitutionalPdfHeader.INST_RED;
            default: return InstitutionalPdfHeader.LIGHT_GOLD;
        }
    }

    public static List<StudentInfoDTO> buildStudentInfos(List<StudentModalityMember> members, StudentProfileRepository profileRepo) {
        List<Long> userIds = members.stream().map(member -> member.getStudent().getId()).distinct().toList();
        return buildStudentInfos(members, loadProfilesByUserIds(userIds, profileRepo));
    }

    public static List<StudentInfoDTO> buildStudentInfos(List<StudentModalityMember> members, Map<Long, StudentProfile> profilesByUserId) {
        return members.stream().map(member -> {
            User student = member.getStudent();
            StudentProfile profile = profilesByUserId.get(student.getId());
            return StudentInfoDTO.builder()
                    .studentId(student.getId())
                    .fullName(student.getName() + " " + student.getLastName())
                    .studentCode(profile != null ? profile.getStudentCode() : "N/A")
                    .email(student.getEmail())
                    .semester(profile != null ? profile.getSemester() : null)
                    .gpa(profile != null ? profile.getGpa() : null)
                    .isLeader(member.getIsLeader())
                    .build();
        }).toList();
    }

    public static Map<Long, StudentProfile> loadProfilesByUserIds(List<Long> userIds, StudentProfileRepository profileRepo) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return profileRepo.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(StudentProfile::getId, p -> p));
    }

    public static Map<Long, List<StudentModalityMember>> loadActiveMembersByModalityIds(
            List<Long> modalityIds, StudentModalityMemberRepository memberRepo) {
        if (modalityIds == null || modalityIds.isEmpty()) return Map.of();
        return memberRepo.findByStudentModalityIdInAndStatus(modalityIds, MemberStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(m -> m.getStudentModality().getId()));
    }

    public static Map<Long, List<DefenseExaminer>> loadExaminersByModalityIds(
            List<Long> modalityIds, DefenseExaminerRepository examinerRepo) {
        if (modalityIds == null || modalityIds.isEmpty()) return Map.of();
        return examinerRepo.findByStudentModalityIdIn(modalityIds).stream()
                .collect(Collectors.groupingBy(de -> de.getStudentModality().getId()));
    }

    public static Map<Long, DefenseEvaluationCriteria> loadCriteriaByExaminerIds(
            List<Long> examinerIds, DefenseEvaluationCriteriaRepository criteriaRepo) {
        if (examinerIds == null || examinerIds.isEmpty()) return Map.of();
        return criteriaRepo.findByDefenseExaminerIdIn(examinerIds).stream()
                .collect(Collectors.toMap(c -> c.getDefenseExaminer().getId(), c -> c));
    }

    public static String translateSessionType(ModalityType type) {
        if (type == null) return "Individual";
        return switch (type) {
            case INDIVIDUAL -> "Individual";
            case GROUP -> "Grupal";
        };
    }

    public static AcademicProgram getAuthenticatedUserProgram(ProgramAuthorityRepository programAuthorityRepository) {
        User user = SecurityUtils.getCurrentUser();
        List<ProgramAuthority> authorities = programAuthorityRepository.findByUser_Id(user.getId());
        if (authorities.isEmpty()) {
            throw new ValidationException("El usuario no tiene ningún programa académico asignado");
        }
        if (authorities.size() == 1) {
            return authorities.get(0).getAcademicProgram();
        }
        List<ProgramAuthority> programHeads = authorities.stream()
                .filter(pa -> pa.getRole() == ProgramRole.PROGRAM_HEAD)
                .toList();
        if (programHeads.size() == 1) {
            return programHeads.get(0).getAcademicProgram();
        }
        throw new ValidationException("El usuario tiene más de un programa académico asignado; no se puede determinar el programa del reporte");
    }

    public static int getSemesterFromDate(LocalDateTime date) {
        if (date == null) return 1;
        int month = date.getMonthValue();
        return month <= 6 ? 1 : 2;
    }

    public static boolean isDirectorRequired(String modalityName) {
        if (modalityName == null) {
            return true;
        }
        String normalizedName = modalityName.toUpperCase().trim();
        return !(normalizedName.contains("PLAN COMPLEMENTARIO") ||
                normalizedName.contains("PRODUCCIÓN ACADEMICA") ||
                normalizedName.contains("PRODUCCION ACADEMICA") ||
                normalizedName.contains("SEMINARIO"));
    }

    public static boolean filterByModalityTypeFilter(StudentModality modality, String modalityTypeFilter) {
        if (modalityTypeFilter == null || modalityTypeFilter.isEmpty()) return true;
        if ("INDIVIDUAL".equals(modalityTypeFilter)) {
            return modality.getModalityType() == ModalityType.INDIVIDUAL;
        } else if ("GROUP".equals(modalityTypeFilter)) {
            return modality.getModalityType() == ModalityType.GROUP;
        }
        return true;
    }

    public static boolean filterByModalityTypes(StudentModality modality, List<String> modalityTypes) {
        if (modalityTypes == null || modalityTypes.isEmpty()) return true;
        String modalityName = modality.getProgramDegreeModality().getDegreeModality().getName();
        return modalityTypes.contains(modalityName);
    }
}

