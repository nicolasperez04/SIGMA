package com.SIGMA.USCO.Users.service;

import com.SIGMA.USCO.Modalities.Entity.StudentModality;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.Repository.StudentModalityRepository;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.Faculty;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.Users.Entity.User;
import com.SIGMA.USCO.academic.dto.StudentProfileRequest;
import com.SIGMA.USCO.Users.dto.response.AcademicHistoryProfileResponse;
import com.SIGMA.USCO.Users.dto.response.StudentDocumentDTO;
import com.SIGMA.USCO.Users.dto.response.StudentResponse;
import com.SIGMA.USCO.academic.dto.AcademicHistoryExtractionResult;
import com.SIGMA.USCO.academic.repository.AcademicProgramRepository;
import com.SIGMA.USCO.academic.repository.FacultyRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.academic.service.AcademicHistoryPdfParserService;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.common.exception.NotFoundException;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.academic.entity.AcademicHistoryPdf;
import com.SIGMA.USCO.academic.repository.AcademicHistoryPdfRepository;
import com.SIGMA.USCO.common.util.ResourceAccessPolicy;
import com.SIGMA.USCO.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentModalityRepository studentModalityRepository;
    private final StudentModalityMemberRepository studentModalityMemberRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final AcademicHistoryPdfRepository academicHistoryPdfRepository;
    private final FacultyRepository facultyRepository;
    private final AcademicProgramRepository academicProgramRepository;
    private final AcademicHistoryPdfParserService academicHistoryPdfParserService;
    private final ResourceAccessPolicy resourceAccessPolicy;

    @Transactional
    public void updateStudentProfile(StudentProfileRequest request) {

        User user = SecurityUtils.getCurrentUser();
        String email = user.getEmail();

        StudentProfile studentProfile = studentProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> {
                    StudentProfile sp = new StudentProfile();
                    sp.setUser(user);
                    return sp;
                });



        String studentCode = extractStudentCodeFromEmail(email);
        if (studentCode == null) {
            throw new ValidationException(
                    "No se pudo extraer el código de estudiante del email. " +
                    "El formato esperado es: u[NUMEROS]@dominio (ejemplo: 202xxxxxxxx@usco.edu.co)"
            );
        }

        Long effectiveSemester = request.getSemester();
        if (effectiveSemester == null) {
            effectiveSemester = inferCurrentSemesterFromStudentCode(studentCode);
            if (effectiveSemester == null) {
                throw new ValidationException(
                        "No fue posible inferir el semestre desde el código estudiantil. " +
                        "Ingrese el semestre manualmente."
                );
            }
        }

        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() ->
                        new NotFoundException("La facultad con ID " + request.getFacultyId() + " no existe")
                );



        AcademicProgram program = academicProgramRepository.findById(request.getAcademicProgramId())
                .orElseThrow(() ->
                        new NotFoundException("El programa académico con ID " + request.getAcademicProgramId() + " no existe")
                );


        if (!program.getFaculty().getId().equals(faculty.getId())) {
            throw new ValidationException(
                    "El programa académico no pertenece a la facultad seleccionada."
            );
        }



        if (request.getApprovedCredits() > program.getTotalCredits()) {
            throw new ValidationException(
                    "Los créditos aprobados no pueden superar el total del programa (" +
                            program.getTotalCredits() + ")."
            );
        }



        studentProfile.setFaculty(faculty);
        studentProfile.setAcademicProgram(program);
        studentProfile.setStudentCode(studentCode);
        studentProfile.setSemester(effectiveSemester);
        studentProfile.setGpa(request.getGpa());
        studentProfile.setApprovedCredits(request.getApprovedCredits());

        studentProfileRepository.save(studentProfile);
    }

    @Transactional
    public AcademicHistoryProfileResponse updateStudentProfileFromAcademicHistory(MultipartFile file) {

        User user = SecurityUtils.getCurrentUser();
        String email = user.getEmail();

        String studentCode = extractStudentCodeFromEmail(email);
        if (studentCode == null) {
            throw new ValidationException(
                    "No se pudo extraer el código de estudiante del email autenticado."
            );
        }

        Long inferredSemester = inferCurrentSemesterFromStudentCode(studentCode);
        if (inferredSemester == null) {
            throw new ValidationException(
                    "No fue posible calcular el semestre actual a partir del código estudiantil."
            );
        }

        AcademicHistoryExtractionResult extracted;
        try {
            extracted = academicHistoryPdfParserService.extract(file);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (Error e) {
            throw new ValidationException(
                    "No fue posible procesar el PDF en este servidor. " +
                    "Intenta nuevamente con el historial académico original en PDF."
            );
        } catch (Exception e) {
            logger.error("Error inesperado al procesar el PDF del historial académico", e);
            throw new ValidationException("No fue posible procesar el PDF.");
        }

        Optional<AcademicProgram> programOpt = findProgramByExtractedName(extracted.getProgramName());
        if (programOpt.isEmpty()) {
            List<String> activePrograms = academicProgramRepository.findByActiveTrue().stream()
                    .map(AcademicProgram::getName)
                    .sorted()
                    .toList();

            throw new ValidationException(
                    "No se pudo asociar el programa extraído ('" + extracted.getProgramName() +
                            "') con un programa académico activo. Programas disponibles: " + activePrograms
            );
        }

        AcademicProgram program = programOpt.get();
        Faculty faculty = program.getFaculty();

        if (extracted.getApprovedCredits() < 0) {
            throw new ValidationException("Los créditos aprobados extraídos no son válidos.");
        }

        if (extracted.getApprovedCredits() > extracted.getTotalCreditsInPdf()) {
            throw new ValidationException(
                    "El historial indica créditos aprobados mayores al total reportado en el PDF."
            );
        }

        if (extracted.getGpa() < 0.0 || extracted.getGpa() > 5.0) {
            throw new ValidationException("El promedio extraído está fuera del rango permitido (0.0 - 5.0).");
        }

        if (extracted.getApprovedCredits() > program.getTotalCredits()) {
            throw new ValidationException(
                    "Los créditos aprobados extraídos superan el total del programa en SIGMA (" +
                            program.getTotalCredits() + ")."
            );
        }

        StudentProfile studentProfile = studentProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> {
                    StudentProfile sp = new StudentProfile();
                    sp.setUser(user);
                    return sp;
                });

        studentProfile.setFaculty(faculty);
        studentProfile.setAcademicProgram(program);
        studentProfile.setStudentCode(studentCode);
        studentProfile.setApprovedCredits(extracted.getApprovedCredits());
        studentProfile.setGpa(extracted.getGpa());
        studentProfile.setSemester(inferredSemester);

        studentProfileRepository.save(studentProfile);

        // Guardar el PDF del historial académico en filesystem y BD
        String filePath = saveAcademicHistoryPdf(file, user, studentCode);

        AcademicHistoryProfileResponse.AcademicHistoryProfileResponseBuilder response =
                AcademicHistoryProfileResponse.builder()
                        .message("Perfil académico actualizado automáticamente desde historial académico")
                        .programNameExtracted(extracted.getProgramName())
                        .academicProgramMatched(program.getName())
                        .faculty(faculty.getName())
                        .approvedCredits(extracted.getApprovedCredits())
                        .programTotalCreditsInPdf(extracted.getTotalCreditsInPdf())
                        .programTotalCreditsInSigma(program.getTotalCredits())
                        .gpa(extracted.getGpa())
                        .semester(inferredSemester)
                        .semesterSource("STUDENT_CODE");

        // Agregar información del archivo guardado a la respuesta
        if (filePath != null) {
            response.pdfFilePath(filePath);
            response.pdfFileName(file.getOriginalFilename());
            response.pdfStored(true);
        } else {
            response.pdfStored(false);
            response.pdfWarning("El PDF se procesó pero no se logró almacenar en el servidor");
        }

        return response.build();
    }


    private String extractStudentCodeFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }


        String localPart = email.substring(0, email.indexOf("@"));


        if (localPart.matches("\\d+")) {
            return localPart;
        }

        if (localPart.startsWith("u") && localPart.length() > 1) {
            String numbersAfterU = localPart.substring(1);


            if (numbersAfterU.matches("\\d+")) {
                return numbersAfterU;
            }
        }

        return null;
    }

    private Long inferCurrentSemesterFromStudentCode(String studentCode) {
        if (studentCode == null || studentCode.length() < 5) {
            return null;
        }

        String yearPart = studentCode.substring(0, 4);
        char admissionSemesterChar = studentCode.charAt(4);
        if (!yearPart.matches("\\d{4}") || (admissionSemesterChar != '1' && admissionSemesterChar != '2')) {
            return null;
        }

        int admissionYear = Integer.parseInt(yearPart);
        int admissionSemester = admissionSemesterChar - '0';

        java.time.LocalDate today = java.time.LocalDate.now();
        int currentYear = today.getYear();
        int currentSemester = today.getMonthValue() <= 6 ? 1 : 2;

        int admissionIndex = admissionYear * 2 + (admissionSemester - 1);
        int currentIndex = currentYear * 2 + (currentSemester - 1);
        int semester = (currentIndex - admissionIndex) + 1;

        if (semester < 1) {
            return null;
        }

        return (long) semester;
    }

    private Optional<AcademicProgram> findProgramByExtractedName(String extractedProgramName) {
        if (extractedProgramName == null || extractedProgramName.isBlank()) {
            return Optional.empty();
        }

        List<AcademicProgram> programs = academicProgramRepository.findByActiveTrue();
        if (programs.isEmpty()) {
            programs = academicProgramRepository.findAll();
        }

        String target = normalizeText(extractedProgramName);

        List<AcademicProgram> exactMatches = programs.stream()
                .filter(p -> normalizeText(p.getName()).equals(target))
                .toList();

        if (exactMatches.size() == 1) {
            return Optional.of(exactMatches.get(0));
        }

        List<AcademicProgram> containsMatches = programs.stream()
                .filter(p -> {
                    String normalizedName = normalizeText(p.getName());
                    return target.contains(normalizedName) || normalizedName.contains(target);
                })
                .toList();

        if (containsMatches.size() == 1) {
            return Optional.of(containsMatches.get(0));
        }

        if (containsMatches.isEmpty()) {
            return Optional.empty();
        }

        Map<AcademicProgram, Long> scored = containsMatches.stream()
                .collect(Collectors.toMap(
                        p -> p,
                        p -> overlapScore(target, normalizeText(p.getName()))
                ));

        long max = scored.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        List<AcademicProgram> top = scored.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();

        return top.size() == 1 ? Optional.of(top.get(0)) : Optional.empty();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private long overlapScore(String left, String right) {
        if (left.isBlank() || right.isBlank()) {
            return 0L;
        }

        List<String> leftTokens = List.of(left.split(" "));
        List<String> rightTokens = List.of(right.split(" "));
        return leftTokens.stream().filter(rightTokens::contains).count();
    }


    @Transactional(readOnly = true)
    public StudentResponse getStudentProfile() {

        User user = SecurityUtils.getCurrentUser();
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(user.getId());

        return StudentResponse.builder()
                .name(user.getName())
                .lastname(user.getLastName())
                .email(user.getEmail())
                .approvedCredits(profileOpt.map(StudentProfile::getApprovedCredits).orElse(null))
                .gpa(profileOpt.map(StudentProfile::getGpa).orElse(null))
                .semester(profileOpt.map(StudentProfile::getSemester).orElse(null))
                .studentCode(profileOpt.map(StudentProfile::getStudentCode).orElse(null))
                .faculty( profileOpt.map(StudentProfile::getFaculty).map(Faculty::getName).orElse(null))
                .academicProgram(profileOpt.map(StudentProfile::getAcademicProgram).map(AcademicProgram::getName).orElse(null))

                .build();

    }

    @Transactional(readOnly = true)
    public List<StudentDocumentDTO> getMyDocuments(){

        User currentUser = SecurityUtils.getCurrentUser();

        // Buscar la modalidad activa donde el usuario es miembro
        List<StudentModality> activeModalities =
                studentModalityMemberRepository.findActiveModalitiesByUserId(currentUser.getId());

        if (activeModalities.isEmpty()) {
            throw new NotFoundException("No se encontró ninguna modalidad activa asociada al estudiante");
        }

        // Obtener la modalidad más reciente
        StudentModality latestModality = activeModalities.get(0);
        Long studentModalityId = latestModality.getId();

        List<StudentDocument> documents = studentDocumentRepository.findByStudentModalityId(studentModalityId);

        // Filtrar solo documentos MANDATORY y SECONDARY (excluir CANCELLATION)
        return documents.stream()
                .filter(doc -> doc.getDocumentConfig().getDocumentType() == DocumentType.MANDATORY ||
                              doc.getDocumentConfig().getDocumentType() == DocumentType.SECONDARY)
                .map(doc -> StudentDocumentDTO.builder()
                        .notes(doc.getNotes())
                        .filePath(doc.getFilePath())
                        .studentDocumentId(doc.getId())
                        .uploadedAt(doc.getUploadDate())
                        .documentName(doc.getDocumentConfig().getDocumentName())
                        .documentType(doc.getDocumentConfig().getDocumentType())
                        .status(doc.getStatus())
                        .build())
                .toList();

    }

    @Transactional(readOnly = true)
    public Resource viewMyDocument(Long studentDocumentId) {

        User currentUser = SecurityUtils.getCurrentUser();

        StudentDocument document = studentDocumentRepository.findById(studentDocumentId)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado"));

        resourceAccessPolicy.requireActiveMember(
                document.getStudentModality().getId(),
                currentUser,
                "No tienes permiso para ver este documento"
        );

        // Leer el archivo desde el sistema de archivos
        Path filePath = Paths.get(document.getFilePath());
        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new NotFoundException("Archivo no encontrado o no legible");
        }

        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException("Archivo no encontrado o no legible");
        }

        return resource;
    }

    private String saveAcademicHistoryPdf(MultipartFile file, User user, String studentCode) {
        try {
            // Validar que el archivo no sea nulo o vacío
            if (file == null || file.isEmpty()) {
                logger.warn("Archivo nulo o vacío para estudiante {}", studentCode);
                return null;
            }

            // Obtener nombre original y extensión
            String originalFilename = file.getOriginalFilename();
            
            // Generar nombre único para el archivo (similar a uploadRequiredDocument)
            String uniqueFileName = UUID.randomUUID() + "_" + originalFilename;

            // Crear estructura de carpetas: uploadDir/Historial_Academico/{userName}_{lastName}_{userId}/
            // Similar a la estructura de uploadRequiredDocument
            String studentFolder = user.getName() + user.getLastName() + "_" +
                    user.getLastName() + "_" +
                    user.getId();

            Path basePath = Paths.get(uploadDir, "Historial_Academico", studentFolder);
            Files.createDirectories(basePath);

            // Guardar archivo en filesystem
            Path fullPath = basePath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);
            
            logger.info("PDF del historial académico guardado en filesystem para estudiante {}: {}", 
                    studentCode, fullPath);

            // Obtener perfil del estudiante
            Optional<StudentProfile> studentProfileOpt = studentProfileRepository.findByUserId(user.getId());
            if (studentProfileOpt.isEmpty()) {
                logger.warn("No se encontró StudentProfile para guardar AcademicHistoryPdf - estudiante: {}", studentCode);
                return fullPath.toString();
            }
            StudentProfile studentProfile = studentProfileOpt.get();

            // Crear registro en AcademicHistoryPdf (tabla principal para historial académico)
            AcademicHistoryPdf academicHistoryPdf = AcademicHistoryPdf.builder()
                    .studentProfile(studentProfile)
                    .uploadedBy(user)
                    .filePath(fullPath.toString())
                    .originalFileName(originalFilename)
                    .uploadDate(LocalDateTime.now())
                    .fileSizeBytes(file.getSize())
                    .notes("PDF del historial académico subido por el estudiante")
                    .build();

            // Guardar datos extraídos para búsqueda y auditoría
            if (studentProfile.getAcademicProgram() != null) {
                academicHistoryPdf.setExtractedProgramName(studentProfile.getAcademicProgram().getName());
            }
            academicHistoryPdf.setExtractedApprovedCredits(studentProfile.getApprovedCredits());
            academicHistoryPdf.setExtractedGpa(studentProfile.getGpa());

            academicHistoryPdfRepository.save(academicHistoryPdf);
            logger.info("AcademicHistoryPdf guardado en BD para estudiante {}: ID = {}", 
                    studentCode, academicHistoryPdf.getId());

            // También guardar en StudentDocument para que aparezca en "Mis documentos"
            StudentDocument document = new StudentDocument();
            document.setFileName(originalFilename);
            document.setFilePath(fullPath.toString());
            document.setUploadDate(LocalDateTime.now());
            document.setNotes("Historial académico PDF - " + originalFilename);
            document.setStatus(DocumentStatus.PENDING);

            // Asociar a la modalidad activa del estudiante (si existe)
            Optional<StudentModality> activeModality = studentModalityMemberRepository
                    .findActiveModalitiesByUserId(user.getId())
                    .stream()
                    .findFirst();

            if (activeModality.isPresent()) {
                document.setStudentModality(activeModality.get());
                studentDocumentRepository.save(document);
                logger.info("StudentDocument guardado para estudiante {} en modalidad: {}", 
                        studentCode, activeModality.get().getId());
            } else {
                logger.warn("No se encontró modalidad activa para guardar StudentDocument - estudiante: {}", studentCode);
            }

            return fullPath.toString();
        } catch (IOException e) {
            logger.error("Error de IO al guardar PDF del historial académico para estudiante {}: {}", 
                    studentCode, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Error inesperado al guardar PDF del historial académico para estudiante {}: {}", 
                    studentCode, e.getMessage());
            return null;
        }
    }

}
