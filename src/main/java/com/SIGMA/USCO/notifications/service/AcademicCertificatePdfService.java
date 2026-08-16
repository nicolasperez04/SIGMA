package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Modalities.entity.AcademicCertificate;
import com.SIGMA.USCO.Modalities.entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.entity.enums.CertificateStatus;
import com.SIGMA.USCO.Modalities.repository.AcademicCertificateRepository;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.SIGMA.USCO.notifications.service.CertificatePdfSupport.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicCertificatePdfService {

    private final AcademicCertificateRepository certificateRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ── Paleta institucional ──────────────────────────────────────────────────
    /** Verde oscuro para menciones positivas */
    private static final BaseColor GREEN_DARK  = new BaseColor(30, 100, 30);

    // ── Fuentes ───────────────────────────────────────────────────────────────
    private static final Font FONT_DISTINCTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   10f, GREEN_DARK);
    private static final Font FONT_VERIF       = FontFactory.getFont(FontFactory.COURIER_BOLD,       7f, GRAY_MID);

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AcademicCertificate generateCertificate(StudentModality studentModality) throws IOException {
        return generate(studentModality, "ACTA_", false);
    }

    /**
     * Genera un certificado simplificado para modalidades aprobadas directamente por el Comité
     * de Currículo (sin sustentación, sin jurados, sin director, sin calificación final).
     */
    @Transactional
    public AcademicCertificate generateCertificateForCommitteeApproval(StudentModality studentModality) throws IOException {
        return generate(studentModality, "ACTA_COMITE_", true);
    }

    private AcademicCertificate generate(StudentModality studentModality, String filePrefix, boolean simplified) throws IOException {
        Long programId = studentModality.getProgramDegreeModality().getAcademicProgram().getId();
        int year = LocalDateTime.now().getYear();
        Optional<String> currentMax = certificateRepository
                .findTopByCertificateNumberStartingWithOrderByCertificateNumberDesc(
                        "ACTA-PROG" + programId + "-" + year + "-");
        String certNumber = generateCertificateNumber("ACTA-PROG", programId, currentMax);

        // Eliminar certificado previo si existe
        AcademicCertificate existing = certificateRepository.findByStudentModalityId(studentModality.getId()).orElse(null);
        if (existing != null) {
            try {
                Path old = Paths.get(existing.getFilePath());
                if (Files.exists(old)) Files.delete(old);
                log.info("PDF anterior eliminado: {}", old);
            } catch (IOException ex) {
                log.warn("No se pudo eliminar PDF anterior: {}", ex.getMessage());
            }
            certificateRepository.delete(existing);
            certificateRepository.flush();
            log.info("Registro de certificado antiguo eliminado de BD");
        }

        User leader = studentModality.getLeader();
        Path outDir = Paths.get(uploadDir, "certificates", String.valueOf(programId));
        Files.createDirectories(outDir);
        String fileName = filePrefix + certNumber + "_" + leader.getId() + ".pdf";
        Path filePath = outDir.resolve(fileName);

        if (simplified) {
            buildSimplifiedPdf(filePath, studentModality, certNumber);
            log.info("Certificado simplificado (comité) PDF generado: {}", filePath);
        } else {
            buildPdf(filePath, studentModality, certNumber);
            log.info("Certificado PDF generado: {}", filePath);
        }

        String hash = calculateFileHash(filePath);
        AcademicCertificate cert = AcademicCertificate.builder()
                .studentModality(studentModality)
                .certificateNumber(certNumber)
                .issueDate(LocalDateTime.now())
                .filePath(filePath.toString())
                .fileHash(hash)
                .status(CertificateStatus.GENERATED)
                .build();
        return certificateRepository.save(cert);
    }

    public Path getCertificatePath(Long studentModalityId) {
        AcademicCertificate cert = certificateRepository.findByStudentModalityId(studentModalityId)
                .orElseThrow(() -> new NotFoundException("Certificado no encontrado para la modalidad " + studentModalityId));
        return Paths.get(cert.getFilePath());
    }

    @Transactional
    public void updateCertificateStatus(Long certificateId, CertificateStatus status) {
        AcademicCertificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new NotFoundException("Certificado no encontrado"));
        cert.setStatus(status);
        certificateRepository.save(cert);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generación del PDF (formato institucional USCO)
    // ─────────────────────────────────────────────────────────────────────────

    private void buildPdf(Path filePath, StudentModality sm, String certNumber) {
        User director = sm.getProjectDirector();
        String facultyName  = sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName();
        String programName  = sm.getProgramDegreeModality().getAcademicProgram().getName();
        String modalityName = sm.getProgramDegreeModality().getDegreeModality().getName();
        buildDocument(filePath, "ACTA DE APROBACIÓN DE MODALIDAD DE GRADO", certNumber, FONT_VERIF,
                facultyName, programName, "Error al generar el certificado PDF", doc -> {
            addCertificationBody(doc, sm, modalityName);
            addSpacing(doc, 12f);
            addSectionHeader(doc, "I. DATOS DEL GRADUANDO");
            addSpacing(doc, 4f);
            addStudentsTable(doc, sm);
            addSpacing(doc, 10f);
            addSectionHeader(doc, "II. INFORMACIÓN DE LA MODALIDAD");
            addSpacing(doc, 4f);
            addModalityTable(doc, sm, director);
            addSpacing(doc, 10f);
            addSectionHeader(doc, "III. RESULTADO DE LA EVALUACIÓN");
            addSpacing(doc, 4f);
            addResultTable(doc, sm);
            addSpacing(doc, 14f);
            addResolutiveNote(doc, sm);
            addSpacing(doc, 20f);
            addSignaturesSection(doc, sm, director);
            addSpacing(doc, 16f);
        });
        log.info("PDF institucional generado: {}", filePath);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bloques de construcción del documento
    // ─────────────────────────────────────────────────────────────────────────

    /** Cuerpo certificatorio con texto formal de la institución */
    private void addCertificationBody(Document doc, StudentModality sm, String modalityName)
            throws DocumentException {

        // Construir listado de estudiantes para el texto
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

        String gradeStr = sm.getFinalGrade() != null
                ? String.format("%.1f", sm.getFinalGrade())
                : "registrada en el sistema";

        String distinctionStr = "";
        AcademicDistinction dist = sm.getAcademicDistinction();
        if (dist != null && dist != AcademicDistinction.NO_DISTINCTION) {
            // ponytail: vocabulario unificado TranslationUtils (T6.10b/T7.12)
            distinctionStr = " con la distinción académica: " + TranslationUtils.translateAcademicDistinction(dist);
        }

        String bodyText =
            "El Programa Acad\u00e9mico de " + sm.getProgramDegreeModality().getAcademicProgram().getName() +
            " de la Facultad de " + sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName() +
            " de la Universidad Surcolombiana, mediante el presente documento " +
            "CERTIFICA que el (los) estudiante(s) " + studentNames +
            " sustent\u00f3(aron) y aprob\u00f3(aron) satisfactoriamente la modalidad de grado:\n\n" +
            "            \"" + modalityName + "\"\n\n" +
            "realizada el d\u00eda " + defDateStr +
            ", obteniendo una calificaci\u00f3n de " + gradeStr + " (sobre 5.0)" + distinctionStr +
            ", cumpliendo as\u00ed con todos los requisitos acad\u00e9micos y reglamentarios establecidos " +
            "por el Acuerdo 071 de 2023 y dem\u00e1s normas vigentes de la Universidad Surcolombiana.";

        Paragraph body = new Paragraph(bodyText, FONT_BODY);
        body.setAlignment(Element.ALIGN_JUSTIFIED);
        body.setLeading(0f, 1.4f);
        doc.add(body);
    }

    /** Tabla de datos de los estudiantes */
    private void addStudentsTable(Document doc, StudentModality sm) throws DocumentException {
        List<StudentModalityMember> members = sm.getMembers() != null ? sm.getMembers() : List.of();

        // ponytail: perfiles en 1 query (findAllByUserIdIn) en vez de 1 findByUserId por miembro
        Map<Long, StudentProfile> profilesByUserId = loadProfilesByUserIds(
                Stream.concat(Stream.of(sm.getLeader()), members.stream().map(StudentModalityMember::getStudent))
                        .map(User::getId)
                        .toList());

        if (members.isEmpty()) {
            // Solo el líder
            addSingleStudentRows(doc, sm, sm.getLeader(), true, profilesByUserId);
        } else {
            boolean first = true;
            for (StudentModalityMember m : members) {
                if (!first) {
                    addSpacing(doc, 4f);
                    // Separador
                    addThinGoldLine(doc);
                    addSpacing(doc, 4f);
                }
                addSingleStudentRows(doc, sm, m.getStudent(), m.getIsLeader() != null && m.getIsLeader(), profilesByUserId);
                first = false;
            }
        }
    }

    private Map<Long, StudentProfile> loadProfilesByUserIds(List<Long> userIds) {
        try {
            return studentProfileRepository.findAllByUserIdIn(userIds).stream()
                    .collect(Collectors.toMap(p -> p.getId(), p -> p, (a, b) -> a));
        } catch (Exception e) {
            log.warn("No se pudieron obtener los códigos de los estudiantes: {}", e.getMessage());
            return Map.of();
        }
    }

    private void addSingleStudentRows(Document doc, StudentModality sm, User student, boolean isLeader,
                                      Map<Long, StudentProfile> profilesByUserId)
            throws DocumentException {

        String studentCode = "No registrado";
        StudentProfile profile = profilesByUserId.get(student.getId());
        if (profile != null && profile.getStudentCode() != null) {
            studentCode = profile.getStudentCode();
        }

        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);

        boolean alt = false;
        addDataRow(table, "Nombre completo:", student.getName() + " " + student.getLastName()
                + (isLeader ? " (Líder)" : ""), alt = !alt);
        addDataRow(table, "Código estudiantil:", studentCode, alt = !alt);
        addDataRow(table, "Correo institucional:", student.getEmail(), alt = !alt);
        addDataRow(table, "Programa académico:", sm.getProgramDegreeModality().getAcademicProgram().getName(), alt = !alt);
        addDataRow(table, "Facultad:", sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName(), alt = !alt);

        doc.add(table);
    }

    /** Tabla de información de la modalidad */
    private void addModalityTable(Document doc, StudentModality sm, User director) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);

        boolean alt = false;
        addDataRow(table, "Nombre de la modalidad:", sm.getProgramDegreeModality().getDegreeModality().getName(), alt = !alt);
        addDataRow(table, "Tipo de modalidad:", sm.getModalityType() != null ? translateModalityType(sm.getModalityType().name()) : "Individual", alt = !alt);
        addDataRow(table, "Director de proyecto:", director != null ? director.getName() + " " + director.getLastName() : "No asignado", alt = !alt);
        addDataRow(table, "Fecha de sustentación:", sm.getDefenseDate() != null ? sm.getDefenseDate().format(DATETIME_FMT) : "No registrada", alt = !alt);
        addDataRow(table, "Lugar de sustentación:", sm.getDefenseLocation() != null ? sm.getDefenseLocation() : "No registrado", alt = !alt);

        // Jurados asignados
        String examinersStr = buildExaminersString(sm);
        addDataRow(table, "Jurado(s) evaluador(es):", examinersStr, alt = !alt);

        doc.add(table);
    }

    /** Tabla del resultado de la evaluación */
    private void addResultTable(Document doc, StudentModality sm) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);

        boolean alt = false;
        addDataRow(table, "Estado de la modalidad:", sm.getStatus() != null ? TranslationUtils.translateModalityProcessStatus(sm.getStatus()) : "No registrado", alt = !alt);

        // Nota final
        String grade = sm.getFinalGrade() != null ? String.format("%.1f / 5.0", sm.getFinalGrade()) : "No registrada";
        addDataRow(table, "Calificación final:", grade, alt = !alt);

        // Mención académica
        AcademicDistinction dist = sm.getAcademicDistinction();
        if (dist != null && dist != AcademicDistinction.NO_DISTINCTION) {
            // Fila especial con fuente verde
            PdfPCell lblCell = buildLabelCell(alt = !alt);
            lblCell.addElement(new Phrase("Mención académica:", FONT_LABEL));
            PdfPCell valCell = buildLabelCell(alt);
            valCell.addElement(new Phrase(TranslationUtils.translateAcademicDistinction(dist), FONT_DISTINCTION));
            table.addCell(lblCell);
            table.addCell(valCell);
        } else {
            addDataRow(table, "Mención académica:", "Sin mención especial", alt = !alt);
        }

        doc.add(table);
    }

    /** Párrafo resolutivo formal */
    private void addResolutiveNote(Document doc, StudentModality sm) throws DocumentException {
        Paragraph note = new Paragraph(
            "En constancia de lo anterior se firma el presente documento en la ciudad de Neiva, " +
            "Huila, a los " + LocalDateTime.now().format(DATE_FMT) + ".",
            FONT_BODY
        );
        note.setAlignment(Element.ALIGN_JUSTIFIED);
        doc.add(note);
    }

    /** Sección de firmas */
    private void addSignaturesSection(Document doc, StudentModality sm, User director)
            throws DocumentException {

        List<DefenseExaminer> examiners = sm.getDefenseExaminers() != null ? sm.getDefenseExaminers() : List.of();

        // Construir lista de firmantes
        List<SignatureInfo> signers = new ArrayList<>();

        // Eliminar la firma del director: NO agregar director
        // Solo agregar jurados

        // Primeros 2 jurados principales
        List<DefenseExaminer> sorted = new ArrayList<>(examiners);
        sorted.sort((a, b) -> {
            if (a.getExaminerType() == null) return 1;
            if (b.getExaminerType() == null) return -1;
            return a.getExaminerType().name().compareTo(b.getExaminerType().name());
        });

        for (int i = 0; i < Math.min(sorted.size(), 2); i++) {
            DefenseExaminer de = sorted.get(i);
            signers.add(new SignatureInfo(
                de.getExaminer().getName() + " " + de.getExaminer().getLastName(),
                i == 0 ? "Jurado Principal 1" : "Jurado Principal 2"
            ));
        }

        // Jurado de desempate (solo si aplica)
        boolean needsTiebreaker = needsTiebreakerSignature(sm);
        if (needsTiebreaker) {
            DefenseExaminer tiebreakerDe = sorted.stream()
                    .filter(e -> e.getExaminerType() != null &&
                            e.getExaminerType().name().equals("TIEBREAKER_EXAMINER"))
                    .findFirst().orElse(null);
            if (tiebreakerDe != null) {
                signers.add(new SignatureInfo(
                    tiebreakerDe.getExaminer().getName() + " " + tiebreakerDe.getExaminer().getLastName(),
                    "Jurado de Desempate"
                ));
            }
        }

        if (signers.isEmpty()) return;

        // Determinar número de columnas (máx 3)
        int cols = Math.min(signers.size(), 3);
        float[] colWidths = new float[cols];
        for (int i = 0; i < cols; i++) colWidths[i] = 1f;

        PdfPTable sigTable = new PdfPTable(colWidths);
        sigTable.setWidthPercentage(100);
        sigTable.setSpacingBefore(10f);

        for (SignatureInfo signer : signers) {
            sigTable.addCell(buildSignatureCell(signer.name, signer.role));
        }

        doc.add(sigTable);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de construcción de celdas y líneas
    // ─────────────────────────────────────────────────────────────────────────

    // Traducción de estados de modalidad

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers: filas de tabla de datos
    // ─────────────────────────────────────────────────────────────────────────

    private PdfPCell buildLabelCell(boolean alt) {
        BaseColor bg = alt ? ROW_BG_ALT : ROW_BG_LIGHT;
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(bg);
        cell.setPadding(7f);
        return cell;
    }

    private boolean needsTiebreakerSignature(StudentModality sm) {
        AcademicDistinction dist = sm.getAcademicDistinction();
        if (dist == null) return false;
        return dist == AcademicDistinction.TIEBREAKER_APPROVED
            || dist == AcademicDistinction.TIEBREAKER_MERITORIOUS
            || dist == AcademicDistinction.TIEBREAKER_LAUREATE
            || dist == AcademicDistinction.TIEBREAKER_REJECTED;
    }

    private void addThinGoldLine(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(1f);
        cell.setBackgroundColor(INST_GOLD);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        doc.add(t);
    }

    private String buildExaminersString(StudentModality sm) {
        List<DefenseExaminer> examiners = sm.getDefenseExaminers() != null ? sm.getDefenseExaminers() : List.of();
        if (examiners.isEmpty()) return "No asignados";
        return examiners.stream()
            .map(e -> e.getExaminer().getName() + " " + e.getExaminer().getLastName()
                    + " (" + (e.getExaminerType() != null ? TranslationUtils.translateExaminerType(e.getExaminerType()) : "Jurado") + ")")
            .collect(Collectors.joining(" | "));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Clase interna auxiliar para firmas
    // ─────────────────────────────────────────────────────────────────────────

    private record SignatureInfo(String name, String role) {}

    // ─────────────────────────────────────────────────────────────────────────
    // PDF SIMPLIFICADO — Aprobación directa por Comité de Currículo
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Construye el PDF simplificado: omite director, jurados, fecha/lugar de sustentación
     * y calificación final, pues la modalidad fue aprobada directamente por el Comité.
     */
    private void buildSimplifiedPdf(Path filePath, StudentModality sm, String certNumber) {
        String facultyName  = sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName();
        String programName  = sm.getProgramDegreeModality().getAcademicProgram().getName();
        String modalityName = sm.getProgramDegreeModality().getDegreeModality().getName();
        buildDocument(filePath, "ACTA DE APROBACIÓN DE MODALIDAD DE GRADO", certNumber, FONT_VERIF,
                facultyName, programName, "Error al generar el certificado PDF simplificado", doc -> {
            addSimplifiedCertificationBody(doc, sm, modalityName);
            addSpacing(doc, 12f);
            addSectionHeader(doc, "I. DATOS DEL GRADUANDO");
            addSpacing(doc, 4f);
            addStudentsTable(doc, sm);
            addSpacing(doc, 10f);
            addSectionHeader(doc, "II. INFORMACIÓN DE LA MODALIDAD");
            addSpacing(doc, 4f);
            addSimplifiedModalityTable(doc, sm);
            addSpacing(doc, 10f);
            addSectionHeader(doc, "III. DECISIÓN DEL COMITÉ DE CURRÍCULO");
            addSpacing(doc, 4f);
            addCommitteeDecisionTable(doc, sm);
            addSpacing(doc, 14f);
            addResolutiveNote(doc, sm);
            addSpacing(doc, 20f);
            addCommitteeSignatureNote(doc);
            addSpacing(doc, 16f);
        });
        log.info("PDF simplificado (comité) generado: {}", filePath);
    }

    /**
     * Texto certificatorio adaptado para la aprobación directa por el Comité
     * (no menciona sustentación ni calificación numérica).
     */
    private void addSimplifiedCertificationBody(Document doc, StudentModality sm, String modalityName)
            throws DocumentException {

        List<StudentModalityMember> members = sm.getMembers() != null ? sm.getMembers() : List.of();
        String studentNames;
        if (!members.isEmpty()) {
            studentNames = members.stream()
                    .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName())
                    .collect(Collectors.joining(", "));
        } else {
            studentNames = sm.getLeader().getName() + " " + sm.getLeader().getLastName();
        }

        String bodyText =
            "El Comité de Currículo del Programa Académico de " +
            sm.getProgramDegreeModality().getAcademicProgram().getName() +
            " de la Facultad de " + sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName() +
            " de la Universidad Surcolombiana, en ejercicio de sus atribuciones académicas " +
            "y conforme a las normas vigentes establecidas en el Acuerdo 071 de 2023, " +
            "mediante el presente documento CERTIFICA que el (los) estudiante(s):\n\n" +
            "            " + studentNames + "\n\n" +
            "Ha(n) APROBADO satisfactoriamente la modalidad de grado:\n\n" +
            "            \"" + modalityName + "\"\n\n" +
            "Habiendo cumplido con todos los requisitos académicos, documentales y " +
            "reglamentarios exigidos por la institución para la culminación exitosa " +
            "de su proceso de formación profesional.";

        Paragraph body = new Paragraph(bodyText, FONT_BODY);
        body.setAlignment(Element.ALIGN_JUSTIFIED);
        body.setLeading(0f, 1.4f);
        doc.add(body);
    }

    /**
     * Tabla de información de la modalidad en versión simplificada:
     * solo nombre, tipo y estado — sin director, jurados, fechas de sustentación.
     */
    private void addSimplifiedModalityTable(Document doc, StudentModality sm) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);

        boolean alt = false;
        addDataRow(table, "Nombre de la modalidad:",
                sm.getProgramDegreeModality().getDegreeModality().getName(), alt = !alt);
        addDataRow(table, "Tipo de modalidad:",
                sm.getModalityType() != null ? translateModalityType(sm.getModalityType().name()) : "Individual",
                alt = !alt);
        addDataRow(table, "Programa académico:",
                sm.getProgramDegreeModality().getAcademicProgram().getName(), alt = !alt);
        addDataRow(table, "Facultad:",
                sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName(), alt = !alt);
        addDataRow(table, "Fecha de aprobación:",
                LocalDateTime.now().format(DATE_FMT), alt = !alt);

        doc.add(table);
    }

    /**
     * Tabla de la decisión del comité de currículo.
     */
    private void addCommitteeDecisionTable(Document doc, StudentModality sm) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);

        boolean alt = false;
        addDataRow(table, "Estado de la modalidad:", "APROBADA", alt = !alt);
        addDataRow(table, "Instancia aprobadora:",
                "Comité de Currículo del Programa Académico", alt = !alt);
        addDataRow(table, "Modalidad de aprobación:",
                "Aprobación directa por Comité de Currículo", alt = !alt);

        AcademicDistinction dist = sm.getAcademicDistinction();
        if (dist != null && dist != AcademicDistinction.NO_DISTINCTION) {
            PdfPCell lblCell = buildLabelCell(alt = !alt);
            lblCell.addElement(new Phrase("Mención académica:", FONT_LABEL));
            PdfPCell valCell = buildLabelCell(alt);
            valCell.addElement(new Phrase(TranslationUtils.translateAcademicDistinction(dist), FONT_DISTINCTION));
            table.addCell(lblCell);
            table.addCell(valCell);
        }

        doc.add(table);
    }

    /**
     * Nota de firma simplificada para modalidades aprobadas por el comité
     * (sin firmas individuales de director/jurados).
     */
    private void addCommitteeSignatureNote(Document doc) throws DocumentException {
        Paragraph note = new Paragraph(
            "El presente documento ha sido emitido y avalado por el Comité de Currículo " +
            "del Programa Académico en nombre de la Universidad Surcolombiana.",
            FONT_BODY
        );
        note.setAlignment(Element.ALIGN_JUSTIFIED);
        note.setSpacingAfter(14f);
        doc.add(note);

        // Espacio para firma del comité
        int cols = 1;
        float[] colWidths = new float[]{1f};
        PdfPTable sigTable = new PdfPTable(colWidths);
        sigTable.setWidthPercentage(50);
        sigTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        sigTable.setSpacingBefore(8f);
        sigTable.addCell(buildSignatureCell("Comité de Currículo", "Programa Académico — Universidad Surcolombiana"));
        doc.add(sigTable);
    }
}











































