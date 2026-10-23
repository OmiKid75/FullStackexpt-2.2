package com.example.campusresources.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Consistent error body returned for every failure. Never contains stack traces. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details) {
}
