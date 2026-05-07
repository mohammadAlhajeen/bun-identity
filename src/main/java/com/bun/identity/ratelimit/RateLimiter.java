package com.bun.identity.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private final RateLimitCache cache;
    private final Clock clock;

    @Autowired
    public RateLimiter(RateLimitCache cache) {
        this(cache, Clock.systemUTC());
    }

    RateLimiter(RateLimitCache cache, Clock clock) {
        this.cache = cache;
        this.clock = clock;
    }

    public RateLimitResult tryAcquire(String key, int limit, Duration window) {
        RateLimitCache.RateLimitWindow current = cache.increment(key, window);
        int remaining = Math.max(0, limit - current.count());
        long retryAfterSeconds = Math.max(1, Duration.between(Instant.now(clock), current.expiresAt()).toSeconds());
        return new RateLimitResult(current.count() <= limit, remaining, retryAfterSeconds);
    }

    public record RateLimitResult(boolean allowed, int remaining, long retryAfterSeconds) {
    }
}
