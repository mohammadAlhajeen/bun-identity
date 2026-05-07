package com.bun.identity.exception;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("API error {} at {}", ex.getCode(), path(request), ex);
        } else {
            log.warn("API error {} at {}: {}", ex.getCode(), path(request), ex.getMessage());
        }
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), path(request), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ApiErrorResponse.Violation> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new ApiErrorResponse.Violation(error.getField(), safeMessage(error.getDefaultMessage())))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", path(request),
                violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        List<ApiErrorResponse.Violation> violations = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ApiErrorResponse.Violation(
                        violation.getPropertyPath().toString(),
                        safeMessage(violation.getMessage())))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", path(request),
                violations);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Bad request at {}: {}", path(request), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request", path(request), List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(Exception ex, HttpServletRequest request) {
        log.warn("Authentication failed at {}: {}", path(request), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "Invalid credentials", path(request), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied at {}: {}", path(request), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "Access denied", path(request), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.warn("Data conflict at {}", path(request), ex);
        return build(HttpStatus.CONFLICT, "DATA_CONFLICT", "Request conflicts with existing data", path(request),
                List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", path(request), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", path(request),
                List.of());
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            String path,
            List<ApiErrorResponse.Violation> violations) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                violations);
        return ResponseEntity.status(status).body(body);
    }

    private String path(HttpServletRequest request) {
        return request == null ? "" : request.getRequestURI();
    }

    private String safeMessage(String message) {
        return message == null || message.isBlank() ? "Invalid value" : message;
    }
}
