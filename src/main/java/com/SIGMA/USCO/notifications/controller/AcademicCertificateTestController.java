package com.SIGMA.USCO.notifications.controller;

import com.SIGMA.USCO.Modalities.Entity.AcademicCertificate;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.notifications.service.AcademicCertificatePdfService;
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

@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
public class AcademicCertificateTestController {

    private final AcademicCertificatePdfService certificatePdfService;
    private final StudentModalityRepository studentModalityRepository;

    /**
     * Genera y retorna el acta de aprobación correspondiente según el tipo de modalidad:
     * - Completa (con sustentación, jurados y/o director) → generateCertificate
     * - Simplificada (aprobada directamente por Comité, sin sustentación ni jurados) → generateCertificateForCommitteeApproval
     */
    @GetMapping("/{studentModalityId}")
    public ResponseEntity<InputStreamResource> generateTestCertificate(@PathVariable Long studentModalityId) throws IOException {
        StudentModality modality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));

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
}

