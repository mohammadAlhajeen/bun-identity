package com.bun.identity.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenReuseException extends ApiException {

    public RefreshTokenReuseException() {
        super(HttpStatus.CONFLICT,
                "AUTH_REFRESH_TOKEN_REUSE",
                "Refresh token reuse detected. All sessions were revoked.");
    }
}
