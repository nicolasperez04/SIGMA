package com.SIGMA.USCO.common.web;

public record OperationResultResponse(boolean success, String message, Long studentModalityId) {

    public OperationResultResponse(boolean success, String message) {
        this(success, message, null);
    }
}