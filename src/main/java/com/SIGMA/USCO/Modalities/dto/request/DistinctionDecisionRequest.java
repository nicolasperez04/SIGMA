package com.SIGMA.USCO.Modalities.dto.request;

/**
 * Body opcional del endpoint accept-distinction: { "notes": "..." }.
 * La ausencia del body o de "notes" se traduce a null.
 */
public record DistinctionDecisionRequest(String notes) {
}
