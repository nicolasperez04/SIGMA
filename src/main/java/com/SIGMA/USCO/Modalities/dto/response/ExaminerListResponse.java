package com.SIGMA.USCO.Modalities.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de getExaminersForModality. Cubre las 2 formas del Map anterior:
 * - sin jurados: {success, studentModalityId, examiners: [], message}
 * - con jurados: {success, studentModalityId, modalityName, modalityStatus, examinersCount, examiners}
 * Los campos con @NON_NULL solo se serializan en la rama que los produce.
 */
public record ExaminerListResponse(
        boolean success,
        Long studentModalityId,
        List<ExaminerInfo> examiners,
        @JsonInclude(JsonInclude.Include.NON_NULL) String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) String modalityName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String modalityStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer examinersCount) {

    public record ExaminerInfo(
            Long examinerId,
            String examinerName,
            String examinerLastName,
            String examinerEmail,
            String examinerType,
            String examinerTypeDescription,
            LocalDateTime assignmentDate) {
    }
}