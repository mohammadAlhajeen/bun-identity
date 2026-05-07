package com.bun.identity.exception;

import org.springframework.http.HttpStatus;

public class IdentityException extends ApiException {

    protected IdentityException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    protected IdentityException(HttpStatus status, String code, String message, Throwable cause) {
        super(status, code, message, cause);
    }

    public static IdentityException validation(String message) {
        return new IdentityException(HttpStatus.BAD_REQUEST, "IDENTITY_VALIDATION", message);
    }

    public static IdentityException unauthorized(String message) {
        return new IdentityException(HttpStatus.UNAUTHORIZED, "IDENTITY_UNAUTHORIZED", message);
    }

    public static IdentityException forbidden(String message) {
        return new IdentityException(HttpStatus.FORBIDDEN, "IDENTITY_FORBIDDEN", message);
    }

    public static IdentityException notFound(String message) {
        return new IdentityException(HttpStatus.NOT_FOUND, "IDENTITY_NOT_FOUND", message);
    }

    public static IdentityException conflict(String message) {
        return new IdentityException(HttpStatus.CONFLICT, "IDENTITY_CONFLICT", message);
    }

    public static IdentityException internal(String message, Throwable cause) {
        return new IdentityException(HttpStatus.INTERNAL_SERVER_ERROR, "IDENTITY_INTERNAL", message, cause);
    }
}
