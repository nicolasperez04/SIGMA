package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Modalities.Entity.*;
import com.SIGMA.USCO.Modalities.Entity.enums.CertificateStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.ExaminerCertificateRepository;
import com.SIGMA.USCO.Modalities.Repository.DefenseExaminerRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.common.util.TranslationUtils;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.Users.Entity.User;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.SIGMA.USCO.common.util.TranslationUtils.translateExaminerType;
import static com.SIGMA.USCO.notifications.service.CertificatePdfSupport.*;

/**
 * Servicio para generar actas de participación de jurados en modalidades de grado.
 * Cada jurado recibe un acta que certifica su participación en todas las etapas del proceso
 * y el registro de su evaluación final en la sustentación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExaminerCertificatePdfService {

    private final ExaminerCertificateRepository certificateRepository;
    private final DefenseExaminerRepository defenseExaminerRepository;
    private final StudentModalityRepository studentModalityRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ── Paleta institucional ──────────────────────────────────────────────────
    private static final BaseColor BLUE_DARK   = new BaseColor(25, 75, 140);

    // ── Fuentes ───────────────────────────────────────────────────────────────
    private static final Font FONT_HIGHLIGHT  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    9f, BLUE_DARK);

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Genera una acta de participación para un jurado específico en una modalidad.
     * El acta certifica su participación en todas las etapas y el registro de su evaluación.
     *
     * @param studentModality La modalidad de grado
     * @param defenseExaminer El jurado participante
     * @return El certificado del jurado guardado
     * @throws IOException Si ocurre un error en la generación del PDF
     */
    @Transactional
    public ExaminerCertificate generateExaminerCertificate(StudentModality studentModality, DefenseExaminer defenseExaminer) throws IOException {
        // Refrescar la modalidad desde la BD para obtener el estado más actualizado
        StudentModality refreshedModality = studentModalityRepository.findById(studentModality.getId())
                .orElseThrow(() -> new NotFoundException("Modalidad de grado no encontrada"));
        
        log.info("Generando certificado de jurado. Modalidad ID: {}, Estado actual: {}", 
            refreshedModality.getId(), refreshedModality.getStatus());
        
        // Eliminar certificado previo si existe
        ExaminerCertificate existing = certificateRepository
                .findByModalityAndExaminer(refreshedModality.getId(), defenseExaminer.getExaminer().getId())
                .orElse(null);
        if (existing != null) {
            try {
                Path old = Paths.get(existing.getFilePath());
                if (Files.exists(old)) Files.delete(old);
                log.info("Acta anterior de jurado eliminada: {}", old);
            } catch (IOException ex) {
                log.warn("No se pudo eliminar acta anterior de jurado: {}", ex.getMessage());
            }
            certificateRepository.delete(existing);
            certificateRepository.flush();
            log.info("Registro de acta anterior de jurado eliminado de BD");
        }

        String examinerName = defenseExaminer.getExaminer().getName() + " " + defenseExaminer.getExaminer().getLastName();
        String certNumber = generateCertificateNumber("ACTA-JUR",
                refreshedModality.getProgramDegreeModality().getAcademicProgram().getId(),
                certificateRepository.findAll().stream()
                        .map(ExaminerCertificate::getCertificateNumber)
                        .collect(Collectors.toList()));

        Path outDir = Paths.get(uploadDir, "certificates",
                String.valueOf(refreshedModality.getProgramDegreeModality().getAcademicProgram().getId()),
                "examiners");
        Files.createDirectories(outDir);
        String fileName = "ACTA_JURADO_" + certNumber + "_" + defenseExaminer.getExaminer().getId() + ".pdf";
        Path filePath = outDir.resolve(fileName);

        buildPdf(filePath, refreshedModality, defenseExaminer, certNumber);
        log.info("Acta de jurado generada: {}", filePath);

        String hash = calculateFileHash(filePath);
        ExaminerCertificate cert = ExaminerCertificate.builder()
                .studentModality(refreshedModality)
                .examiner(defenseExaminer.getExaminer())
                .defenseExaminer(defenseExaminer)
                .certificateNumber(certNumber)
                .issueDate(LocalDateTime.now())
                .filePath(filePath.toString())
                .fileHash(hash)
                .status(CertificateStatus.GENERATED)
                .build();
        return certificateRepository.save(cert);
    }

    public Path getCertificatePath(Long modalityId, Long examinerId) {
        ExaminerCertificate cert = certificateRepository
                .findByModalityAndExaminer(modalityId, examinerId)
                .orElseThrow(() -> new NotFoundException("Acta de jurado no encontrada"));
        return Paths.get(cert.getFilePath());
    }

    @Transactional
    public void updateCertificateStatus(Long certificateId, CertificateStatus status) {
        ExaminerCertificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new NotFoundException("Acta de jurado no encontrada"));
        cert.setStatus(status);
        certificateRepository.save(cert);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Construcción del PDF
    // ─────────────────────────────────────────────────────────────────────────

    private void buildPdf(Path filePath, StudentModality sm, DefenseExaminer examiner, String certNumber) {
        try {
            Document doc = new Document(PageSize.A4, 50, 50, 40, 50);
            PdfWriter.getInstance(doc, new FileOutputStream(filePath.toFile()));
            doc.open();

            String facultyName = sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName();
            String programName = sm.getProgramDegreeModality().getAcademicProgram().getName();

            // ── 1. CABECERA INSTITUCIONAL
            addInstitutionalHeader(doc, facultyName, programName);

            // ── 2. LÍNEAS DIVISORAS
            addRedLine(doc);
            addSpacing(doc, 6f);

            // ── 3. TÍTULO Y NÚMERO
            Paragraph title = new Paragraph("ACTA DE PARTICIPACIÓN DE JURADO EN MODALIDAD DE GRADO", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            doc.add(title);

            Paragraph actaNum = new Paragraph("No. " + certNumber, FONT_ACTA_NUM);
            actaNum.setAlignment(Element.ALIGN_CENTER);
            actaNum.setSpacingAfter(4f);
            doc.add(actaNum);

            Paragraph issueDate = new Paragraph(
                    "Neiva, " + LocalDateTime.now().format(DATE_FMT), FONT_BODY);
            issueDate.setAlignment(Element.ALIGN_RIGHT);
            issueDate.setSpacingAfter(10f);
            doc.add(issueDate);

            addGoldLine(doc);
            addSpacing(doc, 8f);

            // ── 4. CUERPO CERTIFICATORIO
            addCertificationBody(doc, sm, examiner);
            addSpacing(doc, 12f);

            // ── 5. DATOS DEL JURADO
            addSectionHeader(doc, "I. DATOS DEL JURADO");
            addSpacing(doc, 4f);
            addExaminerDataTable(doc, examiner);
            addSpacing(doc, 10f);

            // ── 6. INFORMACIÓN DE LA MODALIDAD
            addSectionHeader(doc, "II. INFORMACIÓN DE LA MODALIDAD");
            addSpacing(doc, 4f);
            addModalityTable(doc, sm);
            addSpacing(doc, 10f);

            // ── 7. PARTICIPACIÓN Y EVALUACIÓN
            addSectionHeader(doc, "III. PARTICIPACIÓN Y EVALUACIÓN");
            addSpacing(doc, 4f);
            addParticipationTable(doc, sm, examiner);
            addSpacing(doc, 10f);

            // ── 8. DECLARACIÓN DE CUMPLIMIENTO
            addComplianceDeclaration(doc, examiner);
            addSpacing(doc, 20f);

            // ── 9. FIRMA
            addSignatureSection(doc, examiner);
            addSpacing(doc, 16f);

            // ── 10. PIE DE PÁGINA
            addGoldLine(doc);
            addSpacing(doc, 6f);
            addFooter(doc, certNumber, FONT_FOOTER);

            doc.close();
            log.info("Acta de jurado generada exitosamente: {}", filePath);
        } catch (DocumentException | IOException e) {
            log.error("Error generando acta de jurado: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el acta de jurado", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bloques de construcción
    // ─────────────────────────────────────────────────────────────────────────

    private void addCertificationBody(Document doc, StudentModality sm, DefenseExaminer examiner)
            throws DocumentException {

        User examinerUser = examiner.getExaminer();
        String examinerRole = translateExaminerType(examiner.getExaminerType());

        List<StudentModalityMember> members = sm.getMembers() != null ? sm.getMembers() : List.of();
        String studentNames;
        if (!members.isEmpty()) {
            studentNames = members.stream()
                    .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName())
                    .collect(Collectors.joining(", "));
        } else {
            studentNames = sm.getLeader().getName() + " " + sm.getLeader().getLastName();
        }

        String defDateStr = sm.getDefenseDate() != null
                ? sm.getDefenseDate().format(DATETIME_FMT)
                : "la fecha registrada en el sistema";

        String bodyText =
            "El Programa Académico de " + sm.getProgramDegreeModality().getAcademicProgram().getName() +
            " de la Facultad de " + sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName() +
            " de la Universidad Surcolombiana, mediante el presente documento " +
            "CERTIFICA que el(la) docente y evaluador(a) Dra(o). " + examinerUser.getName() + " " + examinerUser.getLastName() +
            ", en su calidad de " + examinerRole +
            ", participó activamente en todas las etapas del proceso de evaluación de la modalidad de grado presentada por " +
            "el(los) estudiante(s) " + studentNames +
            ". Su participación incluyó la revisión exhaustiva de la documentación requerida, la asistencia a la sustentación " +
            "realizada el día " + defDateStr +
            " y el registro de su evaluación con criterios académicos rigurosos conforme a las normas " +
            "establecidas en el Acuerdo 071 de 2023 de la Universidad Surcolombiana.";

        Paragraph body = new Paragraph(bodyText, FONT_BODY);
        body.setAlignment(Element.ALIGN_JUSTIFIED);
        body.setLeading(0f, 1.4f);
        doc.add(body);
    }

    private void addExaminerDataTable(Document doc, DefenseExaminer examiner) throws DocumentException {
        User user = examiner.getExaminer();
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);

        boolean alt = false;
        addDataRow(table, "Nombre completo:", user.getName() + " " + user.getLastName(), alt = !alt);
        addDataRow(table, "Correo institucional:", user.getEmail(), alt = !alt);
        addDataRow(table, "Rol en la evaluación:", translateExaminerType(examiner.getExaminerType()), alt = !alt);
        addDataRow(table, "Fecha de asignación:", examiner.getAssignmentDate().format(DATE_FMT), alt = !alt);

        doc.add(table);
    }

    private void addModalityTable(Document doc, StudentModality sm) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);

        boolean alt = false;
        addDataRow(table, "Nombre de la modalidad:", sm.getProgramDegreeModality().getDegreeModality().getName(), alt = !alt);
        addDataRow(table, "Tipo de modalidad:", sm.getModalityType() != null ? translateModalityType(sm.getModalityType().name()) : "Individual", alt = !alt);

        List<StudentModalityMember> members = sm.getMembers() != null ? sm.getMembers() : List.of();
        String studentNames;
        if (!members.isEmpty()) {
            studentNames = members.stream()
                    .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName())
                    .collect(Collectors.joining("; "));
        } else {
            studentNames = sm.getLeader().getName() + " " + sm.getLeader().getLastName();
        }
        addDataRow(table, "Estudiante(s):", studentNames, alt = !alt);

        addDataRow(table, "Director de proyecto:", sm.getProjectDirector() != null ? sm.getProjectDirector().getName() + " " + sm.getProjectDirector().getLastName() : "No asignado", alt = !alt);
        addDataRow(table, "Fecha de sustentación:", sm.getDefenseDate() != null ? sm.getDefenseDate().format(DATETIME_FMT) : "No registrada", alt = !alt);
        addDataRow(table, "Lugar de sustentación:", sm.getDefenseLocation() != null ? sm.getDefenseLocation() : "No registrado", alt = !alt);

        doc.add(table);
    }

    private void addParticipationTable(Document doc, StudentModality sm, DefenseExaminer examiner) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);

        boolean alt = false;

        // Etapas de participación
        String etapa1 = "✓ Asignación como " + translateExaminerType(examiner.getExaminerType());
        addDataRow(table, "Etapa I - Asignación:", etapa1, alt = !alt);

        String etapa2 = "✓ Revisión de documentación de propuesta y documentos finales";
        addDataRow(table, "Etapa II - Revisión:", etapa2, alt = !alt);

        String etapa3 = "✓ Asistencia y participación en sustentación";
        addDataRow(table, "Etapa III - Sustentación:", etapa3, alt = !alt);

        String etapa4 = "✓ Registro de evaluación y emisión de decisión";
        addDataRow(table, "Etapa IV - Evaluación:", etapa4, alt = !alt);

        // Decisión registrada
        Object statusObj = sm.getStatus();
        log.info("Estado de la modalidad en addParticipationTable - ID: {}, Status Object: {}, Status Class: {}", 
            sm.getId(), statusObj, statusObj != null ? statusObj.getClass().getSimpleName() : "null");
        
        String decisionText = statusObj != null ? TranslationUtils.translateModalityProcessStatus((ModalityProcessStatus) statusObj) : "No registrado";
        log.info("Estado traducido: {}", decisionText);
        
        addDataRow(table, "Estado final de la modalidad:", decisionText, alt = !alt);

        doc.add(table);
    }

    private void addComplianceDeclaration(Document doc, DefenseExaminer examiner) throws DocumentException {
        Paragraph declaration = new Paragraph(
            "DECLARACIÓN DE CUMPLIMIENTO\n\n",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, INST_RED)
        );
        doc.add(declaration);

        String exRole = translateExaminerType(examiner.getExaminerType());
        String text = "El(la) abajo firmante, en su calidad de " + exRole +
            ", declara bajo su responsabilidad académica que:\n\n" +
            "1. Participé activamente en todas las etapas del proceso de evaluación de esta modalidad de grado.\n\n" +
            "2. Realicé la revisión exhaustiva de los documentos y materiales presentados por el(los) estudiante(s), " +
            "conforme a los criterios académicos y reglamentarios establecidos por la Universidad Surcolombiana.\n\n" +
            "3. Asistí a la sustentación oral de la modalidad en la fecha y lugar especificados en este documento.\n\n" +
            "4. Registré mi evaluación académica de manera rigurosa e imparcial, aplicando la rúbrica de criterios " +
            "institucionales.\n\n" +
            "5. Mi participación en este proceso ha sido registrada en el sistema académico institucional  " +
            "y forma parte de la trazabilidad académica requerida por la institución.\n\n" +
            "El presente acta certifica mi participación íntegra en el proceso de evaluación de la modalidad de grado, " +
            "constituyendo evidencia formal de cumplimiento de funciones académicas y aseguramiento de calidad.";

        Paragraph compliance = new Paragraph(text, FONT_BODY);
        compliance.setAlignment(Element.ALIGN_JUSTIFIED);
        compliance.setLeading(0f, 1.3f);
        doc.add(compliance);
    }

    private void addSignatureSection(Document doc, DefenseExaminer examiner) throws DocumentException {
        PdfPTable sigTable = new PdfPTable(1);
        sigTable.setWidthPercentage(60);
        sigTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        sigTable.setSpacingBefore(10f);

        String examinerName = examiner.getExaminer().getName() + " " + examiner.getExaminer().getLastName();
        String role = translateExaminerType(examiner.getExaminerType());

        PdfPCell sigCell = buildSignatureCell(examinerName, role);
        sigTable.addCell(sigCell);

        doc.add(sigTable);
    }

    // ─────────────────────────────────────────────────────────────────────────
}
