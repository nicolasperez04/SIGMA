package com.SIGMA.USCO.config;

import com.SIGMA.USCO.Users.Entity.Permission;
import com.SIGMA.USCO.Users.Entity.Role;
import com.SIGMA.USCO.Users.repository.PermissionRepository;
import com.SIGMA.USCO.Users.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.parameters.P;

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
            Permission activateOrDeactivateUser = createPermission("ACTIVATE_OR_DEACTIVATE_USER");
            Permission createRole = createPermission("CREATE_ROLE");
            Permission updateRole = createPermission("UPDATE_ROLE");
            Permission assignRole = createPermission("ASSIGN_ROLE");
            Permission createModality = createPermission("CREATE_MODALITY");
            Permission updateModality = createPermission("UPDATE_MODALITY");
            Permission createRequiredDocument = createPermission("CREATE_REQUIRED_DOCUMENT");
            Permission updateRequiredDocument = createPermission("UPDATE_REQUIRED_DOCUMENT");
            Permission reviewDocuments = createPermission("REVIEW_DOCUMENTS");
            Permission viewDocuments = createPermission("VIEW_DOCUMENTS");
            Permission approveModality = createPermission("APPROVE_MODALITY");
            Permission viewAllModalities = createPermission("VIEW_ALL_MODALITIES");
            Permission approveCancellation = createPermission("APPROVE_CANCELLATION");
            Permission rejectCancellation = createPermission("REJECT_CANCELLATION");
            Permission assignProjectDirector = createPermission("ASSIGN_PROJECT_DIRECTOR");
            Permission scheduleDefense = createPermission("SCHEDULE_DEFENSE");
            Permission viewReports = createPermission("VIEW_REPORTS");
            Permission viewCancellations = createPermission("VIEW_CANCELLATIONS");
            Permission viewRole = createPermission("VIEW_ROLE");
            Permission createPermission = createPermission("CREATE_PERMISSION");
            Permission viewPermission = createPermission("VIEW_PERMISSION");
            Permission viewUser = createPermission("VIEW_USER");
            Permission desactiveModality = createPermission("DESACTIVE_MODALITY");
            Permission viewModalityAdmin = createPermission("VIEW_MODALITIES_ADMIN");
            Permission deleteRequirement = createPermission("DELETE_MODALITY_REQUIREMENT");
            Permission deleteRequiredDocument = createPermission("DELETE_REQUIRED_DOCUMENT");
            Permission viewRequiredDocument = createPermission("VIEW_REQUIRED_DOCUMENT");
            Permission viewProjectDirector = createPermission("VIEW_PROJECT_DIRECTOR");
            Permission viewFinalDefenseResult = createPermission("VIEW_FINAL_DEFENSE_RESULT");
            Permission createFaculty = createPermission("CREATE_FACULTY");
            Permission createProgram = createPermission("CREATE_PROGRAM");
            Permission createProgramDegreeModality = createPermission("CREATE_PROGRAM_DEGREE_MODALITY");
            Permission assignProgramHead = createPermission("ASSIGN_PROGRAM_HEAD");
            Permission viewFaculties = createPermission("VIEW_FACULTIES");
            Permission updateFaculty = createPermission("UPDATE_FACULTY");
            Permission deleteFaculty = createPermission("DELETE_FACULTY");
            Permission viewPrograms = createPermission("VIEW_PROGRAMS");
            Permission updateProgram = createPermission("UPDATE_PROGRAM");
            Permission viewProgramHead = createPermission("VIEW_PROGRAM_HEAD");
            Permission viewCommitteeMembers = createPermission("VIEW_COMMITTEE");
            Permission viewExaminers = createPermission("VIEW_EXAMINERS");
            Permission createUser = createPermission("CREATE_USER");
            Permission viewModality = createPermission("VIEW_MODALITY");
            Permission viewExaminer = createPermission("VIEW_EXAMINER");
            Permission proposeDefense = createPermission("PROPOSE_DEFENSE");
            Permission approveCancellationByProjectDirector = createPermission("APPROVE_CANCELLATION_DIRECTOR");
            Permission assignExaminer = createPermission("ASSIGN_EXAMINER");
            Permission evaluateDefense = createPermission("EVALUATE_DEFENSE");
            Permission viewExaminerModalities = createPermission("VIEW_EXAMINER_MODALITIES");
            Permission approveModalityByCommittee = createPermission("APPROVE_MODALITY_BY_COMMITTEE");
            Permission rejectModalityByCommittee = createPermission("REJECT_MODALITY_BY_COMMITTEE");
            Permission createSeminar = createPermission("CREATE_SEMINAR");
            Permission viewReport = createPermission("VIEW_REPORT");
            Permission approvModalityByExaminer = createPermission("APPROVE_MODALITY_BY_EXAMINER");
            Permission approveFinalModalityByExaminer = createPermission("APPROVE_FINAL_MODALITY_BY_EXAMINER");
            Permission viewExaminerEvaluation = createPermission("VIEW_EXAMINER_EVALUATION");
            Permission studentList = createPermission("STUDENT_LIST");





            // Crear roles y asignar permisos
            createRole("SUPERADMIN", Set.of(verDocumentos, crearUsuario, editarUsuario, activateOrDeactivateUser, createRole, updateRole, assignRole, createModality, updateModality, createRequiredDocument, updateRequiredDocument, reviewDocuments, viewDocuments, approveModality, viewAllModalities, approveCancellation, rejectCancellation, assignProjectDirector, scheduleDefense, viewReports, viewCancellations, viewRole, createPermission, viewPermission, viewUser, desactiveModality, viewModalityAdmin, deleteRequirement, deleteRequiredDocument, viewRequiredDocument, viewProjectDirector, viewFinalDefenseResult, createFaculty, createProgram, createProgramDegreeModality, assignProgramHead, viewFaculties, updateFaculty, deleteFaculty, viewPrograms, updateProgram, viewProgramHead, viewCommitteeMembers, createUser, viewModality, proposeDefense, approveCancellationByProjectDirector, assignExaminer, viewExaminers, viewReport) );

            createRole("PROGRAM_HEAD", Set.of(verDocumentos, crearUsuario, editarUsuario, activateOrDeactivateUser, createRole, updateRole, assignRole, createModality, updateModality, createRequiredDocument, updateRequiredDocument, reviewDocuments, viewDocuments, approveModality, viewAllModalities, approveCancellation, rejectCancellation, assignProjectDirector, scheduleDefense, viewReports, viewCancellations, viewRole, createPermission, viewPermission, viewUser, desactiveModality, viewModalityAdmin, deleteRequirement, deleteRequiredDocument, viewRequiredDocument, viewProjectDirector, viewFinalDefenseResult, createFaculty, createProgram, createProgramDegreeModality, assignProgramHead, viewFaculties, updateFaculty, deleteFaculty, viewPrograms, updateProgram, viewProgramHead, viewCommitteeMembers, createSeminar) );

            createRole("PROGRAM_CURRICULUM_COMMITTEE", Set.of(verDocumentos, crearUsuario, editarUsuario, activateOrDeactivateUser, createRole, updateRole, assignRole, createModality, updateModality, createRequiredDocument, updateRequiredDocument, reviewDocuments, viewDocuments, approveModality, viewAllModalities, approveCancellation, rejectCancellation, assignProjectDirector, scheduleDefense, viewReports, viewCancellations, viewRole, createPermission, viewPermission, viewUser, desactiveModality, viewModalityAdmin, deleteRequirement, deleteRequiredDocument, viewRequiredDocument, viewProjectDirector, viewFinalDefenseResult, createFaculty, createProgram, createProgramDegreeModality, assignProgramHead, viewFaculties, updateFaculty, deleteFaculty, viewPrograms, updateProgram, viewProgramHead, viewExaminers, viewExaminer, approveModalityByCommittee, rejectModalityByCommittee, createSeminar, viewReport, viewExaminerModalities, studentList ) );

            createRole("STUDENT", Set.of(verDocumentos));

            createRole("PROJECT_DIRECTOR", Set.of(verDocumentos, crearUsuario, editarUsuario, activateOrDeactivateUser, createRole, updateRole, assignRole, createModality, updateModality, createRequiredDocument, updateRequiredDocument, reviewDocuments, viewDocuments, approveModality, viewAllModalities, approveCancellation, rejectCancellation, assignProjectDirector, scheduleDefense, viewReports, viewCancellations, viewRole, createPermission, viewPermission, viewUser, desactiveModality, viewModalityAdmin, deleteRequirement, deleteRequiredDocument, viewRequiredDocument, viewProjectDirector, viewModality, proposeDefense, approveCancellationByProjectDirector) );

            createRole("EXAMINER", Set.of(verDocumentos, crearUsuario, editarUsuario, activateOrDeactivateUser, createRole, updateRole, assignRole, createModality, updateModality, createRequiredDocument, updateRequiredDocument, reviewDocuments, viewDocuments, approveModality, viewAllModalities, approveCancellation, rejectCancellation, assignProjectDirector, scheduleDefense, viewReports, viewCancellations, viewRole, createPermission, viewPermission, viewUser, desactiveModality, viewModalityAdmin, deleteRequirement, deleteRequiredDocument, viewRequiredDocument, viewProjectDirector, viewFinalDefenseResult, createFaculty, createProgram, createProgramDegreeModality, assignProgramHead, viewFaculties, updateFaculty, deleteFaculty, viewPrograms, updateProgram, viewProgramHead, evaluateDefense, viewExaminerModalities, approvModalityByExaminer, approveFinalModalityByExaminer, viewExaminerEvaluation) );
        };
    }


    private Permission createPermission(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(
                        Permission.builder().name(name).build()
                ));
    }

    private void createRole(String name,Set<Permission> permissions) {
        Role role = roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(name)
                                .permissions(permissions)
                                .build()
                ));
        role.setPermissions(permissions);
        roleRepository.save(role);
    }

}
