package com.SIGMA.USCO.common.exception;

import java.util.List;

public class RequirementsValidationException extends ValidationException {

    // ponytail: List<?> en vez de List<ValidationItemDTO> — common/ no puede depender de Modalities/ (regla ArchUnit common_should_not_depend_on_business_modules)
    private final List<?> results;

    public RequirementsValidationException(String message, List<?> results) {
        super(message);
        this.results = results;
    }

    public List<?> getResults() {
        return results;
    }
}