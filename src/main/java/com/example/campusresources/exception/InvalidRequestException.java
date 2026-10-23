package com.example.campusresources.exception;

/** Thrown for bad query parameters such as an invalid page size or sort field (HTTP 400). */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
