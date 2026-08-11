package com.SIGMA.USCO.common.web;

import java.util.List;

public record PaginatedResponse<T>(List<T> content, int page, int size,
                                   long totalElements, int totalPages) {

    public static <T> PaginatedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        long totalPages = size <= 0 ? 0 : (totalElements + size - 1) / size;
        return new PaginatedResponse<>(content, page, size, totalElements, (int) totalPages);
    }
}