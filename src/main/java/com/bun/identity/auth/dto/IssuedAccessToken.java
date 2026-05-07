package com.bun.identity.auth.dto;

/**
 * DTO for issued access token
 */
public record IssuedAccessToken(
        String token,
        long expiresInSeconds) {
}
