package com.bun.identity.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<Violation> violations) {

    public ApiErrorResponse {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public record Violation(String field, String message) {
    }
}
