package com.bun.identity.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.bun.identity.config.RateLimitProperties;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.servlet.ServletException;

class RateLimitFilterTest {

    @Test
    void returnsTooManyRequestsWhenPolicyIsExceeded() throws ServletException, IOException {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setAuthLogin(new RateLimitProperties.EndpointPolicy(
                true,
                "POST",
                "/auth/login",
                1,
                Duration.ofMinutes(1)));
        RateLimitFilter filter = new RateLimitFilter(properties, rateLimiter());

        filter.doFilter(loginRequest(), new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), secondResponse, new MockFilterChain());

        assertEquals(429, secondResponse.getStatus());
        assertEquals("1", secondResponse.getHeader("X-RateLimit-Limit"));
        assertEquals("0", secondResponse.getHeader("X-RateLimit-Remaining"));
        assertTrue(secondResponse.getContentAsString().contains("\"code\":\"RATE_LIMIT_EXCEEDED\""));
    }

    @Test
    void skipsLimitsWhenDisabled() throws ServletException, IOException {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(false);
        RateLimitFilter filter = new RateLimitFilter(properties, rateLimiter());

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setServletPath("/auth/login");
        request.setRemoteAddr("203.0.113.10");
        return request;
    }

    private RateLimiter rateLimiter() {
        return new RateLimiter(new CaffeineRateLimitCache(Caffeine.newBuilder().build()));
    }
}
