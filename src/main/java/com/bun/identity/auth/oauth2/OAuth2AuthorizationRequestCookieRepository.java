package com.bun.identity.auth.oauth2;

import java.util.Base64;
import java.util.Optional;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2AuthorizationRequestCookieRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    private static final String DEVICE_ID_COOKIE_NAME = "oauth2_device_id";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request, AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .map(this::deserialize)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response, AUTHORIZATION_REQUEST_COOKIE_NAME);
            deleteCookie(request, response, DEVICE_ID_COOKIE_NAME);
            return;
        }

        addCookie(request, response, AUTHORIZATION_REQUEST_COOKIE_NAME, serialize(authorizationRequest),
                COOKIE_EXPIRE_SECONDS);

        resolveDeviceId(request).ifPresent(deviceId -> addCookie(
                request,
                response,
                DEVICE_ID_COOKIE_NAME,
                deviceId,
                COOKIE_EXPIRE_SECONDS));
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        deleteCookie(request, response, AUTHORIZATION_REQUEST_COOKIE_NAME);
        return authorizationRequest;
    }

    public Optional<String> loadDeviceId(HttpServletRequest request) {
        return getCookie(request, DEVICE_ID_COOKIE_NAME)
                .map(Cookie::getValue)
                .filter(StringUtils::hasText);
    }

    public void clearRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, AUTHORIZATION_REQUEST_COOKIE_NAME);
        deleteCookie(request, response, DEVICE_ID_COOKIE_NAME);
    }

    private Optional<String> resolveDeviceId(HttpServletRequest request) {
        String snakeCase = request.getParameter("device_id");
        if (StringUtils.hasText(snakeCase)) {
            return Optional.of(snakeCase);
        }

        String camelCase = request.getParameter("deviceId");
        return StringUtils.hasText(camelCase) ? Optional.of(camelCase) : Optional.empty();
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        byte[] serialized = SerializationUtils.serialize(authorizationRequest);
        return Base64.getUrlEncoder().encodeToString(serialized);
    }

    private OAuth2AuthorizationRequest deserialize(String cookieValue) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cookieValue);
            Object deserialized = SerializationUtils.deserialize(bytes);
            return deserialized instanceof OAuth2AuthorizationRequest authorizationRequest
                    ? authorizationRequest
                    : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return Optional.of(cookie);
            }
        }

        return Optional.empty();
    }

    private void addCookie(HttpServletRequest request,
            HttpServletResponse response,
            String name,
            String value,
            int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
