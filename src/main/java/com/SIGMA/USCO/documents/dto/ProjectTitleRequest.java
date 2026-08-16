package com.SIGMA.USCO.documents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectTitleRequest {

    @NotBlank(message = "El campo 'projectTitle' es requerido.")
    @Size(max = 500, message = "El título no puede exceder 500 caracteres.")
    private String projectTitle;
}