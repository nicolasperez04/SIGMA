package com.SIGMA.USCO.notifications.service;

import com.SIGMA.USCO.Modalities.Entity.AcademicCertificate;
import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Entity.enums.AcademicDistinction;
import com.SIGMA.USCO.Modalities.Entity.enums.CertificateStatus;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicCertificatePdfService {

    private final AcademicCertificateRepository certificateRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // COLORES INSTITUCIONALES
    private static final BaseColor INSTITUTIONAL_RED = new BaseColor(143, 30, 30); // #8F1E1E
    private static final BaseColor INSTITUTIONAL_GOLD = new BaseColor(213, 203, 160); // #D5CBA0
    private static final BaseColor WHITE = BaseColor.WHITE;
    private static final BaseColor LIGHT_GOLD = new BaseColor(245, 242, 235);
    private static final BaseColor TEXT_BLACK = BaseColor.BLACK;
    private static final BaseColor TEXT_GRAY = new BaseColor(80, 80, 80);

    // FUENTES INSTITUCIONALES
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, INSTITUTIONAL_RED);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, INSTITUTIONAL_RED);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, INSTITUTIONAL_RED);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_BLACK);
    private static final Font DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_BLACK);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_GRAY);
    private static final Font SIGNATURE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_BLACK);
    private static final Font SIGNATURE_LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_GRAY);
    private static final Font ACTA_NUMBER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, TEXT_GRAY);
    private static final Font DISTINCTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new BaseColor(0, 100, 0));

    @Transactional
    public AcademicCertificate generateCertificate(StudentModality studentModality) throws IOException {
        AcademicCertificate existingCertificate = certificateRepository.findByStudentModalityId(studentModality.getId()).orElse(null);
        if (existingCertificate != null) {
            try {
                Path oldFilePath = Paths.get(existingCertificate.getFilePath());
                if (Files.exists(oldFilePath)) {
                    Files.delete(oldFilePath);
                    log.info("Archivo PDF antiguo eliminado: {}", oldFilePath);
                }
            } catch (IOException e) {
                log.warn("No se pudo eliminar el archivo PDF antiguo: {}", e.getMessage());
            }
            certificateRepository.delete(existingCertificate);
            log.info("Registro de certificado antiguo eliminado de BD");
        }
        User student = studentModality.getLeader();
        String certificateNumber = generateCertificateNumber(studentModality);
        Path certificatesPath = Paths.get(uploadDir, "certificates",
                String.valueOf(studentModality.getProgramDegreeModality().getAcademicProgram().getId()));
        Files.createDirectories(certificatesPath);
        String fileName = "ACTA_" + certificateNumber + "_" + student.getId() + ".pdf";
        Path filePath = certificatesPath.resolve(fileName);
        generatePdfDocument(filePath, studentModality, certificateNumber);
        log.info("Nuevo certificado PDF generado: {}", filePath);
        String fileHash = calculateFileHash(filePath);
        AcademicCertificate certificate = AcademicCertificate.builder()
                .studentModality(studentModality)
                .certificateNumber(certificateNumber)
                .issueDate(LocalDateTime.now())
                .filePath(filePath.toString())
                .fileHash(fileHash)
                .status(CertificateStatus.GENERATED)
                .build();
        return certificateRepository.save(certificate);
    }

    private void generatePdfDocument(Path filePath, StudentModality studentModality, String certificateNumber) {
        try {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, new FileOutputStream(filePath.toFile()));
            document.open();

            User student = studentModality.getLeader();
            User director = studentModality.getProjectDirector();

            // 1. Banda superior institucional
            PdfPTable headerBand = new PdfPTable(1);
            headerBand.setWidthPercentage(100);
            headerBand.setSpacingAfter(30);
            PdfPCell bandCell = new PdfPCell();
            bandCell.setBackgroundColor(INSTITUTIONAL_RED);
            bandCell.setPadding(25);
            bandCell.setBorder(Rectangle.NO_BORDER);
            Paragraph bandContent = new Paragraph();
            bandContent.setAlignment(Element.ALIGN_CENTER);
            // Texto en blanco
            bandContent.add(new Chunk("UNIVERSIDAD SURCOLOMBIANA\n", new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, WHITE)));
            bandContent.add(new Chunk(studentModality.getProgramDegreeModality().getAcademicProgram().getFaculty().getName().toUpperCase() + "\n", new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, WHITE)));
            bandContent.add(new Chunk(studentModality.getProgramDegreeModality().getAcademicProgram().getName(), new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, WHITE)));
            bandCell.addElement(bandContent);
            headerBand.addCell(bandCell);
            document.add(headerBand);

            // 2. Título principal
            Paragraph title = new Paragraph("ACTA DE APROBACIÓN DE MODALIDAD DE GRADO", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Paragraph actaNum = new Paragraph("Acta No. " + certificateNumber, ACTA_NUMBER_FONT);
            actaNum.setAlignment(Element.ALIGN_CENTER);
            actaNum.setSpacingAfter(20);
            document.add(actaNum);

            // Línea dorada decorativa
            addHorizontalLine(document, INSTITUTIONAL_GOLD);
            document.add(Chunk.NEWLINE);

            // 3. Resultado de la evaluación
            Paragraph resultSection = new Paragraph("RESULTADO DE LA EVALUACIÓN", HEADER_FONT);
            resultSection.setSpacingBefore(10);
            resultSection.setSpacingAfter(8);
            document.add(resultSection);

            Paragraph resultText = new Paragraph(
                "El Comité de Currículo del programa académico, tras la realización de la sustentación oficial y la evaluación exhaustiva por parte de los jurados designados, certifica que el(la) estudiante identificado en este documento ha cumplido satisfactoriamente con todos los requisitos académicos, normativos y procedimentales establecidos por la Universidad Surcolombiana para la modalidad de grado.\n\n" +
                "En virtud de lo anterior, se otorga la aprobación formal de la modalidad, reconociendo el esfuerzo, la dedicación y el cumplimiento de los estándares institucionales exigidos para la culminación del proceso académico.",
                DATA_FONT
            );
            resultText.setAlignment(Element.ALIGN_JUSTIFIED);
            resultText.setSpacingAfter(18);
            document.add(resultText);

            // 4. Datos del estudiante (en tabla con fondo dorado claro)
            Paragraph studentSection = new Paragraph("DATOS DEL ESTUDIANTE", HEADER_FONT);
            studentSection.setSpacingAfter(8);
            document.add(studentSection);

            String studentCode = "No registrado";
            try {
                StudentProfile profile = studentProfileRepository.findByUserId(student.getId()).orElse(null);
                if (profile != null && profile.getStudentCode() != null) {
                    studentCode = profile.getStudentCode();
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener el código del estudiante: {}", e.getMessage());
            }

            PdfPTable studentTable = new PdfPTable(2);
            studentTable.setWidthPercentage(100);
            studentTable.setSpacingBefore(5);
            studentTable.setSpacingAfter(15);
            addStyledTableRow(studentTable, "Nombre completo:", student.getName() + " " + student.getLastName(), LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            addStyledTableRow(studentTable, "Código estudiantil:", studentCode, LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            addStyledTableRow(studentTable, "Correo institucional:", student.getEmail(), LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            addStyledTableRow(studentTable, "Programa académico:", studentModality.getProgramDegreeModality().getAcademicProgram().getName(), LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            addStyledTableRow(studentTable, "Facultad:", studentModality.getProgramDegreeModality().getAcademicProgram().getFaculty().getName(), LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            document.add(studentTable);

            // 5. Información de la modalidad
            Paragraph modalitySection = new Paragraph("INFORMACIÓN DE LA MODALIDAD", HEADER_FONT);
            modalitySection.setSpacingAfter(8);
            document.add(modalitySection);

            PdfPTable modalityTable = new PdfPTable(2);
            modalityTable.setWidthPercentage(100);
            modalityTable.setSpacingBefore(5);
            modalityTable.setSpacingAfter(15);
            addStyledTableRow(modalityTable, "Modalidad de grado:", studentModality.getProgramDegreeModality().getDegreeModality().getName(), LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            // Integrantes
            String members = studentModality.getMembers() != null && !studentModality.getMembers().isEmpty() ?
                    studentModality.getMembers().stream()
                            .map(m -> m.getStudent().getName() + " " + m.getStudent().getLastName() + " (" + m.getStudent().getEmail() + ")")
                            .collect(java.util.stream.Collectors.joining(", ")) :
                    student.getName() + " " + student.getLastName() + " (" + student.getEmail() + ")";
            addStyledTableRow(modalityTable, "Integrantes:", members, LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            // Jurados
            String examiners = "No asignados";
            try {
                java.util.List<com.SIGMA.USCO.Modalities.Entity.DefenseExaminer> defenseExaminers = studentModality.getDefenseExaminers();
                if (defenseExaminers != null && !defenseExaminers.isEmpty()) {
                    examiners = defenseExaminers.stream()
                        .map(e -> e.getExaminer().getName() + " " + e.getExaminer().getLastName())
                        .collect(java.util.stream.Collectors.joining(", "));
                }
            } catch (Exception e) {
                // Si hay error, dejar "No asignados"
            }

            // Estado actual (en español)
            addStyledTableRow(modalityTable, "Estado de la modalidad:", studentModality.getStatus() != null ? translateModalityStatus(studentModality.getStatus()) : "No registrado", LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            // Nota final
            addStyledTableRow(modalityTable, "Nota final:", studentModality.getFinalGrade() != null ? studentModality.getFinalGrade().toString() : "No registrada", LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            // Mención académica (en español)
            AcademicDistinction distinction = studentModality.getAcademicDistinction();
            if (distinction != null && distinction != AcademicDistinction.NO_DISTINCTION) {
                addStyledTableRow(modalityTable, "Mención académica:", translateDistinction(distinction), LABEL_FONT, DISTINCTION_FONT, LIGHT_GOLD);
            }
            addStyledTableRow(modalityTable, "Director de proyecto:", director != null ? director.getName() + " " + director.getLastName() : "No asignado", LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            addStyledTableRow(modalityTable, "Jurados:", examiners, LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            addStyledTableRow(modalityTable, "Fecha de sustentación:", studentModality.getDefenseDate() != null ? studentModality.getDefenseDate().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy, HH:mm")) : "No registrada", LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            // Lugar de sustentación
            addStyledTableRow(modalityTable, "Lugar de sustentación:", studentModality.getDefenseLocation() != null ? studentModality.getDefenseLocation() : "No registrado", LABEL_FONT, DATA_FONT, LIGHT_GOLD);
            document.add(modalityTable);

            // 6. Fecha de emisión
            Paragraph issueDateParagraph = new Paragraph();
            issueDateParagraph.add(new Chunk("Fecha de emisión: ", LABEL_FONT));
            issueDateParagraph.add(new Chunk(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy")), DATA_FONT));
            issueDateParagraph.setAlignment(Element.ALIGN_RIGHT);
            issueDateParagraph.setSpacingBefore(10);
            issueDateParagraph.setSpacingAfter(30);
            document.add(issueDateParagraph);

            // 7. Firmas
            PdfPTable signaturesTable = new PdfPTable(3);
            signaturesTable.setWidthPercentage(100);
            signaturesTable.setSpacingBefore(20);
            signaturesTable.setSpacingAfter(25);

            // Director de proyecto
            if (director != null) {
                PdfPCell directorCell = new PdfPCell();
                directorCell.setBorder(Rectangle.NO_BORDER);
                directorCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                directorCell.setPaddingTop(20);
                // Línea de firma (borde inferior)
                PdfPTable firmaTable = new PdfPTable(1);
                firmaTable.setWidthPercentage(80);
                PdfPCell firmaLine = new PdfPCell();
                firmaLine.setBorder(Rectangle.BOTTOM);
                firmaLine.setBorderWidthBottom(1.5f);
                firmaLine.setFixedHeight(18f);
                firmaLine.setBorderColorBottom(TEXT_BLACK);
                firmaLine.setPadding(0);
                firmaLine.setHorizontalAlignment(Element.ALIGN_CENTER);
                firmaTable.addCell(firmaLine);
                directorCell.addElement(firmaTable);
                // Texto debajo de la línea
                Paragraph directorSign = new Paragraph();
                directorSign.add(new Chunk("Firma\n", SIGNATURE_FONT));
                directorSign.add(new Chunk("Director de Proyecto\n", SIGNATURE_LABEL_FONT));
                directorSign.add(new Chunk(director.getName() + " " + director.getLastName(), FOOTER_FONT));
                directorSign.setAlignment(Element.ALIGN_CENTER);
                directorCell.addElement(directorSign);
                signaturesTable.addCell(directorCell);
            }

            // Jurados principales
            java.util.List<com.SIGMA.USCO.Modalities.Entity.DefenseExaminer> defenseExaminers = studentModality.getDefenseExaminers();
            int juradoCount = defenseExaminers != null ? defenseExaminers.size() : 0;
            for (int i = 0; i < Math.min(juradoCount, 2); i++) {
                com.SIGMA.USCO.Modalities.Entity.DefenseExaminer examiner = defenseExaminers.get(i);
                User jurado = examiner.getExaminer();
                PdfPCell juradoCell = new PdfPCell();
                juradoCell.setBorder(Rectangle.NO_BORDER);
                juradoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                juradoCell.setPaddingTop(20);
                PdfPTable firmaTable = new PdfPTable(1);
                firmaTable.setWidthPercentage(80);
                PdfPCell firmaLine = new PdfPCell();
                firmaLine.setBorder(Rectangle.BOTTOM);
                firmaLine.setBorderWidthBottom(1.5f);
                firmaLine.setFixedHeight(18f);
                firmaLine.setBorderColorBottom(TEXT_BLACK);
                firmaLine.setPadding(0);
                firmaLine.setHorizontalAlignment(Element.ALIGN_CENTER);
                firmaTable.addCell(firmaLine);
                juradoCell.addElement(firmaTable);
                Paragraph juradoSign = new Paragraph();
                juradoSign.add(new Chunk("Firma\n", SIGNATURE_FONT));
                juradoSign.add(new Chunk("Jurado Principal\n", SIGNATURE_LABEL_FONT));
                juradoSign.add(new Chunk(jurado.getName() + " " + jurado.getLastName(), FOOTER_FONT));
                juradoSign.setAlignment(Element.ALIGN_CENTER);
                juradoCell.addElement(juradoSign);
                signaturesTable.addCell(juradoCell);
            }

            // Jurado de desempate (solo si aplica)
            boolean showTieBreaker = false;
            com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus status = studentModality.getStatus();
            if (juradoCount >= 3 && status != null) {
                showTieBreaker = switch (status) {
                    case DISAGREEMENT_REQUIRES_TIEBREAKER, UNDER_EVALUATION_TIEBREAKER, EVALUATION_COMPLETED -> true;
                    default -> false;
                };
            }
            if (showTieBreaker) {
                com.SIGMA.USCO.Modalities.Entity.DefenseExaminer tieBreakerExaminer = defenseExaminers.get(2);
                User tieBreaker = tieBreakerExaminer.getExaminer();
                PdfPCell tieBreakerCell = new PdfPCell();
                tieBreakerCell.setBorder(Rectangle.NO_BORDER);
                tieBreakerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tieBreakerCell.setPaddingTop(20);
                PdfPTable firmaTable = new PdfPTable(1);
                firmaTable.setWidthPercentage(80);
                PdfPCell firmaLine = new PdfPCell();
                firmaLine.setBorder(Rectangle.BOTTOM);
                firmaLine.setBorderWidthBottom(1.5f);
                firmaLine.setFixedHeight(18f);
                firmaLine.setBorderColorBottom(TEXT_BLACK);
                firmaLine.setPadding(0);
                firmaLine.setHorizontalAlignment(Element.ALIGN_CENTER);
                firmaTable.addCell(firmaLine);
                tieBreakerCell.addElement(firmaTable);
                Paragraph tieBreakerSign = new Paragraph();
                tieBreakerSign.add(new Chunk("Firma\n", SIGNATURE_FONT));
                tieBreakerSign.add(new Chunk("Jurado de Desempate\n", SIGNATURE_LABEL_FONT));
                tieBreakerSign.add(new Chunk(tieBreaker.getName() + " " + tieBreaker.getLastName(), FOOTER_FONT));
                tieBreakerSign.setAlignment(Element.ALIGN_CENTER);
                tieBreakerCell.addElement(tieBreakerSign);
                signaturesTable.addCell(tieBreakerCell);
            }
            document.add(signaturesTable);

            // Línea dorada decorativa
            addHorizontalLine(document, INSTITUTIONAL_GOLD);

            // 8. Nota legal y código de verificación
            Paragraph legalNote = new Paragraph(
                "Este documento certifica la aprobación oficial de la modalidad de grado " +
                "conforme a la normatividad académica vigente de la Universidad Surcolombiana. " +
                "La información contenida en este acta es auténtica y verificable.",
                FOOTER_FONT
            );
            legalNote.setAlignment(Element.ALIGN_CENTER);
            legalNote.setSpacingBefore(10);
            legalNote.setSpacingAfter(8);
            document.add(legalNote);

            Paragraph verificationCode = new Paragraph(
                "Código de verificación: " + certificateNumber,
                FontFactory.getFont(FontFactory.COURIER_BOLD, 8, TEXT_GRAY)
            );
            verificationCode.setAlignment(Element.ALIGN_CENTER);
            document.add(verificationCode);

            document.close();
            log.info("PDF generado exitosamente en: {}", filePath);
        } catch (DocumentException | IOException e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el certificado PDF", e);
        }
    }

    // Nuevo método para filas de tabla con fondo personalizado
    private void addStyledTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont, BaseColor bgColor) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(bgColor);
        labelCell.setPadding(8);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setBackgroundColor(bgColor);
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(valueCell);
    }


    private PdfPTable createStyledDataTable(int columns) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(5);
        return table;
    }


    private void addStyledTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setBackgroundColor(new BaseColor(245, 245, 245)); // Fondo gris claro
        labelCell.setPadding(8);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(labelCell);


        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(valueCell);
    }


    private void addHorizontalLine(Document document, BaseColor color) throws DocumentException {
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        lineTable.setSpacingBefore(5);
        lineTable.setSpacingAfter(5);

        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineCell.setBorderWidthBottom(1.5f);
        lineCell.setBorderColorBottom(color);
        lineCell.setFixedHeight(2f);

        lineTable.addCell(lineCell);
        document.add(lineTable);
    }

    private String generateCertificateNumber(StudentModality studentModality) {
        Long programId = studentModality.getProgramDegreeModality().getAcademicProgram().getId();
        int year = LocalDateTime.now().getYear();
        // Buscar el siguiente número disponible
        int nextNumber = 1;
        boolean unique = false;
        while (!unique) {
            String candidate = String.format("ACTA-PROG%d-%d-%04d", programId, year, nextNumber);
            boolean exists = certificateRepository.findAll().stream()
                .anyMatch(cert -> cert.getCertificateNumber().equals(candidate));
            if (!exists) {
                unique = true;
                return candidate;
            }
            nextNumber++;
        }
        // fallback
        return String.format("ACTA-PROG%d-%d-%04d", programId, year, nextNumber);
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
    private String translateModalityStatus(com.SIGMA.USCO.Modalities.Entity.enums.ModalityProcessStatus status) {
        if (status == null) return "No registrado";
        return switch (status) {
            case MODALITY_SELECTED -> "Modalidad seleccionada";
            case UNDER_REVIEW_PROGRAM_HEAD -> "En revisión por Jefatura de Programa";
            case CORRECTIONS_REQUESTED_PROGRAM_HEAD -> "Correcciones solicitadas por Jefatura de Programa";
            case CORRECTIONS_SUBMITTED -> "Correcciones enviadas";
            case CORRECTIONS_APPROVED -> "Correcciones aprobadas";
            case CORRECTIONS_REJECTED_FINAL -> "Correcciones rechazadas (final)";
            case READY_FOR_PROGRAM_CURRICULUM_COMMITTEE -> "Lista para Comité de Currículo";
            case UNDER_REVIEW_PROGRAM_CURRICULUM_COMMITTEE -> "En revisión por Comité de Currículo";
            case CORRECTIONS_REQUESTED_PROGRAM_CURRICULUM_COMMITTEE -> "Correcciones solicitadas por Comité de Currículo";
            case PROPOSAL_APPROVED -> "Propuesta aprobada";
            case DEFENSE_REQUESTED_BY_PROJECT_DIRECTOR -> "Sustentación solicitada por Director de Proyecto";
            case DEFENSE_SCHEDULED -> "Sustentación programada";
            case EXAMINERS_ASSIGNED -> "Jueces asignados";
            case READY_FOR_EXAMINERS -> "Lista para jueces";
            case CORRECTIONS_REQUESTED_EXAMINERS -> "Correcciones solicitadas por jueces";
            case READY_FOR_DEFENSE -> "Lista para sustentación";
            case FINAL_REVIEW_COMPLETED -> "Revisión final completada";
            case DEFENSE_COMPLETED -> "Sustentación realizada";
            case UNDER_EVALUATION_PRIMARY_EXAMINERS -> "En evaluación por jueces principales";
            case DISAGREEMENT_REQUIRES_TIEBREAKER -> "Desacuerdo, requiere desempate";
            case UNDER_EVALUATION_TIEBREAKER -> "En evaluación por juez de desempate";
            case EVALUATION_COMPLETED -> "Evaluación completada";
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

    public Path getCertificatePath(Long studentModalityId) {
        AcademicCertificate certificate = certificateRepository.findByStudentModalityId(studentModalityId)
                .orElseThrow(() -> new RuntimeException("Certificado no encontrado"));
        return Paths.get(certificate.getFilePath());
    }

    @Transactional
    public void updateCertificateStatus(Long certificateId, CertificateStatus status) {
        AcademicCertificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificado no encontrado"));
        certificate.setStatus(status);
        certificateRepository.save(certificate);
    }
}




























