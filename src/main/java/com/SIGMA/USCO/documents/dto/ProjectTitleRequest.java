package com.SIGMA.USCO.documents.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectTitleRequest {

    @NotBlank(message = "El campo 'projectTitle' es requerido.")
    private String projectTitle;
}