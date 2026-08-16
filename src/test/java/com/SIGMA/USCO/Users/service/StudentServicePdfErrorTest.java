package com.SIGMA.USCO.Users.service;

import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.academic.repository.AcademicHistoryPdfRepository;
import com.SIGMA.USCO.academic.repository.AcademicProgramRepository;
import com.SIGMA.USCO.academic.repository.FacultyRepository;
import com.SIGMA.USCO.academic.repository.StudentProfileRepository;
import com.SIGMA.USCO.academic.service.AcademicHistoryPdfParserService;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.shared.util.ResourceAccessPolicy;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("T1.9 - Regresión T0.6: error del parser de historial sin fuga de detalle interno")
class StudentServicePdfErrorTest {

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

    @InjectMocks
    private StudentService service;

    private User student;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(200L)
                .name("Nicolas")
                .lastName("Perez")
                .email("20221204357@usco.edu.co")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(student, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("T0.6: el parser falla con mensaje interno y el servicio lanza ValidationException con mensaje FIJO")
    void parserFailureThrowsValidationExceptionWithFixedMessage() {
        when(academicHistoryPdfParserService.extract(any()))
                .thenThrow(new IllegalArgumentException("detalle interno del parser XYZ"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "historial.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.updateStudentProfileFromAcademicHistory(file, student))
                .isInstanceOf(ValidationException.class)
                .hasMessage("No fue posible procesar el PDF del historial académico.");
    }

    @Test
    @DisplayName("T0.6: el mensaje expuesto al cliente no contiene el detalle interno del parser")
    void parserFailureDoesNotLeakInternalDetail() {
        when(academicHistoryPdfParserService.extract(any()))
                .thenThrow(new IllegalArgumentException("detalle interno del parser XYZ"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "historial.pdf", "application/pdf", new byte[]{1, 2, 3});

        Throwable thrown = catchThrowable(() -> service.updateStudentProfileFromAcademicHistory(file, student));

        assertThat(thrown).isInstanceOf(ValidationException.class);
        assertThat(thrown.getMessage())
                .doesNotContain("detalle interno")
                .doesNotContain("XYZ")
                .isEqualTo("No fue posible procesar el PDF del historial académico.");
    }
}