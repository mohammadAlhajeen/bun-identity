package com.bun.identity.auth.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bun.identity.auth.RefreshToken;
import com.bun.identity.auth.crypto.TokenHashing;
import com.bun.identity.auth.dto.LogoutRequest;
import com.bun.identity.exception.AuthErrors;
import com.bun.identity.user.AppUser;
import com.bun.identity.user.service.UserIdentityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling user logout and session revocation
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class LogoutService {

    private final RefreshTokenService refreshService;
    private final UserIdentityService userIdentityService;
    private static final String PFX_DEVICE = "[LOGOUT DEVICE] ";
    private static final String PFX_ALL = "[LOGOUT ALL] ";
    private static final String PFX_SPECIFIC = "[LOGOUT SPECIFIC] ";

    /**
     * Logout from a specific device (revoke its refresh token)
     *
     * @param request logout request with token and device ID
     */
    @Transactional
    public void logoutDevice(LogoutRequest request) {
        log.info(PFX_DEVICE + "Device logout attempt - deviceId: {}", request.deviceId());
        Instant now = Instant.now();
        String tokenHash = TokenHashing.sha256Hex(request.refreshToken());

        RefreshToken token = refreshService.findByTokenHash(tokenHash);

        log.debug(PFX_DEVICE + "Refresh token found for logout - userId: {}, deviceId: {}",
                token.getAppUser().getId(), token.getDeviceId());

        // Device binding check
        if (request.deviceId() != null && token.getDeviceId() != null) {
            if (!token.getDeviceId().equals(request.deviceId())) {
                log.warn(PFX_DEVICE + "Device mismatch during logout - userId: {}, expected: {}, got: {}",
                        token.getAppUser().getId(), token.getDeviceId(), request.deviceId());
                throw AuthErrors.unauthorized("Device mismatch");
            }
        }

        // Revoke the token
        if (!token.isRevoked()) {
            token.setRevokedAt(now);
            token.setRevokedReason(RefreshToken.RevocationReason.LOGOUT);
            log.info(PFX_DEVICE + "Device logout successful - userId: {}, deviceId: {}",
                    token.getAppUser().getId(), token.getDeviceId());
        } else {
            log.debug(PFX_DEVICE + "Token already revoked - userId: {}, deviceId: {}",
                    token.getAppUser().getId(), token.getDeviceId());
        }
    }

    /**
     * Logout from all devices (revoke all refresh tokens for user)
     *
     * @param userId the user ID
     */
    @Transactional
    public void logoutAllDevices(UUID userId) {
        log.info(PFX_ALL + "Logout all devices requested - userId: {}", userId);
        AppUser user = userIdentityService.getUserById(userId);

        refreshService.revokeAllDevices(user);
        log.info(PFX_ALL + "All devices logged out successfully - userId: {}", userId);
    }

    /**
     * Revoke all tokens for a specific device
     *
     * @param userId   the user ID
     * @param deviceId the device identifier
     */
    @Transactional
    public void logoutSpecificDevice(UUID userId, String deviceId) {
        log.info(PFX_SPECIFIC + "Logout specific device requested - userId: {}, deviceId: {}", userId,
                deviceId);
        AppUser user = userIdentityService.getUserById(userId);
        refreshService.revokeAllForDevice(user, deviceId);
        log.info(PFX_SPECIFIC + "Specific device logged out successfully - userId: {}, deviceId: {}", userId,
                deviceId);
    }
}
