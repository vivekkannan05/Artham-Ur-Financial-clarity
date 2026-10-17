package com.artha.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global error handler for all REST controllers.
 *
 * @RestControllerAdvice catches exceptions thrown by ANY controller
 * and converts them to structured JSON error responses.
 *
 * WHY?
 *   Without this, Spring returns its default error page (HTML or generic JSON).
 *   We want consistent, API-friendly error responses that the React frontend
 *   can parse and display to the user.
 *
 * ERROR RESPONSE FORMAT (per Design Doc Section 11):
 *   {
 *     "error": "ERROR_CODE",
 *     "message": "Human-readable description",
 *     "timestamp": "2024-01-15T10:30:00Z",
 *     "details": { ... }   // optional field-level errors
 *   }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (e.g., @NotBlank, @Positive fails).
     * Returns HTTP 400 with field-level error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> body = new HashMap<>();
        body.put("error", "VALIDATION_ERROR");
        body.put("message", "Request validation failed");
        body.put("timestamp", Instant.now().toString());
        body.put("details", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handle "not found" errors (thrown as RuntimeException in services).
     * In a production app, you'd create a custom NotFoundException.
     * For teaching clarity, we catch RuntimeException with "not found" message.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex) {
        String message = ex.getMessage();

        if (message != null && message.toLowerCase().contains("not found")) {
            Map<String, Object> body = new HashMap<>();
            body.put("error", "NOT_FOUND");
            body.put("message", message);
            body.put("timestamp", Instant.now().toString());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // Generic server error for other RuntimeExceptions
        Map<String, Object> body = new HashMap<>();
        body.put("error", "INTERNAL_ERROR");
        body.put("message", "An unexpected error occurred");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
