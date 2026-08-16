package com.SIGMA.USCO.notifications.controller;

import com.SIGMA.USCO.Modalities.entity.AcademicCertificate;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.common.exception.ForbiddenException;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.security.Permissions;
import com.SIGMA.USCO.notifications.service.AcademicCertificatePdfService;
import com.SIGMA.USCO.notifications.service.CertificatePdfSupport;
import com.SIGMA.USCO.security.SecurityUtils;
import com.SIGMA.USCO.shared.util.ResourceAccessPolicy;
import org.springframework.context.annotation.Profile;
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
import org.springframework.transaction.annotation.Transactional;
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
@Profile("dev")
public class AcademicCertificateTestController {

    private final AcademicCertificatePdfService certificatePdfService;
    private final StudentModalityRepository studentModalityRepository;
    private final ResourceAccessPolicy resourceAccessPolicy;

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
    @Transactional(readOnly = true)
    public ResponseEntity<InputStreamResource> generateTestCertificate(
            @Parameter(description = "ID de la modalidad del estudiante") @PathVariable Long studentModalityId) throws IOException {
        StudentModality modality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Modalidad no encontrada"));

        User current = SecurityUtils.getCurrentUser();
        boolean authorized =
                resourceAccessPolicy.tryRequire(() -> resourceAccessPolicy.requireLeader(modality, current, "No autorizado"))
                || resourceAccessPolicy.tryRequire(() -> resourceAccessPolicy.requireActiveMember(modality.getId(), current, "No autorizado"))
                || resourceAccessPolicy.tryRequire(() -> resourceAccessPolicy.requireAssignedExaminer(modality.getId(), current, "No autorizado"))
                || resourceAccessPolicy.tryRequire(() -> resourceAccessPolicy.requireProjectDirector(modality, current, "No autorizado"))
                || current.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Permissions.PERM_VIEW_REPORT))
                || resourceAccessPolicy.tryRequire(() -> resourceAccessPolicy.requireProgramAuthorityIn(current, modality.getAcademicProgram().getId(),
                        List.of(ProgramRole.PROGRAM_HEAD, ProgramRole.PROGRAM_CURRICULUM_COMMITTEE), "No autorizado"));
        if (!authorized) {
            throw new ForbiddenException("No está autorizado para descargar este certificado.");
        }

        boolean isComplete = CertificatePdfSupport.isCompleteModality(modality);

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
}

