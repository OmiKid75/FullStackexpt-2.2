package com.example.campusresources.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable JSON shape for every paginated response (instead of exposing Spring's PageImpl). */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        String sortBy,
        String direction) {

    public static <T> PageResponse<T> from(Page<T> page, String sortBy, String direction) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                sortBy,
                direction);
    }
}
