package com.SIGMA.USCO.Users.service;

import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.dto.response.AcademicHistoryProfileResponse;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.dto.AcademicHistoryExtractionResult;
import com.SIGMA.USCO.academic.entity.AcademicHistoryPdf;
import com.SIGMA.USCO.academic.entity.AcademicProgram;
import com.SIGMA.USCO.academic.entity.Faculty;
import com.SIGMA.USCO.academic.entity.StudentProfile;
import com.SIGMA.USCO.academic.repository.AcademicHistoryPdfRepository;
import com.SIGMA.USCO.academic.repository.AcademicProgramRepository;
import com.SIGMA.USCO.academic.repository.FacultyRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.academic.service.AcademicHistoryPdfParserService;
import com.SIGMA.USCO.shared.util.ResourceAccessPolicy;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("T1.7 - Regresión T0.12: guardado del historial académico sin bloque StudentDocument")
class StudentServiceAcademicHistoryTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private StudentModalityRepository studentModalityRepository;
    @Mock
    private StudentModalityMemberRepository studentModalityMemberRepository;
    @Mock
    private StudentDocumentRepository studentDocumentRepository;
    @Mock
    private AcademicHistoryPdfRepository academicHistoryPdfRepository;
    @Mock
    private FacultyRepository facultyRepository;
    @Mock
    private AcademicProgramRepository academicProgramRepository;
    @Mock
    private AcademicHistoryPdfParserService academicHistoryPdfParserService;
    @Mock
    private ResourceAccessPolicy resourceAccessPolicy;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    @InjectMocks
    private StudentService service;

    @TempDir
    Path tempDir;

    private User student;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(200L)
                .name("Nicolas")
                .lastName("Perez")
                .email("20221204357@usco.edu.co")
                .build();

        Faculty faculty = Faculty.builder().id(1L).name("Facultad de Ingeniería").code("FIET").build();
        AcademicProgram program = AcademicProgram.builder()
                .id(1L)
                .name("INGENIERIA DE SOFTWARE")
                .code("ING_SOFTWARE")
                .totalCredits(160L)
                .faculty(faculty)
                .build();

        StudentProfile profile = StudentProfile.builder()
                .id(200L)
                .user(student)
                .academicProgram(program)
                .faculty(faculty)
                .studentCode("20221204357")
                .approvedCredits(142L)
                .gpa(4.21)
                .semester(10L)
                .build();

        when(academicHistoryPdfParserService.extract(any())).thenReturn(AcademicHistoryExtractionResult.builder()
                .programName("INGENIERIA DE SOFTWARE")
                .approvedCredits(142L)
                .totalCreditsInPdf(165L)
                .gpa(4.21)
                .build());
        when(academicProgramRepository.findByActiveTrue()).thenReturn(List.of(program));
        when(userRepository.findById(200L)).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByUserId(200L)).thenReturn(Optional.of(profile));
        when(academicHistoryPdfRepository.save(any(AcademicHistoryPdf.class)))
                .thenReturn(AcademicHistoryPdf.builder().id(1L).build());
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);

        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(student, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("T0.12: el historial se guarda sin excepción, persiste AcademicHistoryPdf y NO crea StudentDocument")
    void saveAcademicHistoryPdfPersistsAcademicHistoryPdfOnly() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "historial.pdf", "application/pdf", new byte[]{1, 2, 3});

        AcademicHistoryProfileResponse response = service.updateStudentProfileFromAcademicHistory(file, student);

        assertThat(response.getPdfStored()).isTrue();
        assertThat(response.getPdfFilePath()).isNotBlank().endsWith("historial.pdf");
        assertThat(response.getPdfFileName()).isEqualTo("historial.pdf");

        verify(academicHistoryPdfRepository).save(any(AcademicHistoryPdf.class));
        verify(studentDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("T0.12: el PDF se guarda en la carpeta del estudiante bajo Historial_Academico")
    void academicHistoryPdfIsStoredInStudentFolder() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "historial.pdf", "application/pdf", new byte[]{1, 2, 3});

        AcademicHistoryProfileResponse response = service.updateStudentProfileFromAcademicHistory(file, student);

        assertThat(response.getPdfFilePath())
                .contains("Historial_Academico")
                .contains("Nicolas_Perez_200");
        assertThat(Path.of(response.getPdfFilePath())).exists();
    }
}