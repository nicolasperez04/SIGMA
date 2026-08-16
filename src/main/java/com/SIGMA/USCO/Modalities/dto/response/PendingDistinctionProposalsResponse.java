package com.SIGMA.USCO.Modalities.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Shape de getPendingDistinctionProposals:
 * {success, totalPending, pendingDistinctionProposals}.
 * Las filas internas siguen siendo Map (construcción local, 15 claves — YAGNI).
 */
public record PendingDistinctionProposalsResponse(Boolean success, Integer totalPending,
                                                  List<Map<String, Object>> pendingDistinctionProposals) {
}