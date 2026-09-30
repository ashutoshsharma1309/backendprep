package com.example.library.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String message;
    private final Instant timestamp = Instant.now();
    private List<String> details;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public ErrorResponse(String message, List<String> details) {
        this.message = message;
        this.details = details;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public List<String> getDetails() {
        return details;
    }
}
