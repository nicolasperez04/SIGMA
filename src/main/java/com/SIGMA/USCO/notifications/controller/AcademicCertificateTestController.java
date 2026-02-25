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

    @GetMapping("/{studentModalityId}")
    public ResponseEntity<InputStreamResource> generateTestCertificate(@PathVariable Long studentModalityId) throws IOException {
        StudentModality modality = studentModalityRepository.findById(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Modalidad no encontrada"));
        AcademicCertificate certificate = certificatePdfService.generateCertificate(modality);
        Path pdfPath = certificatePdfService.getCertificatePath(studentModalityId);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(pdfPath.toFile()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + pdfPath.getFileName())
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}

