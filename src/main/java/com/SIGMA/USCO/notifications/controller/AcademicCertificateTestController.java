package com.SIGMA.USCO.notifications.controller;

import com.SIGMA.USCO.Modalities.Entity.AcademicCertificate;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.notifications.service.AcademicCertificatePdfService;
import com.SIGMA.USCO.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Tag(name = "Certificados Académicos", description = "Generación y descarga de certificados de aprobación de modalidades de grado")
@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class AcademicCertificateTestController {

    private final AcademicCertificatePdfService certificatePdfService;
    private final StudentModalityRepository studentModalityRepository;
    private final ProgramAuthorityRepository programAuthorityRepository;

    @Operation(
            summary = "Generar certificado académico",
            description = "Genera y retorna el acta de aprobación correspondiente según el tipo de modalidad:\n" +
                    "- Completa (con sustentación, jurados y/o director) → Acta de sustentación\n" +
                    "- Simplificada (aprobada directamente por Comité, sin sustentación ni jurados) → Acta de aprobación simplificada"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Certificado generado y descargado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Modalidad no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error al generar el certificado")
    })
    @GetMapping("/{studentModalityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> generateTestCertificate(
            @Parameter(description = "ID de la modalidad del estudiante") @PathVariable Long studentModalityId) throws IOException {
        StudentModality modality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

        User current = SecurityUtils.getCurrentUser();
        if (!isAuthorizedForCertificate(modality, current)) {
            throw new AccessDeniedException("No está autorizado para descargar este certificado.");
        }

        boolean isComplete = isCompleteModality(modality);

        AcademicCertificate certificate;
        if (isComplete) {
            certificate = certificatePdfService.generateCertificate(modality);
        } else {
            certificate = certificatePdfService.generateCertificateForCommitteeApproval(modality);
        }

        Path pdfPath = certificatePdfService.getCertificatePath(studentModalityId);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(pdfPath.toFile()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + pdfPath.getFileName())
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    /**
     * Determina si la modalidad es "completa" (tiene sustentación programada, jurados asignados
     * o director de proyecto). Si no tiene ninguno, se considera simplificada (aprobada por comité).
     */
    private boolean isCompleteModality(StudentModality modality) {
        boolean hasDefenseDate = modality.getDefenseDate() != null;
        boolean hasExaminers = modality.getDefenseExaminers() != null && !modality.getDefenseExaminers().isEmpty();
        boolean hasDirector = modality.getProjectDirector() != null;
        return hasDefenseDate || hasExaminers || hasDirector;
    }

    private boolean isAuthorizedForCertificate(StudentModality modality, User current) {
        boolean isLeader = modality.getLeader() != null
                && modality.getLeader().getId().equals(current.getId());
        boolean isDirector = modality.getProjectDirector() != null
                && modality.getProjectDirector().getId().equals(current.getId());
        boolean isExaminer = modality.getDefenseExaminers() != null
                && modality.getDefenseExaminers().stream()
                .anyMatch(de -> de.getExaminer() != null
                        && de.getExaminer().getId().equals(current.getId()));
        boolean isStaff = current.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PERM_VIEW_REPORT"));
        boolean isProgramAuthority = modality.getAcademicProgram() != null
                && programAuthorityRepository.existsByUser_IdAndAcademicProgram_IdAndRoleIn(
                current.getId(),
                modality.getAcademicProgram().getId(),
                List.of(ProgramRole.PROGRAM_HEAD, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE));
        return isLeader || isDirector || isExaminer || isStaff || isProgramAuthority;
    }
}

