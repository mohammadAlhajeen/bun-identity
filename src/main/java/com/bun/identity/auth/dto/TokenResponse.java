package com.bun.identity.auth.dto;

/**
 * Token response containing access and refresh tokens
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds) {
}
