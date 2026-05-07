package com.bun.identity.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Caffeine;

class RateLimiterTest {

    @Test
    void blocksRequestsAfterLimitUntilWindowExpires() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-06T00:00:00Z"), ZoneOffset.UTC);
        RateLimitCache cache = new CaffeineRateLimitCache(
                Caffeine.newBuilder().build(),
                clock);
        RateLimiter limiter = new RateLimiter(cache, clock);

        assertTrue(limiter.tryAcquire("login:127.0.0.1", 2, Duration.ofMinutes(1)).allowed());
        assertTrue(limiter.tryAcquire("login:127.0.0.1", 2, Duration.ofMinutes(1)).allowed());

        RateLimiter.RateLimitResult blocked = limiter.tryAcquire(
                "login:127.0.0.1",
                2,
                Duration.ofMinutes(1));

        assertFalse(blocked.allowed());
        assertEquals(0, blocked.remaining());
        assertEquals(60, blocked.retryAfterSeconds());
    }
}
