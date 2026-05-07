package com.bun.identity.user.service;

import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bun.identity.exception.AuthErrors;
import com.bun.identity.user.AppUser;
import com.bun.identity.user.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves the authenticated AppUser UUID directly from JWT claims.
 *
 * AppUser.id is the API identity now, so there is no secondary identity
 * mapping table.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class UserIdentityService {

    public static final String USER_ID_CLAIM = "uid";

    private static final String PFX_IDENTITY = "[IDENTITY] ";

    private final AppUserRepository appUserRepository;

    public UUID extractUserIdFromJwt(Jwt jwt) {
        if (jwt == null) {
            throw AuthErrors.unauthorized("Missing authentication token");
        }

        String userId = jwt.getClaimAsString(USER_ID_CLAIM);
        if (!StringUtils.hasText(userId)) {
            throw AuthErrors.unauthorized("Missing uid claim");
        }

        try {
            return UUID.fromString(userId.trim());
        } catch (IllegalArgumentException ex) {
            throw AuthErrors.unauthorized("Invalid uid format");
        }
    }

    @Transactional(readOnly = true)
    public AppUser getUserById(@NonNull UUID userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn(PFX_IDENTITY + "User not found for id: {}", userId);
                    return AuthErrors.unauthorized("User not found");
                });
    }

    @Transactional(readOnly = true)
    public AppUser getUserFromJwt(Jwt jwt) {
        return getUserById(extractUserIdFromJwt(jwt));
    }

    @Transactional(readOnly = true)
    public boolean isUserActive(@NonNull UUID userId) {
        return appUserRepository.findById(userId)
                .map(user -> user.isEnabled()
                        && user.isAccountNonLocked()
                        && user.isAccountNonExpired()
                        && user.isCredentialsNonExpired())
                .orElse(false);
    }

}
