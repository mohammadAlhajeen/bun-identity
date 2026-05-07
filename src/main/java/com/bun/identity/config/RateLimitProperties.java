package com.bun.identity.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private String clientIpHeader = "X-Forwarded-For";
    private int maxKeys = 10_000;
    private EndpointPolicy authGuest = new EndpointPolicy(true, "POST", "/auth/guest", 20, Duration.ofHours(1));
    private EndpointPolicy authLogin = new EndpointPolicy(true, "POST", "/auth/login", 10, Duration.ofMinutes(10));
    private EndpointPolicy authRefresh = new EndpointPolicy(true, "POST", "/auth/refresh", 60, Duration.ofHours(1));
    private EndpointPolicy userRegister = new EndpointPolicy(true, "POST", "/api/user/register", 5,
            Duration.ofHours(1));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientIpHeader() {
        return clientIpHeader;
    }

    public void setClientIpHeader(String clientIpHeader) {
        this.clientIpHeader = clientIpHeader;
    }

    public int getMaxKeys() {
        return maxKeys;
    }

    public void setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
    }

    public EndpointPolicy getAuthGuest() {
        return authGuest;
    }

    public void setAuthGuest(EndpointPolicy authGuest) {
        this.authGuest = authGuest;
    }

    public EndpointPolicy getAuthLogin() {
        return authLogin;
    }

    public void setAuthLogin(EndpointPolicy authLogin) {
        this.authLogin = authLogin;
    }

    public EndpointPolicy getAuthRefresh() {
        return authRefresh;
    }

    public void setAuthRefresh(EndpointPolicy authRefresh) {
        this.authRefresh = authRefresh;
    }

    public EndpointPolicy getUserRegister() {
        return userRegister;
    }

    public void setUserRegister(EndpointPolicy userRegister) {
        this.userRegister = userRegister;
    }

    public List<EndpointPolicy> enabledPolicies() {
        return List.of(authGuest, authLogin, authRefresh, userRegister)
                .stream()
                .filter(EndpointPolicy::isEnabled)
                .toList();
    }

    public static class EndpointPolicy {
        private boolean enabled;
        private String method;
        private String path;
        private int limit;
        private Duration window;

        public EndpointPolicy() {
        }

        public EndpointPolicy(boolean enabled, String method, String path, int limit, Duration window) {
            this.enabled = enabled;
            this.method = method;
            this.path = path;
            this.limit = limit;
            this.window = window;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}
