package com.SIGMA.USCO.documents.service;

import com.SIGMA.USCO.Modalities.entity.StudentModality;
import com.SIGMA.USCO.Modalities.repository.DegreeModalityRepository;
import com.SIGMA.USCO.Modalities.repository.StudentModalityRepository;
import com.SIGMA.USCO.Users.entity.User;
import com.SIGMA.USCO.Users.repository.ProgramAuthorityRepository;
import com.SIGMA.USCO.Users.repository.UserRepository;
import com.SIGMA.USCO.shared.util.ResourceAccessPolicy;
import com.SIGMA.USCO.shared.util.TranslationUtils;
import com.SIGMA.USCO.documents.dto.StatusHistoryDTO;
import com.SIGMA.USCO.documents.entity.StudentDocument;
import com.SIGMA.USCO.documents.entity.StudentDocumentStatusHistory;
import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.repository.RequiredDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentRepository;
import com.SIGMA.USCO.documents.repository.StudentDocumentStatusHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("T1.8 - Regresión T0.7: historial de documentos traducido por el canónico (14/14 estados)")
class DocumentServiceStatusTest {

    @Mock
    private RequiredDocumentRepository requiredDocumentRepository;
    @Mock
    private DegreeModalityRepository degreeModalityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentDocumentRepository studentDocumentRepository;
    @Mock
    private StudentDocumentStatusHistoryRepository documentHistoryRepository;
    @Mock
    private StudentModalityRepository studentModalityRepository;
    @Mock
    private ResourceAccessPolicy resourceAccessPolicy;
    @Mock
    private ProgramAuthorityRepository programAuthorityRepository;

    @InjectMocks
    private DocumentService service;

    @Test
    @DisplayName("T0.7: un estado fuera de los 6 que cubría el método viejo se traduce (EDIT_REQUEST_REJECTED)")
    void historyUsesCanonicalTranslationForStatusOldMethodMissed() {
        User leader = User.builder().id(1L).name("Ana").lastName("Perez").email("ana@usco.edu.co").build();
        StudentModality modality = StudentModality.builder().id(1L).leader(leader).build();
        StudentDocument document = StudentDocument.builder().id(1L).studentModality(modality).build();

        when(studentDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentHistoryRepository.findByStudentDocumentIdOrderByChangeDateAsc(1L)).thenReturn(List.of(
                StudentDocumentStatusHistory.builder()
                        .id(1L)
                        .studentDocument(document)
                        .status(DocumentStatus.EDIT_REQUEST_REJECTED)
                        .changeDate(LocalDateTime.of(2026, 1, 15, 10, 0))
                        .responsible(leader)
                        .observations("Edición rechazada por el jurado")
                        .build()
        ));

        List<StatusHistoryDTO> history = service.getDocumentHistory(1L, leader);

        assertThat(history).hasSize(1);
        StatusHistoryDTO entry = history.get(0);
        assertThat(entry.getStatus()).isEqualTo(DocumentStatus.EDIT_REQUEST_REJECTED.name());
        assertThat(entry.getDescription())
                .isEqualTo(TranslationUtils.translateDocumentStatus(DocumentStatus.EDIT_REQUEST_REJECTED))
                .doesNotContain("Estado del documento no definido");
        assertThat(entry.getResponsible()).isEqualTo("ana@usco.edu.co");
    }

    @Test
    @DisplayName("T0.7: los 14 estados del enum se traducen, ninguno cae en el default del método viejo")
    void canonicalTranslationCoversAllDocumentStatuses() {
        for (DocumentStatus status : DocumentStatus.values()) {
            String translation = TranslationUtils.translateDocumentStatus(status);

            assertThat(translation)
                    .as("Traducción de %s", status)
                    .isNotBlank()
                    .isNotEqualTo("Estado del documento no definido")
                    .isNotEqualTo(status.name());
        }
    }

    @Test
    @DisplayName("T0.7: estados distintos producen descripciones distintas")
    void distinctStatusesProduceDistinctDescriptions() {
        assertThat(TranslationUtils.translateDocumentStatus(DocumentStatus.PENDING))
                .isNotEqualTo(TranslationUtils.translateDocumentStatus(DocumentStatus.CORRECTION_RESUBMITTED));
    }
}