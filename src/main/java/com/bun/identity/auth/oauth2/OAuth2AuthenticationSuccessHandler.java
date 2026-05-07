package com.bun.identity.auth.oauth2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.UriUtils;

import com.bun.identity.auth.dto.TokenResponse;
import com.bun.identity.auth.service.TokenIssueService;
import com.bun.identity.exception.IdentityException;
import com.bun.identity.user.service.UserOnboardingService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String FRONTEND_CALLBACK_PATH = "/login/google/callback";

    private final OAuth2UserProfileExtractorRegistry profileExtractorRegistry;
    private final UserOnboardingService userOnboardingService;
    private final TokenIssueService tokenIssueService;
    private final OAuth2AuthorizationRequestCookieRepository authorizationRequestRepository;

    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken)) {
                throw IdentityException.internal("Unexpected OAuth2 authentication type", null);
            }

            String deviceId = authorizationRequestRepository.loadDeviceId(request)
                    .orElseGet(() -> UUID.randomUUID().toString());

            ExternalIdentityProfile profile = profileExtractorRegistry.extract(
                    oauth2AuthenticationToken.getAuthorizedClientRegistrationId(),
                    oauth2AuthenticationToken.getPrincipal());

            var appUser = userOnboardingService.resolveOrCreateFromExternalIdentity(profile, deviceId);
            TokenResponse tokenResponse = tokenIssueService.issue(deviceId, appUser);

            authorizationRequestRepository.clearRequestCookies(request, response);

            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.setHeader(HttpHeaders.PRAGMA, "no-cache");
            response.sendRedirect(buildFrontendCallbackUri(tokenResponse, deviceId));
        } catch (Exception ex) {
            authorizationRequestRepository.clearRequestCookies(request, response);
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }

    private String buildFrontendCallbackUri(TokenResponse tokenResponse, String deviceId) {
        String fragment = "accessToken=" + encode(tokenResponse.accessToken())
                + "&refreshToken=" + encode(tokenResponse.refreshToken())
                + "&expiresInSeconds=" + tokenResponse.expiresInSeconds()
                + "&deviceId=" + encode(deviceId);
        return FRONTEND_CALLBACK_PATH + "#" + fragment;
    }

    private String encode(String value) {
        return UriUtils.encode(value, StandardCharsets.UTF_8);
    }
}
