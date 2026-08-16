package com.SIGMA.USCO.Modalities.service;

import com.SIGMA.USCO.Modalities.entity.DegreeModality;
import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.entity.enums.ModalityProcessStatus;
import com.SIGMA.USCO.Modalities.repository.StudentModalityMemberRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Modalities.dto.response.UploadDocumentResponse;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.academic.entity.ProgramDegreeModality;
import com.SIGMA.USCO.common.exception.ValidationException;
import com.SIGMA.USCO.shared.util.ResourceAccessPolicy;
import com.SIGMA.USCO.documents.entity.RequiredDocument;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import com.SIGMA.USCO.documents.repository.DocumentEditRequestRepository;
import com.SIGMA.USCO.documents.repository.DocumentEditRequestVoteRepository;
import com.SIGMA.USCO.documents.repository.ExaminerDocumentReviewRepository;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentStatusHistoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("T1.10 - Regresión T0.10: validación exacta de extensión de archivo (sin falsos positivos de contains)")
class ModalityDocumentUploadFormatTest {

    @Mock
    private StudentModalityRepository studentModalityRepository;
    @Mock
    private StudentModalityMemberRepository studentModalityMemberRepository;
    @Mock
    private RequiredDocumentRepository requiredDocumentRepository;
    @Mock
    private StudentDocumentRepository studentDocumentRepository;
    @Mock
    private StudentDocumentStatusHistoryRepository documentHistoryRepository;
    @Mock
    private ProgramAuthorityRepository programAuthorityRepository;
    @Mock
    private ExaminerDocumentReviewRepository examinerDocumentReviewRepository;
    @Mock
    private DocumentEditRequestRepository documentEditRequestRepository;
    @Mock
    private DocumentEditRequestVoteRepository documentEditRequestVoteRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ResourceAccessPolicy resourceAccessPolicy;
    @Mock
    private ModalityStatusTransition modalityStatusTransition;

    @InjectMocks
    private ModalityDocumentService service;

    @TempDir
    Path tempDir;

    private User uploader;
    private StudentModality modality;
    private RequiredDocument requiredDocument;

    @BeforeEach
    void setUp() {
        uploader = User.builder().id(300L).name("Estudiante").lastName("Prueba").email("20221204357@usco.edu.co").build();

        DegreeModality degreeModality = DegreeModality.builder()
                .id(7L)
                .name("SEMINARIO DE GRADO")
                .build();
        ProgramDegreeModality programDegreeModality = ProgramDegreeModality.builder()
                .degreeModality(degreeModality)
                .build();

        modality = StudentModality.builder()
                .id(1L)
                .leader(uploader)
                .status(ModalityProcessStatus.PROPOSAL_APPROVED)
                .programDegreeModality(programDegreeModality)
                .build();

        requiredDocument = RequiredDocument.builder()
                .id(1L)
                .modality(degreeModality)
                .documentName("Propuesta de grado")
                .allowedFormat("pdf,docx")
                .maxFileSizeMB(null)
                .documentType(DocumentType.MANDATORY)
                .build();

        when(studentModalityRepository.findById(1L)).thenReturn(Optional.of(modality));
        when(studentModalityMemberRepository.isActiveMember(1L, 300L)).thenReturn(true);
        when(requiredDocumentRepository.findById(1L)).thenReturn(Optional.of(requiredDocument));
        when(requiredDocumentRepository.existsByIdAndModalityId(1L, 7L)).thenReturn(true);

        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(uploader, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("T0.10: tesis.doc NO pasa aunque 'pdf,docx'.contains('doc') sea true")
    void docExtensionIsRejectedDespiteSubstringMatch() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "tesis.doc", "application/msword", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.uploadRequiredDocument(1L, 1L, file, uploader))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Formato de archivo no permitido");

        verify(studentDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("T0.10: tesis.docx sí pasa la validación de formato y completa la subida")
    void docxExtensionIsAcceptedAndUploadCompletes() throws IOException {
        StudentDocument existing = StudentDocument.builder()
                .id(50L)
                .studentModality(modality)
                .documentConfig(requiredDocument)
                .status(DocumentStatus.PENDING)
                .build();
        when(studentDocumentRepository.findByStudentModalityIdAndDocumentConfigId(1L, 1L))
                .thenReturn(Optional.of(existing));

        MockMultipartFile file = new MockMultipartFile(
                "file", "tesis.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{1, 2, 3});

        UploadDocumentResponse result = service.uploadRequiredDocument(1L, 1L, file, uploader);

        assertThat(result.message()).isEqualTo("Documento subido correctamente");
        assertThat(result.documentStatus()).isEqualTo(DocumentStatus.PENDING.name());
        assertThat(result.modalityStatus()).isEqualTo(ModalityProcessStatus.PROPOSAL_APPROVED.name());
        verify(studentDocumentRepository).save(any(StudentDocument.class));
    }
}