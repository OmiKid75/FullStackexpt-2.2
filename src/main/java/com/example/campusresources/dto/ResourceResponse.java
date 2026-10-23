package com.example.campusresources.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Full resource view (used by GET /api/resources/{id}, create and update). Cached by Ehcache. */
public record ResourceResponse(
        Long id,
        String title,
        String description,
        String author,
        String resourceType,
        Double rating,
        CategorySummary category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) implements Serializable {
}
