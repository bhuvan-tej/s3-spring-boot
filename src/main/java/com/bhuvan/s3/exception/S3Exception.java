package com.bhuvan.s3.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Domain-specific exceptions for the S3 service layer.
 */
public class S3Exception extends Throwable {

    /**
     * Thrown when an S3 object cannot be found (e.g. wrong key or folder).
     * Maps to HTTP 404 Not Found.
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class FileNotFoundException extends RuntimeException {
        public FileNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when an S3 upload or copy operation fails.
     * Maps to HTTP 500 Internal Server Error.
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class FileUploadException extends RuntimeException {
        public FileUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when caller supplies an invalid folder name or file name.
     * Maps to HTTP 400 Bad Request.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

}