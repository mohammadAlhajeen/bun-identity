package com.bun.identity.auth.oauth2;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import com.bun.identity.auth.dto.TokenResponse;
import com.bun.identity.auth.service.TokenIssueService;
import com.bun.identity.user.AppUser;
import com.bun.identity.user.IdentityProvider;
import com.bun.identity.user.service.UserOnboardingService;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

        @Mock
        private OAuth2UserProfileExtractorRegistry profileExtractorRegistry;

        @Mock
        private UserOnboardingService userOnboardingService;

        @Mock
        private TokenIssueService tokenIssueService;

        @Mock
        private OAuth2AuthorizationRequestCookieRepository authorizationRequestRepository;

        @Mock
        private org.springframework.web.servlet.HandlerExceptionResolver handlerExceptionResolver;

        @InjectMocks
        private OAuth2AuthenticationSuccessHandler handler;

        @Test
        void onSuccessRedirectsWithTokenFragmentAndClearsCookies() throws Exception {
                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();
                String deviceId = "c43dc4f0-1ebf-4189-b22e-245ccd0f0ec4";

                var oauthUser = new DefaultOAuth2User(
                                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                                Map.of("email", "a@example.com", "name", "Alice", "sub", "google-sub", "email_verified",
                                                true),
                                "email");
                OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(oauthUser,
                                oauthUser.getAuthorities(), "google");
                ExternalIdentityProfile profile = new ExternalIdentityProfile(IdentityProvider.GOOGLE, "google-sub",
                                "a@example.com", "Alice", true);

                when(authorizationRequestRepository.loadDeviceId(request)).thenReturn(Optional.of(deviceId));
                when(profileExtractorRegistry.extract("google", oauthUser)).thenReturn(profile);
                when(userOnboardingService.resolveOrCreateFromExternalIdentity(eq(profile), eq(deviceId)))
                                .thenReturn(AppUser.builder().build());
                when(tokenIssueService.issue(eq(deviceId), org.mockito.ArgumentMatchers.any(AppUser.class)))
                                .thenReturn(new TokenResponse("access-token", "refresh-token", 900L));

                handler.onAuthenticationSuccess(request, response, authentication);

                verify(authorizationRequestRepository).clearRequestCookies(request, response);
                String redirect = response.getRedirectedUrl();
                assertTrue(redirect.startsWith("/login/google/callback#"));
                assertTrue(redirect.contains("accessToken=access-token"));
                assertTrue(redirect.contains("refreshToken=refresh-token"));
                assertTrue(redirect.contains("deviceId=" + deviceId));
        }

        @Test
        void onUnexpectedAuthenticationTypeResolvesException() throws java.io.IOException, ServletException {
                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();
                var invalidAuth = new TestingAuthenticationToken("principal", "credentials");

                handler.onAuthenticationSuccess(request, response, invalidAuth);

                verify(authorizationRequestRepository).clearRequestCookies(request, response);
                verify(handlerExceptionResolver).resolveException(eq(request), eq(response), eq(null),
                                org.mockito.ArgumentMatchers.any(Exception.class));
        }
}
