package com.bun.identity.auth.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.bun.identity.auth.dto.IssuedAccessToken;
import com.bun.identity.auth.jwt.JwtTokenIssuer;
import com.bun.identity.user.AppUser;

import lombok.RequiredArgsConstructor;

/**
 * Service for issuing JWT access tokens
 */
@Service
@RequiredArgsConstructor
public class AccessTokenIssuer {

    private final JwtTokenIssuer jwtTokenIssuer;

    /**
     * Issue a new JWT access token (backward compatibility)
     *
     * @param user the authenticated user
     * @return issued access token with expiration
     */
    public IssuedAccessToken issue(AppUser user, String deviceId) {
        String token = jwtTokenIssuer.issue(user, deviceId);
        return new IssuedAccessToken(token, jwtTokenIssuer.accessTokenTtlSeconds());
    }

    public Duration getAccessTtl() {
        return Duration.ofSeconds(jwtTokenIssuer.accessTokenTtlSeconds());
    }
}
