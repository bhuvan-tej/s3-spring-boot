package com.bhuvan.s3.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Centralised exception handler for all REST controllers.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(S3Exception.FileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(S3Exception.FileNotFoundException ex) {
        log.warn("File not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(S3Exception.FileUploadException.class)
    public ResponseEntity<Map<String, Object>> handleUploadError(S3Exception.FileUploadException ex) {
        log.error("Upload failed: {}", ex.getMessage(), ex.getCause());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(S3Exception.InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(S3Exception.InvalidRequestException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Catch-all for any unhandled exception.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
    }

    // Helper
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }

}