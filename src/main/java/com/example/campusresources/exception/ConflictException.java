package com.example.campusresources.exception;

/** Thrown for duplicate names or deleting a category that still has resources (HTTP 409). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
