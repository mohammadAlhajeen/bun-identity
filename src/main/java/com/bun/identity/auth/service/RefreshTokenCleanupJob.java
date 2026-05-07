package com.bun.identity.auth.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bun.identity.auth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled cleanup for expired and old revoked refresh-token records.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private static final Duration RETENTION_PERIOD = Duration.ofDays(60);

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    @Transactional
    public void cleanupExpiredRefreshTokens() {

        Instant cutoff = Instant.now().minus(RETENTION_PERIOD);

        int deleted = refreshTokenRepository
                .deleteExpiredAndRevokedBefore(cutoff);

        if (deleted > 0) {
            log.info("[REFRESH TOKEN CLEANUP] Deleted {} old refresh tokens (cutoff={})",
                    deleted, cutoff);
        }
    }
}
