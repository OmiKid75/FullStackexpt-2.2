package com.example.campusresources.exception;

/** Thrown when a resource or category does not exist (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
