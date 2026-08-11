package com.SIGMA.USCO.documents.dto;

import com.SIGMA.USCO.documents.entity.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequiredDocumentDTO {


    @NotNull
    private Long modalityId;

    private Long id;

    @NotBlank
    private String documentName;
    private String allowedFormat;
    private Integer maxFileSizeMB;
    private DocumentType documentType;
    private String description;
    private boolean active;

        /**
     * Indica si este documento requiere la evaluación detallada de propuesta por el jurado.
     * Solo es true para el documento de propuesta de grado; para otros documentos MANDATORY
     * como contratos, formularios, etc., es false.
     */
    private boolean requiresProposalEvaluation;

}
