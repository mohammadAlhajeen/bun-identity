package com.bun.identity.ratelimit;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bun.identity.config.RateLimitProperties;
import com.bun.identity.config.RateLimitProperties.EndpointPolicy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED";

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<EndpointPolicy> policy = matchingPolicy(request);
        if (policy.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        EndpointPolicy endpointPolicy = policy.get();
        String key = endpointPolicy.getMethod() + ":" + endpointPolicy.getPath() + ":" + clientIp(request);
        RateLimiter.RateLimitResult result = rateLimiter.tryAcquire(
                key,
                endpointPolicy.getLimit(),
                endpointPolicy.getWindow());

        response.setHeader("X-RateLimit-Limit", Integer.toString(endpointPolicy.getLimit()));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(result.remaining()));

        if (result.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(result.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write("""
                        {"timestamp":"%s","status":429,"error":"Too Many Requests","code":"%s","message":"Too many requests","path":"%s","violations":[]}
                        """
                        .formatted(Instant.now(), ERROR_CODE, escapeJson(path(request))).trim());
    }

    private Optional<EndpointPolicy> matchingPolicy(HttpServletRequest request) {
        String method = request.getMethod();
        String path = path(request);
        return properties.enabledPolicies()
                .stream()
                .filter(policy -> policy.getLimit() > 0)
                .filter(policy -> policy.getWindow() != null && !policy.getWindow().isNegative()
                        && !policy.getWindow().isZero())
                .filter(policy -> policy.getMethod().equalsIgnoreCase(method))
                .filter(policy -> policy.getPath().equals(path))
                .findFirst();
    }

    private String clientIp(HttpServletRequest request) {
        String configuredHeader = properties.getClientIpHeader();
        if (configuredHeader != null && !configuredHeader.isBlank()) {
            String headerValue = request.getHeader(configuredHeader);
            if (headerValue != null && !headerValue.isBlank()) {
                return headerValue.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String path(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath == null || servletPath.isBlank() ? request.getRequestURI() : servletPath;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
