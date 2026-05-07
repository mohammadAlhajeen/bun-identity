package com.bun.identity.exception;

public final class AuthErrors {

    private AuthErrors() {
    }

    public static IdentityException unauthorized(String message) {
        return IdentityException.unauthorized(message);
    }

    public static IdentityException forbidden(String message) {
        return IdentityException.forbidden(message);
    }
}
