package com.example.campusresources.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request body for POST /api/resources and PUT /api/resources/{id}. */
public record ResourceRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @Size(max = 150, message = "author must be at most 150 characters")
        String author,

        @NotNull(message = "categoryId is required")
        Long categoryId,

        @NotBlank(message = "resourceType is required")
        @Pattern(regexp = "BOOK|TUTORIAL|STUDY_GUIDE|REFERENCE|ARTICLE",
                message = "resourceType must be one of BOOK, TUTORIAL, STUDY_GUIDE, REFERENCE, ARTICLE")
        String resourceType,

        @DecimalMin(value = "0.0", message = "rating must be between 0 and 5")
        @DecimalMax(value = "5.0", message = "rating must be between 0 and 5")
        Double rating) {
}
