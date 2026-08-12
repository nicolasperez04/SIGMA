package com.SIGMA.USCO.Users.dto.response;

import com.SIGMA.USCO.documents.entity.enums.DocumentStatus;
import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentDocumentDTO {

    private String notes;
    private String filePath;
    private Long studentDocumentId;
    private LocalDateTime uploadedAt;
    private String documentName;
    private DocumentType documentType;
    private DocumentStatus status;
}
