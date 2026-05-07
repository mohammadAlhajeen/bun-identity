package com.bun.identity.auth.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bun.identity.auth.RefreshToken;
import com.bun.identity.auth.crypto.RefreshTokenGenerator;
import com.bun.identity.auth.crypto.TokenHashing;
import com.bun.identity.auth.dto.IssuedAccessToken;
import com.bun.identity.auth.dto.RefreshRequest;
import com.bun.identity.auth.dto.TokenResponse;
import com.bun.identity.auth.repository.RefreshTokenRepository;
import com.bun.identity.exception.AuthErrors;
import com.bun.identity.exception.RefreshTokenReuseException;
import com.bun.identity.user.AppUser;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of refresh token service with token rotation and reuse
 * detection
 */
@Service
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshRepo;
    private final AccessTokenIssuer accessTokenIssuer;
    private static final String PFX_REFRESH = "[REFRESH] ";
    private static final String PFX_SECURITY = "[SECURITY] ";

    // Refresh token TTL: 14 days
    private final Duration refreshTtl = Duration.ofDays(14);

    public RefreshTokenService(
            RefreshTokenRepository refreshRepo,
            AccessTokenIssuer accessTokenIssuer) {
        this.refreshRepo = refreshRepo;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        log.info(PFX_REFRESH + "Token refresh attempt - deviceId: {}", request != null ? request.deviceId() : "null");

        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            log.warn(PFX_REFRESH + "Refresh token missing in request");
            throw AuthErrors.unauthorized("Missing refresh token");
        }

        Instant now = Instant.now();

        // 1) Hash incoming refresh token
        String incomingHash = TokenHashing.sha256Hex(request.refreshToken());
        log.debug(PFX_REFRESH + "Refresh token hashed for lookup");

        // 2) Load row FOR UPDATE (prevents concurrent refresh)
        RefreshToken old = refreshRepo.findByTokenHashForUpdate(incomingHash)
                .orElseThrow(() -> {
                    log.warn(PFX_REFRESH + "Invalid refresh token hash provided");
                    return AuthErrors.unauthorized("Invalid refresh token");
                });

        log.debug(PFX_REFRESH
                + "Refresh token found - userId: {}, deviceId: {}, expiresAt: {}",
                old.getAppUser().getId(), old.getDeviceId(), old.getExpiresAt());

        // 3) Verify user state directly from AppUser
        var user = old.getAppUser();
        log.debug(PFX_REFRESH + "User identity verified - userId: {}", user.getId());

        // 4) check user is active
        validateUserState(user);
        log.debug(PFX_REFRESH + "User state validated - userId: {}", user.getId());

        // 5) Device binding check (strict mode)
        if (request.deviceId() != null && old.getDeviceId() != null) {
            if (!old.getDeviceId().equals(request.deviceId())) {
                log.warn(PFX_REFRESH + "Device mismatch detected - userId: {}, expected: {}, got: {}",
                        user.getId(), old.getDeviceId(), request.deviceId());
                throw AuthErrors.unauthorized("Device mismatch");
            }
            log.debug(PFX_REFRESH + "Device binding validated - userId: {}, deviceId: {}", user.getId(),
                    request.deviceId());
        }

        // 6) Validate refresh token state
        if (old.isReplaced()) {
            log.warn(PFX_REFRESH + "Attempt to use replaced refresh token - userId: {}", user.getId());
            throw AuthErrors.unauthorized("Refresh token revoked");
        }
        if (old.isExpired()) {
            log.warn(PFX_REFRESH + "Expired refresh token used - userId: {}, expiresAt: {}", user.getId(),
                    old.getExpiresAt());
            throw AuthErrors.unauthorized("Refresh token expired");
        }

        // Reuse detection: 409 Conflict and revoke all user tokens.
        if (old.isRevoked()) {
            // Security incident: old token used after replacement
            log.error(PFX_SECURITY
                    + "SECURITY BREACH: Token reuse detected - userId: {}, deviceId: {}",
                    user.getId(), old.getDeviceId());

            refreshRepo.revokeAllByUser(user, RefreshToken.RevocationReason.SECURITY_BREACH, now);
            log.warn(PFX_SECURITY + "All user tokens revoked due to security breach - userId: {}", user.getId());

            user.lockAccount();
            log.warn(PFX_SECURITY + "User locked due to token reuse - userId: {}", user.getId());

            throw new RefreshTokenReuseException();
        }

        // 8) Token rotation: revoke old + create new
        log.debug(PFX_REFRESH + "Starting token rotation - userId: {}", user.getId());
        String newRefreshToken = RefreshTokenGenerator.newOpaqueToken();
        String newHash = TokenHashing.sha256Hex(newRefreshToken);

        old.setRevokedAt(now);
        old.setReplacedByTokenHash(newHash);
        old.setRevokedReason(RefreshToken.RevocationReason.TOKEN_ROTATION);
        old.markAsUsed();
        log.debug(PFX_REFRESH + "Old refresh token revoked - userId: {}, reason: TOKEN_ROTATION", user.getId());

        RefreshToken fresh = new RefreshToken();
        fresh.setAppUser(old.getAppUser());
        fresh.setTokenHash(newHash);
        fresh.setDeviceId(request.deviceId());
        fresh.setCreatedAt(now);
        fresh.setLastUsedAt(now);
        fresh.setExpiresAt(now.plus(refreshTtl));

        refreshRepo.save(fresh);
        log.debug(PFX_REFRESH + "New refresh token created - userId: {}, deviceId: {}, expiresAt: {}",
                user.getId(), request.deviceId(), fresh.getExpiresAt());

        // old entity is managed and will be updated on flush
        IssuedAccessToken access = accessTokenIssuer.issue(user, request.deviceId());
        log.debug(PFX_REFRESH + "New access token issued - userId: {}, expiresIn: {}s", user.getId(),
                access.expiresInSeconds());

        // 9) Return response
        log.info(PFX_REFRESH + "Token refresh successful - userId: {}, deviceId= {}",
                user.getId(), request.deviceId());
        return new TokenResponse(access.token(), newRefreshToken, access.expiresInSeconds());
    }

    /**
     * Validate user state
     * 
     * @param user
     */
    private void validateUserState(AppUser user) {
        if (!user.isEnabled()) {
            log.warn(PFX_REFRESH + "User is disabled - userId: {}", user.getId());
            throw AuthErrors.unauthorized("User is disabled");
        }
        if (!user.isAccountNonLocked()) {
            log.warn(PFX_REFRESH + "User account is locked - userId: {}", user.getId());

            throw AuthErrors.unauthorized("User account is locked");
        }
        if (!user.isAccountNonExpired()) {
            log.warn(PFX_REFRESH + "User account is expired - userId: {}", user.getId());

            throw AuthErrors.unauthorized("User account is expired");
        }
        if (!user.isCredentialsNonExpired()) {
            log.warn(PFX_REFRESH + "User credentials are expired - userId: {}", user.getId());

            throw AuthErrors.unauthorized("User credentials are expired");
        }

    }

    public void revokeAllDevices(AppUser user) {

        refreshRepo.revokeAllByUser(user, RefreshToken.RevocationReason.LOGOUT, Instant.now());

    }

    public void revokeAllForDevice(AppUser user, String deviceId) {
        refreshRepo.revokeAllByUserAndDeviceId(user, deviceId, RefreshToken.RevocationReason.LOGOUT, Instant.now());
    }

    public RefreshToken findByTokenHash(String tokenHash) {
        return refreshRepo.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn(PFX_REFRESH + "Invalid refresh token provided for logout");
                    return AuthErrors.unauthorized("Invalid refresh token");
                });
    }
}
