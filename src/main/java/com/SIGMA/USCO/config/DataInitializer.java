package com.SIGMA.USCO.config;

import com.SIGMA.USCO.Users.entity.Permission;
import com.SIGMA.USCO.Users.entity.Role;
import com.SIGMA.USCO.Users.repository.PermissionRepository;
import com.SIGMA.USCO.Users.repository.RoleRepository;
import com.SIGMA.USCO.common.security.Permissions;
import com.SIGMA.USCO.common.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Profile("dev")
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Bean
    CommandLineRunner initData() {
        return args -> {

            // Crear permisos base
            Permission verDocumentos = createPermission("VER_DOCUMENTOS_ESTUDIANTE");
            Permission crearUsuario = createPermission("CREAR_USUARIO");
            Permission editarUsuario = createPermission("EDITAR_USUARIO");
            Permission activateOrDeactivateUser = createPermission(Permissions.PERM_ACTIVATE_OR_DEACTIVATE_USER);
            Permission createRole = createPermission(Permissions.PERM_CREATE_ROLE);
            Permission updateRole = createPermission(Permissions.PERM_UPDATE_ROLE);
            Permission assignRole = createPermission(Permissions.PERM_ASSIGN_ROLE);
            Permission createModality = createPermission(Permissions.PERM_CREATE_MODALITY);
            Permission updateModality = createPermission(Permissions.PERM_UPDATE_MODALITY);
            Permission createRequiredDocument = createPermission(Permissions.PERM_CREATE_REQUIRED_DOCUMENT);
            Permission updateRequiredDocument = createPermission(Permissions.PERM_UPDATE_REQUIRED_DOCUMENT);
            Permission reviewDocuments = createPermission(Permissions.PERM_REVIEW_DOCUMENTS);
            Permission viewDocuments = createPermission(Permissions.PERM_VIEW_DOCUMENTS);
            Permission approveModality = createPermission(Permissions.PERM_APPROVE_MODALITY);
            Permission viewAllModalities = createPermission(Permissions.PERM_VIEW_ALL_MODALITIES);
            Permission approveCancellation = createPermission(Permissions.PERM_APPROVE_CANCELLATION);
            Permission rejectCancellation = createPermission(Permissions.PERM_REJECT_CANCELLATION);
            Permission assignProjectDirector = createPermission(Permissions.PERM_ASSIGN_PROJECT_DIRECTOR);
            Permission scheduleDefense = createPermission(Permissions.PERM_SCHEDULE_DEFENSE);
            Permission viewReports = createPermission("VIEW_REPORTS");
            Permission viewCancellations = createPermission(Permissions.PERM_VIEW_CANCELLATIONS);
            Permission viewRole = createPermission(Permissions.PERM_VIEW_ROLE);
            Permission createPermission = createPermission(Permissions.PERM_CREATE_PERMISSION);
            Permission viewPermission = createPermission(Permissions.PERM_VIEW_PERMISSION);
            Permission viewUser = createPermission(Permissions.PERM_VIEW_USER);
            Permission desactiveModality = createPermission(Permissions.PERM_DESACTIVE_MODALITY);
            Permission viewModalityAdmin = createPermission(Permissions.PERM_VIEW_MODALITIES_ADMIN);
            Permission deleteRequirement = createPermission(Permissions.PERM_DELETE_MODALITY_REQUIREMENT);
            Permission deleteRequiredDocument = createPermission(Permissions.PERM_DELETE_REQUIRED_DOCUMENT);
            Permission viewRequiredDocument = createPermission(Permissions.PERM_VIEW_REQUIRED_DOCUMENT);
            Permission viewProjectDirector = createPermission(Permissions.PERM_VIEW_PROJECT_DIRECTOR);
            Permission viewFinalDefenseResult = createPermission(Permissions.PERM_VIEW_FINAL_DEFENSE_RESULT);
            Permission createFaculty = createPermission(Permissions.PERM_CREATE_FACULTY);
            Permission createProgram = createPermission(Permissions.PERM_CREATE_PROGRAM);
            Permission createProgramDegreeModality = createPermission(Permissions.PERM_CREATE_PROGRAM_DEGREE_MODALITY);
            Permission assignProgramHead = createPermission(Permissions.PERM_ASSIGN_PROGRAM_HEAD);
            Permission viewFaculties = createPermission(Permissions.PERM_VIEW_FACULTIES);
            Permission updateFaculty = createPermission(Permissions.PERM_UPDATE_FACULTY);
            Permission deleteFaculty = createPermission(Permissions.PERM_DELETE_FACULTY);
            Permission viewPrograms = createPermission(Permissions.PERM_VIEW_PROGRAMS);
            Permission updateProgram = createPermission(Permissions.PERM_UPDATE_PROGRAM);
            Permission viewProgramHead = createPermission(Permissions.PERM_VIEW_PROGRAM_HEAD);
            Permission viewCommitteeMembers = createPermission(Permissions.PERM_VIEW_COMMITTEE);
            Permission viewExaminers = createPermission("VIEW_EXAMINERS");
            Permission createUser = createPermission(Permissions.PERM_CREATE_USER);
            Permission viewModality = createPermission(Permissions.PERM_VIEW_MODALITY);
            Permission viewExaminer = createPermission(Permissions.PERM_VIEW_EXAMINER);
            Permission proposeDefense = createPermission(Permissions.PERM_PROPOSE_DEFENSE);
            Permission approveCancellationByProjectDirector = createPermission(Permissions.PERM_APPROVE_CANCELLATION_DIRECTOR);
            Permission assignExaminer = createPermission(Permissions.PERM_ASSIGN_EXAMINER);
            Permission evaluateDefense = createPermission(Permissions.PERM_EVALUATE_DEFENSE);
            Permission viewExaminerModalities = createPermission(Permissions.PERM_VIEW_EXAMINER_MODALITIES);
            Permission approveModalityByCommittee = createPermission(Permissions.PERM_APPROVE_MODALITY_BY_COMMITTEE);
            Permission rejectModalityByCommittee = createPermission(Permissions.PERM_REJECT_MODALITY_BY_COMMITTEE);
            Permission createSeminar = createPermission(Permissions.PERM_CREATE_SEMINAR);
            Permission viewReport = createPermission(Permissions.PERM_VIEW_REPORT);
            Permission approvModalityByExaminer = createPermission(Permissions.PERM_APPROVE_MODALITY_BY_EXAMINER);
            Permission approveFinalModalityByExaminer = createPermission("APPROVE_FINAL_MODALITY_BY_EXAMINER");
            Permission viewExaminerEvaluation = createPermission("VIEW_EXAMINER_EVALUATION");
            Permission studentList = createPermission(Permissions.PERM_STUDENT_LIST);
            Permission startModality = createPermission(Permissions.PERM_START_MODALITY);
            Permission uploadDocument = createPermission(Permissions.PERM_UPLOAD_DOCUMENT);
            Permission requestCancellation = createPermission(Permissions.PERM_REQUEST_CANCELLATION);
            Permission requestEdit = createPermission(Permissions.PERM_REQUEST_EDIT);
            Permission viewResult = createPermission(Permissions.PERM_VIEW_RESULT);





            // Crear roles y asignar permisos
            createRole(Roles.ROLE_SUPERADMIN, Set.of(verDocumentos, crearUsuario, editarUsuario, activateOrDeactivateUser, createRole, updateRole, assignRole, createModality, updateModality, createRequiredDocument, updateRequiredDocument, reviewDocuments, viewDocuments, approveModality, viewAllModalities, approveCancellation, rejectCancellation, assignProjectDirector, scheduleDefense, viewReports, viewCancellations, viewRole, createPermission, viewPermission, viewUser, desactiveModality, viewModalityAdmin, deleteRequirement, deleteRequiredDocument, viewRequiredDocument, viewProjectDirector, viewFinalDefenseResult, createFaculty, createProgram, createProgramDegreeModality, assignProgramHead, viewFaculties, updateFaculty, deleteFaculty, viewPrograms, updateProgram, viewProgramHead, viewCommitteeMembers, createUser, viewModality, proposeDefense, approveCancellationByProjectDirector, assignExaminer, viewExaminers, viewReport) );

            createRole(Roles.ROLE_PROGRAM_HEAD, Set.of(verDocumentos, crearUsuario, editarUsuario, activateOrDeactivateUser, createRole, updateRole, assignRole, createModality, updateModality, createRequiredDocument, updateRequiredDocument, reviewDocuments, viewDocuments, approveModality, viewAllModalities, approveCancellation, rejectCancellation, assignProjectDirector, scheduleDefense, viewReports, viewCancellations, viewRole, createPermission, viewPermission, viewUser, desactiveModality, viewModalityAdmin, deleteRequirement, deleteRequiredDocument, viewRequiredDocument, viewProjectDirector, viewFinalDefenseResult, createFaculty, createProgram, createProgramDegreeModality, assignProgramHead, viewFaculties, updateFaculty, deleteFaculty, viewPrograms, updateProgram, viewProgramHead, viewCommitteeMembers, createSeminar) );

            createRole(Roles.ROLE_PROGRAM_CURRICULUM_COMMITTEE, Set.of(verDocumentos, crearUsuario, editarUsuario, activateOrDeactivateUser, createRole, updateRole, assignRole, createModality, updateModality, createRequiredDocument, updateRequiredDocument, reviewDocuments, viewDocuments, approveModality, viewAllModalities, approveCancellation, rejectCancellation, assignProjectDirector, scheduleDefense, viewReports, viewCancellations, viewRole, createPermission, viewPermission, viewUser, desactiveModality, viewModalityAdmin, deleteRequirement, deleteRequiredDocument, viewRequiredDocument, viewProjectDirector, viewFinalDefenseResult, createFaculty, createProgram, createProgramDegreeModality, assignProgramHead, viewFaculties, updateFaculty, deleteFaculty, viewPrograms, updateProgram, viewProgramHead, viewExaminers, viewExaminer, approveModalityByCommittee, rejectModalityByCommittee, createSeminar, viewReport, viewExaminerModalities, studentList ) );

            createRole(Roles.ROLE_STUDENT, Set.of(verDocumentos, startModality, uploadDocument, requestCancellation, requestEdit, viewResult));

            createRole(Roles.ROLE_PROJECT_DIRECTOR, Set.of(verDocumentos, crearUsuario, editarUsuario, activateOrDeactivateUser, createRole, updateRole, assignRole, createModality, updateModality, createRequiredDocument, updateRequiredDocument, reviewDocuments, viewDocuments, approveModality, viewAllModalities, approveCancellation, rejectCancellation, assignProjectDirector, scheduleDefense, viewReports, viewCancellations, viewRole, createPermission, viewPermission, viewUser, desactiveModality, viewModalityAdmin, deleteRequirement, deleteRequiredDocument, viewRequiredDocument, viewProjectDirector, viewModality, proposeDefense, approveCancellationByProjectDirector) );

            createRole(Roles.ROLE_EXAMINER, Set.of(verDocumentos, crearUsuario, editarUsuario, activateOrDeactivateUser, createRole, updateRole, assignRole, createModality, updateModality, createRequiredDocument, updateRequiredDocument, reviewDocuments, viewDocuments, approveModality, viewAllModalities, approveCancellation, rejectCancellation, assignProjectDirector, scheduleDefense, viewReports, viewCancellations, viewRole, createPermission, viewPermission, viewUser, desactiveModality, viewModalityAdmin, deleteRequirement, deleteRequiredDocument, viewRequiredDocument, viewProjectDirector, viewFinalDefenseResult, createFaculty, createProgram, createProgramDegreeModality, assignProgramHead, viewFaculties, updateFaculty, deleteFaculty, viewPrograms, updateProgram, viewProgramHead, evaluateDefense, viewExaminerModalities, approvModalityByExaminer, approveFinalModalityByExaminer, viewExaminerEvaluation) );
        };
    }


    private Permission createPermission(String name) {
        // ponytail: los permisos llegan como autoridad PERM_X; se siembra el nombre base (User.java antepone PERM_ al conceder)
        String baseName = name.startsWith("PERM_") ? name.substring("PERM_".length()) : name;
        return permissionRepository.findByName(baseName)
                .orElseGet(() -> permissionRepository.save(
                        Permission.builder().name(baseName).build()
                ));
    }

    private void createRole(String name,Set<Permission> permissions) {
        if (!roleRepository.findByName(name).isPresent()) {
            roleRepository.save(
                    Role.builder()
                            .name(name)
                            .permissions(permissions)
                            .build()
            );
        }
    }

}
