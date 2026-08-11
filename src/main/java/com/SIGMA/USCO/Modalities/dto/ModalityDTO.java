package com.SIGMA.USCO.Modalities.dto;


import com.SIGMA.USCO.Modalities.Entity.enums.ModalityStatus;
import com.SIGMA.USCO.documents.dto.RequiredDocumentDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModalityDTO {

    private Long id;

    @NotNull(message = "La facultad es obligatoria.")
    private Long facultyId;
    private String facultyName;

    @NotBlank(message = "El nombre de la modalidad es obligatorio.")
    private String name;
    private String description;
    private ModalityStatus status;
    private Double requiredCredits;
    private List<RequirementDTO> requirements;
    private List<RequiredDocumentDTO> documents;

}
