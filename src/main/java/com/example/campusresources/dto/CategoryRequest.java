package com.example.campusresources.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for POST /api/categories and PUT /api/categories/{id}. */
public record CategoryRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description) {
}
