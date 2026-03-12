package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Modalities.Entity.AcademicCertificate;
import com.SIGMA.USCO.Modalities.Entity.DefenseExaminer;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.StudentModalityMember;
import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.Entity.enums.CertificateStatus;
import com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.Repository.AcademicCertificateRepository;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicCertificatePdfService {

    private final AcademicCertificateRepository certificateRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ── Paleta institucional ──────────────────────────────────────────────────
    /** Rojo institucional USCO */
    private static final BaseColor INST_RED    = new BaseColor(143, 30, 30);
    /** Dorado institucional USCO */
    private static final BaseColor INST_GOLD   = new BaseColor(180, 140, 60);
    /** Fondo muy claro para filas de datos */
    private static final BaseColor ROW_BG_LIGHT = new BaseColor(250, 247, 242);
    /** Fondo alternado para filas */
    private static final BaseColor ROW_BG_ALT  = new BaseColor(245, 240, 230);
    /** Gris oscuro para texto secundario */
    private static final BaseColor GRAY_DARK   = new BaseColor(60, 60, 60);
    /** Gris medio para notas */
    private static final BaseColor GRAY_MID    = new BaseColor(110, 110, 110);
    /** Verde oscuro para menciones positivas */
    private static final BaseColor GREEN_DARK  = new BaseColor(30, 100, 30);

    // ── Fuentes ───────────────────────────────────────────────────────────────
    private static final Font FONT_UNIV_NAME   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   13f, INST_RED);
    private static final Font FONT_FACULTY     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11f, GRAY_DARK);
    private static final Font FONT_PROGRAM     = FontFactory.getFont(FontFactory.HELVETICA,         10f, GRAY_DARK);
    private static final Font FONT_TITLE       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   15f, INST_RED);
    private static final Font FONT_ACTA_NUM    = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,  9f, GRAY_MID);
    private static final Font FONT_SECTION_HDR = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11f, BaseColor.WHITE);
    private static final Font FONT_LABEL       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    9f, GRAY_DARK);
    private static final Font FONT_VALUE       = FontFactory.getFont(FontFactory.HELVETICA,          9f, BaseColor.BLACK);
    private static final Font FONT_BODY        = FontFactory.getFont(FontFactory.HELVETICA,          9f, GRAY_DARK);
    // FONT_BODY_BOLD disponible para uso futuro
    private static final Font FONT_FOOTER      = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,  7f, GRAY_MID);
    private static final Font FONT_SIGN_NAME   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    8f, BaseColor.BLACK);
    private static final Font FONT_SIGN_ROLE   = FontFactory.getFont(FontFactory.HELVETICA,          7f, GRAY_MID);
    private static final Font FONT_DISTINCTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   10f, GREEN_DARK);
    private static final Font FONT_VERIF       = FontFactory.getFont(FontFactory.COURIER_BOLD,       7f, GRAY_MID);

    // ── DateTimeFormatter en español ──────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-CO"));

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AcademicCertificate generateCertificate(StudentModality studentModality) throws IOException {
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
        String certNumber = generateCertificateNumber(studentModality);

        Path outDir = Paths.get(uploadDir, "certificates",
                String.valueOf(studentModality.getProgramDegreeModality().getAcademicProgram().getId()));
        Files.createDirectories(outDir);
        String fileName = "ACTA_" + certNumber + "_" + leader.getId() + ".pdf";
        Path filePath  = outDir.resolve(fileName);

        buildPdf(filePath, studentModality, certNumber);
        log.info("Certificado PDF generado: {}", filePath);

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

    /**
     * Genera un certificado simplificado para modalidades aprobadas directamente por el Comité
     * de Currículo (sin sustentación, sin jurados, sin director, sin calificación final).
     */
    @Transactional
    public AcademicCertificate generateCertificateForCommitteeApproval(StudentModality studentModality) throws IOException {
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
        String certNumber = generateCertificateNumber(studentModality);

        Path outDir = Paths.get(uploadDir, "certificates",
                String.valueOf(studentModality.getProgramDegreeModality().getAcademicProgram().getId()));
        Files.createDirectories(outDir);
        String fileName = "ACTA_COMITE_" + certNumber + "_" + leader.getId() + ".pdf";
        Path filePath  = outDir.resolve(fileName);

        buildSimplifiedPdf(filePath, studentModality, certNumber);
        log.info("Certificado simplificado (comité) PDF generado: {}", filePath);

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
                .orElseThrow(() -> new RuntimeException("Certificado no encontrado para la modalidad " + studentModalityId));
        return Paths.get(cert.getFilePath());
    }

    @Transactional
    public void updateCertificateStatus(Long certificateId, CertificateStatus status) {
        AcademicCertificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificado no encontrado"));
        cert.setStatus(status);
        certificateRepository.save(cert);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generación del PDF (formato institucional USCO)
    // ─────────────────────────────────────────────────────────────────────────

    private void buildPdf(Path filePath, StudentModality sm, String certNumber) {
        try {
            // Márgenes: izq 50, der 50, sup 40, inf 50
            Document doc = new Document(PageSize.A4, 50, 50, 40, 50);
            PdfWriter.getInstance(doc, new FileOutputStream(filePath.toFile()));
            doc.open();

            User director = sm.getProjectDirector();
            String facultyName  = sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName();
            String programName  = sm.getProgramDegreeModality().getAcademicProgram().getName();
            String modalityName = sm.getProgramDegreeModality().getDegreeModality().getName();

            // ── 1. CABECERA INSTITUCIONAL (logo + membrete) ───────────────────
            addInstitutionalHeader(doc, facultyName, programName);

            // ── 2. LÍNEA ROJA DIVISORA ─────────────────────────────────────────
            addRedLine(doc);
            addSpacing(doc, 6f);

            // ── 3. TÍTULO Y NÚMERO DE ACTA ─────────────────────────────────────
            Paragraph title = new Paragraph("ACTA DE APROBACIÓN DE MODALIDAD DE GRADO", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            doc.add(title);

            Paragraph actaNum = new Paragraph("No. " + certNumber, FONT_ACTA_NUM);
            actaNum.setAlignment(Element.ALIGN_CENTER);
            actaNum.setSpacingAfter(4f);
            doc.add(actaNum);

            // Fecha de emisión alineada a la derecha
            Paragraph issueDate = new Paragraph(
                    "Neiva, " + LocalDateTime.now().format(DATE_FMT), FONT_BODY);
            issueDate.setAlignment(Element.ALIGN_RIGHT);
            issueDate.setSpacingAfter(10f);
            doc.add(issueDate);

            addGoldLine(doc);
            addSpacing(doc, 8f);

            // ── 4. CUERPO CERTIFICATORIO ────────────────────────────────────────
            addCertificationBody(doc, sm, modalityName);
            addSpacing(doc, 12f);

            // ── 5. SECCIÓN: DATOS DE LOS ESTUDIANTES ──────────────────────────
            addSectionHeader(doc, "I. DATOS DEL GRADUANDO");
            addSpacing(doc, 4f);
            addStudentsTable(doc, sm);
            addSpacing(doc, 10f);

            // ── 6. SECCIÓN: INFORMACIÓN DE LA MODALIDAD ────────────────────────
            addSectionHeader(doc, "II. INFORMACIÓN DE LA MODALIDAD");
            addSpacing(doc, 4f);
            addModalityTable(doc, sm, director);
            addSpacing(doc, 10f);

            // ── 7. SECCIÓN: RESULTADO DE LA EVALUACIÓN ─────────────────────────
            addSectionHeader(doc, "III. RESULTADO DE LA EVALUACIÓN");
            addSpacing(doc, 4f);
            addResultTable(doc, sm);
            addSpacing(doc, 14f);

            // ── 8. NOTA RESOLUTIVA ─────────────────────────────────────────────
            addResolutiveNote(doc, sm);
            addSpacing(doc, 20f);

            // ── 9. FIRMAS ──────────────────────────────────────────────────────
            addSignaturesSection(doc, sm, director);
            addSpacing(doc, 16f);

            // ── 10. PIE DE PÁGINA ─────────────────────────────────────────────
            addGoldLine(doc);
            addSpacing(doc, 6f);
            addFooter(doc, certNumber);

            doc.close();
            log.info("PDF institucional generado: {}", filePath);
        } catch (DocumentException | IOException e) {
            log.error("Error generando PDF institucional: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el certificado PDF", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bloques de construcción del documento
    // ─────────────────────────────────────────────────────────────────────────

    /** Cabecera: logo a la izquierda + texto institucional centrado */
    private void addInstitutionalHeader(Document doc, String facultyName, String programName)
            throws DocumentException, IOException {

        PdfPTable headerTable = new PdfPTable(new float[]{1.5f, 5f});
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(4f);

        // Celda del logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(2f);

        try {
            ClassPathResource logoResource = new ClassPathResource("templates/logo ingenieria.png");
            try (InputStream logoStream = logoResource.getInputStream()) {
                byte[] logoBytes = logoStream.readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(90f, 70f);
                logo.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo institucional: {}", e.getMessage());
            // Si no se encuentra el logo, la celda queda vacía
            logoCell.addElement(new Paragraph(" ", FONT_BODY));
        }

        headerTable.addCell(logoCell);

        // Celda del texto institucional
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        textCell.setPaddingLeft(10f);

        Paragraph univName = new Paragraph("UNIVERSIDAD SURCOLOMBIANA", FONT_UNIV_NAME);
        univName.setAlignment(Element.ALIGN_LEFT);
        univName.setSpacingAfter(2f);

        Paragraph fac = new Paragraph(facultyName.toUpperCase(), FONT_FACULTY);
        fac.setAlignment(Element.ALIGN_LEFT);
        fac.setSpacingAfter(2f);

        Paragraph prog = new Paragraph(programName, FONT_PROGRAM);
        prog.setAlignment(Element.ALIGN_LEFT);
        prog.setSpacingAfter(2f);

        Paragraph slogan = new Paragraph("Sistema de Información y Gestión Académica — SIGMA", FONT_SIGN_ROLE);
        slogan.setAlignment(Element.ALIGN_LEFT);

        textCell.addElement(univName);
        textCell.addElement(fac);
        textCell.addElement(prog);
        textCell.addElement(slogan);

        headerTable.addCell(textCell);
        doc.add(headerTable);
    }

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
            distinctionStr = " con " + translateDistinction(dist).toLowerCase();
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

        if (members.isEmpty()) {
            // Solo el líder
            addSingleStudentRows(doc, sm, sm.getLeader(), true);
        } else {
            boolean first = true;
            for (StudentModalityMember m : members) {
                if (!first) {
                    addSpacing(doc, 4f);
                    // Separador
                    addThinGoldLine(doc);
                    addSpacing(doc, 4f);
                }
                addSingleStudentRows(doc, sm, m.getStudent(), m.getIsLeader() != null && m.getIsLeader());
                first = false;
            }
        }
    }

    private void addSingleStudentRows(Document doc, StudentModality sm, User student, boolean isLeader)
            throws DocumentException {

        String studentCode = "No registrado";
        try {
            StudentProfile profile = studentProfileRepository.findByUserId(student.getId()).orElse(null);
            if (profile != null && profile.getStudentCode() != null) {
                studentCode = profile.getStudentCode();
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener el código del estudiante {}: {}", student.getId(), e.getMessage());
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
        addDataRow(table, "Estado de la modalidad:", sm.getStatus() != null ? translateModalityStatus(sm.getStatus()) : "No registrado", alt = !alt);

        // Nota final
        String grade = sm.getFinalGrade() != null ? String.format("%.1f / 5.0", sm.getFinalGrade()) : "No registrada";
        addDataRow(table, "Calificación final:", grade, alt = !alt);

        // Mención académica
        AcademicDistinction dist = sm.getAcademicDistinction();
        if (dist != null && dist != AcademicDistinction.NO_DISTINCTION) {
            // Fila especial con fuente verde
            PdfPCell lblCell = buildLabelCell(alt = !alt);
            lblCell.addElement(new Phrase("Mención académica:", FONT_LABEL));
            PdfPCell valCell = buildValueCell(alt);
            valCell.addElement(new Phrase(translateDistinction(dist), FONT_DISTINCTION));
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

    private String generateCertificateNumber(StudentModality studentModality) {

        Long programId = studentModality.getProgramDegreeModality().getAcademicProgram().getId();
        int year = LocalDateTime.now().getYear();
        // Buscar el siguiente número disponible
        int nextNumber = 1;
        while (true) {
            int num = nextNumber;
            String candidate = String.format("ACTA-PROG%d-%d-%04d", programId, year, num);
            boolean exists = certificateRepository.findAll().stream()
                .anyMatch(cert -> cert.getCertificateNumber().equals(candidate));
            if (!exists) {
                return candidate;
            }
            nextNumber++;
        }
    }

    private String calculateFileHash(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Error calculando hash del archivo: {}", e.getMessage());
            return "";
        }
    }

    private String translateDistinction(AcademicDistinction distinction) {
        if (distinction == null) {
            return "Sin distinción";
        }

        return switch (distinction) {

            case AGREED_APPROVED -> "Aprobado sin distinción";
            case AGREED_MERITORIOUS -> "Aprobado con mención meritoria";
            case AGREED_LAUREATE -> "Aprobado con mención laureada";
            case AGREED_REJECTED -> "Rechazado";

            // Desacuerdo pendiente
            case DISAGREEMENT_PENDING_TIEBREAKER -> "En proceso de evaluación por desempate";


            case TIEBREAKER_APPROVED -> "Aprobado sin distinción (por desempate)";
            case TIEBREAKER_MERITORIOUS -> "Aprobado con mención meritoria (por desempate)";
            case TIEBREAKER_LAUREATE -> "Aprobado con mención laureada (por desempate)";
            case TIEBREAKER_REJECTED -> "Rechazado (por desempate)";


            case NO_DISTINCTION -> "Sin distinción";
            default -> "Sin distinción";
        };
    }

    // Traducción de estados de modalidad
    private String translateModalityStatus(ModalityProcessStatus status) {
        if (status == null) return "No registrado";
        return switch (status) {
            case MODALITY_SELECTED -> "Modalidad seleccionada";
            case UNDER_REVIEW_PROGRAM_HEAD -> "En revisión por Jefatura de Programa de programa y/o coordinación de modalidades";
            case CORRECTIONS_REQUESTED_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura de Programa";
            case CORRECTIONS_SUBMITTED -> "Correcciones enviadas";
            case CORRECTIONS_SUBMITTED_TO_PROGRAM_HEAD -> "Correcciones enviadas a Jefatura de Programa y/o Coordinación de Modalidades";
            case CORRECTIONS_SUBMITTED_TO_COMMITTEE -> "Correcciones enviadas al Comité de Currículo";
            case CORRECTIONS_SUBMITTED_TO_EXAMINERS -> "Correcciones enviadas a los Jurados";
            case CORRECTIONS_APPROVED -> "Correcciones aprobadas";
            case CORRECTIONS_REJECTED_FINAL -> "Correcciones rechazadas (final)";
            case READY_FOR_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para Comité de Currículo";
            case UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE -> "En revisión por Comité de Currículo";
            case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité de Currículo";
            case READY_FOR_DIRECTOR_ASSIGNMENT -> "Lista para asignación de Director de Proyecto";
            case READY_FOR_APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para aprobación por Comité de Currículo";
            case APPROVED_BY_PROGRAM_CURRICULUM_COMMITTEE -> "Aprobada por Comité de Currículo";
            case PROPOSAL_APPROVED -> "Propuesta aprobada";
            case PENDING_PROGRAM_HEAD_FINAL_REVIEW -> "Pendiente de revisión final por Jefatura de Programa";
            case APPROVED_BY_PROGRAM_HEAD_FINAL_REVIEW -> "Documentos finales aprobados por Jefatura de Programa";
            case DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR -> "Sustentación solicitada por Director de Proyecto";
            case DEFENSE_SCHEDULED -> "Sustentación programada";
            case EXAMINERS_ASSIGNED -> "Jurados asignados";
            case READY_FOR_EXAMINERS -> "Lista para jurados";
            case DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos de propuesta aprobados por los jurados";
            case SECONDARY_DOCUMENTS_APPROVED_BY_EXAMINERS -> "Documentos finales aprobados por los jurados";
            case DOCUMENT_REVIEW_TIEBREAKER_REQUIRED -> "Revisión con desempate requerida por jurados";
            case EDIT_REQUESTED_BY_STUDENT -> "Edición de documento solicitado por estudiante";
            case CORRECTIONS_REQUESTED_EXAMINERS -> "Correcciones solicitadas por jurados";
            case READY_FOR_DEFENSE -> "Lista para sustentación";
            case FINAL_REVIEW_COMPLETED -> "Revisión final completada";
            case DEFENSE_COMPLETED -> "Sustentación realizada";
            case UNDER_EVALUATION_PRIMARY_EXAMINERS -> "En evaluación por jurados principales";
            case DISAGREEMENT_REQUIRES_TIEBREAKER -> "Desacuerdo, requiere desempate";
            case UNDER_EVALUATION_TIEBREAKER -> "En evaluación por jurado de desempate";
            case EVALUATION_COMPLETED -> "Evaluación completada";
            case PENDING_DISTINCTION_COMMITTEE_REVIEW -> "Aprobada - Distinción honorífica pendiente de revisión por el Comité";
            case GRADED_APPROVED -> "Aprobada";
            case GRADED_FAILED -> "No aprobada";
            case MODALITY_CLOSED -> "Modalidad cerrada";
            case SEMINAR_CANCELED -> "Seminario cancelado";
            case MODALITY_CANCELLED -> "Modalidad cancelada";
            case CANCELLATION_REQUESTED -> "Cancelación solicitada";
            case CANCELLATION_APPROVED_BY_PROJECT_DIRECTOR -> "Cancelación aprobada por Director de Proyecto";
            case CANCELLATION_REJECTED_BY_PROJECT_DIRECTOR -> "Cancelación rechazada por Director de Proyecto";
            case CANCELLED_WITHOUT_REPROVAL -> "Cancelada sin reprobación";
            case CANCELLATION_REJECTED -> "Cancelación rechazada";
            case CANCELLED_BY_CORRECTION_TIMEOUT -> "Cancelada por vencimiento de plazo de corrección";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers: filas de tabla de datos
    // ─────────────────────────────────────────────────────────────────────────

    private void addDataRow(PdfPTable table, String label, String value, boolean alt) {
        BaseColor bg = alt ? ROW_BG_ALT : ROW_BG_LIGHT;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(bg);
        labelCell.setPadding(7f);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", FONT_VALUE));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setBackgroundColor(bg);
        valueCell.setPadding(7f);
        table.addCell(valueCell);
    }

    private PdfPCell buildLabelCell(boolean alt) {
        BaseColor bg = alt ? ROW_BG_ALT : ROW_BG_LIGHT;
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(bg);
        cell.setPadding(7f);
        return cell;
    }

    private PdfPCell buildValueCell(boolean alt) {
        BaseColor bg = alt ? ROW_BG_ALT : ROW_BG_LIGHT;
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(bg);
        cell.setPadding(7f);
        return cell;
    }

    private PdfPCell buildSignatureCell(String name, String role) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(10f);
        cell.setPaddingBottom(6f);
        cell.setPaddingLeft(10f);
        cell.setPaddingRight(10f);

        // Línea de firma
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(80);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setFixedHeight(22f);
        lineCell.setBorder(Rectangle.BOTTOM);
        lineCell.setBorderWidthBottom(1f);
        lineCell.setBorderColorBottom(GRAY_DARK);
        lineTable.addCell(lineCell);
        cell.addElement(lineTable);

        // Nombre
        Paragraph namePara = new Paragraph(name, FONT_SIGN_NAME);
        namePara.setAlignment(Element.ALIGN_CENTER);
        namePara.setSpacingBefore(4f);
        cell.addElement(namePara);

        // Rol
        Paragraph rolePara = new Paragraph(role, FONT_SIGN_ROLE);
        rolePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(rolePara);

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

    private void addSectionHeader(Document doc, String title) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4f);
        t.setSpacingAfter(0f);
        PdfPCell cell = new PdfPCell(new Phrase(title, FONT_SECTION_HDR));
        cell.setBackgroundColor(INST_RED);
        cell.setPadding(7f);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        doc.add(t);
    }

    private void addRedLine(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(3f);
        cell.setBackgroundColor(INST_RED);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        doc.add(t);
    }

    private void addGoldLine(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(2f);
        cell.setBackgroundColor(INST_GOLD);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        doc.add(t);
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

    private void addSpacing(Document doc, float spacingPt) throws DocumentException {
        Paragraph sp = new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, spacingPt));
        sp.setLeading(spacingPt);
        doc.add(sp);
    }

    private void addFooter(Document doc, String certNumber) throws DocumentException {
        Paragraph footer = new Paragraph(
            "Este documento tiene validez oficial en el marco de los procesos académicos " +
            "de la Universidad Surcolombiana, conforme al Acuerdo 071 de 2023.",
            FONT_FOOTER
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingAfter(4f);
        doc.add(footer);

        Paragraph verif = new Paragraph("Código de verificación: " + certNumber, FONT_VERIF);
        verif.setAlignment(Element.ALIGN_CENTER);
        doc.add(verif);
    }

    private String buildExaminersString(StudentModality sm) {
        List<DefenseExaminer> examiners = sm.getDefenseExaminers() != null ? sm.getDefenseExaminers() : List.of();
        if (examiners.isEmpty()) return "No asignados";
        return examiners.stream()
            .map(e -> e.getExaminer().getName() + " " + e.getExaminer().getLastName()
                    + " (" + translateExaminerType(e.getExaminerType() != null ? e.getExaminerType().name() : "") + ")")
            .collect(Collectors.joining(" | "));
    }

    private String translateExaminerType(String type) {
        return switch (type) {
            case "PRIMARY_EXAMINER_1" -> "Jurado Principal 1";
            case "PRIMARY_EXAMINER_2" -> "Jurado Principal 2";
            case "TIEBREAKER_EXAMINER" -> "Jurado de Desempate";
            default -> "Jurado";
        };
    }

    private String translateModalityType(String type) {
        if (type == null) return "Individual";
        return switch (type.toUpperCase()) {
            case "GROUP" -> "Grupal";
            case "INDIVIDUAL" -> "Individual";
            default -> type;
        };
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
        try {
            Document doc = new Document(PageSize.A4, 50, 50, 40, 50);
            PdfWriter.getInstance(doc, new FileOutputStream(filePath.toFile()));
            doc.open();

            String facultyName  = sm.getProgramDegreeModality().getAcademicProgram().getFaculty().getName();
            String programName  = sm.getProgramDegreeModality().getAcademicProgram().getName();
            String modalityName = sm.getProgramDegreeModality().getDegreeModality().getName();

            // ── 1. CABECERA INSTITUCIONAL ─────────────────────────────────────
            addInstitutionalHeader(doc, facultyName, programName);

            // ── 2. LÍNEA ROJA ─────────────────────────────────────────────────
            addRedLine(doc);
            addSpacing(doc, 6f);

            // ── 3. TÍTULO Y NÚMERO DE ACTA ────────────────────────────────────
            Paragraph title = new Paragraph("ACTA DE APROBACIÓN DE MODALIDAD DE GRADO", FONT_TITLE);
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

            // ── 4. CUERPO CERTIFICATORIO SIMPLIFICADO ─────────────────────────
            addSimplifiedCertificationBody(doc, sm, modalityName);
            addSpacing(doc, 12f);

            // ── 5. DATOS DEL GRADUANDO ────────────────────────────────────────
            addSectionHeader(doc, "I. DATOS DEL GRADUANDO");
            addSpacing(doc, 4f);
            addStudentsTable(doc, sm);
            addSpacing(doc, 10f);

            // ── 6. INFORMACIÓN DE LA MODALIDAD (versión simplificada) ─────────
            addSectionHeader(doc, "II. INFORMACIÓN DE LA MODALIDAD");
            addSpacing(doc, 4f);
            addSimplifiedModalityTable(doc, sm);
            addSpacing(doc, 10f);

            // ── 7. DECISIÓN DEL COMITÉ ────────────────────────────────────────
            addSectionHeader(doc, "III. DECISIÓN DEL COMITÉ DE CURRÍCULO");
            addSpacing(doc, 4f);
            addCommitteeDecisionTable(doc, sm);
            addSpacing(doc, 14f);

            // ── 8. NOTA RESOLUTIVA ────────────────────────────────────────────
            addResolutiveNote(doc, sm);
            addSpacing(doc, 20f);

            // ── 9. FIRMA DEL COMITÉ ───────────────────────────────────────────
            addCommitteeSignatureNote(doc);
            addSpacing(doc, 16f);

            // ── 10. PIE DE PÁGINA ─────────────────────────────────────────────
            addGoldLine(doc);
            addSpacing(doc, 6f);
            addFooter(doc, certNumber);

            doc.close();
            log.info("PDF simplificado (comité) generado: {}", filePath);
        } catch (DocumentException | IOException e) {
            log.error("Error generando PDF simplificado: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el certificado PDF simplificado", e);
        }
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
            PdfPCell valCell = buildValueCell(alt);
            valCell.addElement(new Phrase(translateDistinction(dist), FONT_DISTINCTION));
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











































