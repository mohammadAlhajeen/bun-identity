package com.bun.identity.ratelimit;

import java.time.Duration;
import java.time.Instant;

public interface RateLimitCache {

    RateLimitWindow increment(String key, Duration window);

    record RateLimitWindow(Instant expiresAt, int count) {
    }
}
