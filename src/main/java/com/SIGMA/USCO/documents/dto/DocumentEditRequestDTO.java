package com.SIGMA.USCO.documents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO de entrada para que el estudiante solicite la edición de un documento aprobado.
 */
@Data
public class DocumentEditRequestDTO {

    /** Motivo o justificación de la solicitud de edición */
    @NotBlank(message = "El motivo de la solicitud es obligatorio")
    // ponytail: max alineado con @Column(length=200000) de DocumentEditRequest.reason (texto libre de justificación)
    @Size(min = 20, max = 200000, message = "El motivo debe tener entre 20 y 200000 caracteres")
    private String reason;
}
