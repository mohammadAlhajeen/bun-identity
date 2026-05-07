package com.bun.identity.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bun.identity.ratelimit.CaffeineRateLimitCache;
import com.bun.identity.ratelimit.RateLimitCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

@Configuration
public class CaffeineRateLimitCacheConfig {

    @Bean
    @ConditionalOnMissingBean(RateLimitCache.class)
    public Cache<String, CaffeineRateLimitCache.Window> rateLimitWindowCache(RateLimitProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(Math.max(1, properties.getMaxKeys()))
                .expireAfter(new RateLimitWindowExpiry())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(RateLimitCache.class)
    public RateLimitCache rateLimitCache(
            @Qualifier("rateLimitWindowCache") Cache<String, CaffeineRateLimitCache.Window> windows) {
        return new CaffeineRateLimitCache(windows);
    }

    private static class RateLimitWindowExpiry implements Expiry<String, CaffeineRateLimitCache.Window> {

        @Override
        public long expireAfterCreate(String key, CaffeineRateLimitCache.Window value, long currentTime) {
            return value.nanosUntilExpiry();
        }

        @Override
        public long expireAfterUpdate(
                String key,
                CaffeineRateLimitCache.Window value,
                long currentTime,
                long currentDuration) {
            return value.nanosUntilExpiry();
        }

        @Override
        public long expireAfterRead(
                String key,
                CaffeineRateLimitCache.Window value,
                long currentTime,
                long currentDuration) {
            return currentDuration;
        }
    }
}
