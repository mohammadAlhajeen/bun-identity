package com.bun.identity.auth.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bun.identity.auth.RefreshToken;
import com.bun.identity.auth.crypto.RefreshTokenGenerator;
import com.bun.identity.auth.crypto.TokenHashing;
import com.bun.identity.auth.dto.IssuedAccessToken;
import com.bun.identity.auth.dto.TokenResponse;
import com.bun.identity.auth.repository.RefreshTokenRepository;
import com.bun.identity.user.AppUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenIssueService {

        private static final String PFX_LOGIN = "[LOGIN] ";
        private static final String PFX_AUTH = "[AUTH] ";

        private final RefreshTokenRepository refreshRepo;
        private final AccessTokenIssuer accessTokenIssuer;

        private final Duration refreshTtl = Duration.ofDays(14);

        @Transactional
        public TokenResponse issue(String deviceId, AppUser user) {
                Instant now = Instant.now();
                log.info(PFX_LOGIN + "User authenticated successfully: userId={}, username={}", user.getId(),
                                user.getUsername());

                String refreshToken = RefreshTokenGenerator.newOpaqueToken();
                String tokenHash = TokenHashing.sha256Hex(refreshToken);
                RefreshToken rt = new RefreshToken();
                rt.setAppUser(user);
                rt.setTokenHash(tokenHash);
                rt.setDeviceId(deviceId);
                rt.setCreatedAt(now);
                rt.setLastUsedAt(now);
                rt.setExpiresAt(now.plus(refreshTtl));

                refreshRepo.save(rt);
                log.debug(PFX_AUTH + "Refresh token saved for userId: {}, deviceId: {}, expiresAt: {}",
                                user.getId(), deviceId, rt.getExpiresAt());

                IssuedAccessToken access = accessTokenIssuer.issue(user, deviceId);
                log.debug(PFX_AUTH + "Access token issued for userId: {}, expiresIn: {}s", user.getId(),
                                access.expiresInSeconds());

                user.setLastLoginAt(now);
                user.setFailedLoginAttempts(0);
                log.debug(PFX_AUTH + "Updated user login metadata: userId={}, lastLoginAt={}", user.getId(), now);

                log.info(PFX_LOGIN + "Login successful for user: userId={}, deviceId={}",
                                user.getId(), deviceId);

                return new TokenResponse(access.token(), refreshToken, access.expiresInSeconds());
        }
}
