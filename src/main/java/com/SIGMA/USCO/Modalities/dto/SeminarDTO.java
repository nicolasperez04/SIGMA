package com.SIGMA.USCO.Modalities.dto;

import com.SIGMA.USCO.Modalities.entity.enums.SeminarStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeminarDTO {

    @NotBlank(message = "El nombre del seminario es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String name;

    @Size(max = 4000, message = "La descripción no puede exceder 4000 caracteres")
    private String description;

    private SeminarStatus status;

    @NotNull(message = "El costo total es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El costo debe ser mayor a 0")
    private BigDecimal totalCost;

    @NotNull(message = "El mínimo de participantes es obligatorio")
    @Min(value = 15, message = "El mínimo de participantes debe ser al menos 15")
    private Integer minParticipants;

    @NotNull(message = "El máximo de participantes es obligatorio")
    @Max(value = 35, message = "El máximo de participantes no puede exceder 35")
    private Integer maxParticipants;

    @NotNull(message = "La intensidad horaria es obligatoria")
    @Min(value = 160, message = "La intensidad horaria mínima debe ser de 160 horas")
    private Integer totalHours;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

}


