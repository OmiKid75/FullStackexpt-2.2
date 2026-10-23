package com.example.campusresources.dto;

import java.time.LocalDateTime;

/** Small resource view for list pages (no long description - keeps pages light). */
public record ResourceSummaryResponse(
        Long id,
        String title,
        String author,
        String resourceType,
        Double rating,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt) {
}
