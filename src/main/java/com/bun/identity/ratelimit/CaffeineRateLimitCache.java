package com.bun.identity.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.github.benmanes.caffeine.cache.Cache;

public class CaffeineRateLimitCache implements RateLimitCache {

    private final Cache<String, Window> windows;
    private final Clock clock;

    public CaffeineRateLimitCache(Cache<String, Window> windows) {
        this(windows, Clock.systemUTC());
    }

    CaffeineRateLimitCache(Cache<String, Window> windows, Clock clock) {
        this.windows = windows;
        this.clock = clock;
    }

    @Override
    public RateLimitWindow increment(String key, Duration window) {
        Instant now = clock.instant();
        Window current = windows.asMap().compute(key, (ignored, existing) -> {
            if (existing == null || !existing.expiresAt().isAfter(now)) {
                return new Window(now.plus(window), 1, safeNanos(window));
            }
            return new Window(
                    existing.expiresAt(),
                    existing.count() + 1,
                    safeNanos(Duration.between(now, existing.expiresAt())));
        });
        return new RateLimitWindow(current.expiresAt(), current.count());
    }

    private long safeNanos(Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            return 1L;
        }
        try {
            return Math.max(1L, duration.toNanos());
        } catch (ArithmeticException ex) {
            return Long.MAX_VALUE;
        }
    }

    public record Window(Instant expiresAt, int count, long nanosUntilExpiry) {
    }
}
