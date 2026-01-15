// src/main/java/com/docprocessor/system/exception/GlobalExceptionHandler.java
package com.docprocessor.system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND, req.getRequestURI(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        return buildResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED, req.getRequestURI(), null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return buildResponse(ex.getMessage(), HttpStatus.FORBIDDEN, req.getRequestURI(), null);
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ErrorResponse> handleFileProcessing(FileProcessingException ex, HttpServletRequest req) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, req.getRequestURI(), null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        return buildResponse(ex.getMessage(), HttpStatus.CONFLICT, req.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing
                ));

        return buildResponse("Validation failed", HttpStatus.BAD_REQUEST, req.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        return buildResponse(ex.getMessage() != null ? ex.getMessage() : "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR, req.getRequestURI(), null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(String message, HttpStatus status, String path, Map<String, String> fieldErrors) {
        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }

    public static class ErrorResponse {
        private final String timestamp;
        private final int status;
        private final String error;
        private final String message;
        private final String path;
        private final Map<String, String> fieldErrors;

        public ErrorResponse(String timestamp, int status, String error, String message, String path, Map<String, String> fieldErrors) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
            this.fieldErrors = fieldErrors;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public int getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public String getPath() {
            return path;
        }

        public Map<String, String> getFieldErrors() {
            return fieldErrors;
        }
    }
}