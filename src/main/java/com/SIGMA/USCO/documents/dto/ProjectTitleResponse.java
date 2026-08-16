package com.SIGMA.USCO.documents.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectTitleResponse(boolean success, String projectTitle, String message) {
}
