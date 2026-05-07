package com.bun.identity.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import com.bun.identity.auth.dto.GuestTokenRequest;
import com.bun.identity.auth.dto.LoginRequest;
import com.bun.identity.auth.dto.LogoutRequest;
import com.bun.identity.auth.dto.RefreshRequest;
import com.bun.identity.auth.dto.TokenResponse;
import com.bun.identity.auth.service.AuthLoginService;
import com.bun.identity.auth.service.LogoutService;
import com.bun.identity.auth.service.RefreshTokenService;
import com.bun.identity.user.service.UserIdentityService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthLoginService authLoginService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LogoutService logoutService;
    @Mock
    private UserIdentityService userIdentityService;

    @InjectMocks
    private AuthController controller;

    @Test
    void loginReturnsOkAndToken() {
        TokenResponse expected = new TokenResponse("access", "refresh", 900L);
        when(authLoginService.login(any())).thenReturn(expected);

        ResponseEntity<TokenResponse> response = controller.login(
                new LoginRequest("a@example.com", "Password1", UUID.randomUUID().toString()));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void guestReturnsOkAndToken() {
        TokenResponse expected = new TokenResponse("access", "refresh", 900L);
        when(authLoginService.issueGuestToken(any())).thenReturn(expected);

        ResponseEntity<TokenResponse> response = controller.issueGuestToken(
                new GuestTokenRequest(UUID.randomUUID().toString()));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void refreshReturnsOkAndToken() {
        TokenResponse expected = new TokenResponse("access2", "refresh2", 900L);
        when(refreshTokenService.refresh(any())).thenReturn(expected);

        ResponseEntity<TokenResponse> response = controller.refresh(
                new RefreshRequest("opaque-refresh", UUID.randomUUID().toString()));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void logoutReturnsNoContent() {
        ResponseEntity<Void> response = controller.logout(
                new LogoutRequest("opaque-refresh", UUID.randomUUID().toString()));

        verify(logoutService).logoutDevice(any());
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void logoutAllUsesUserIdFromJwtAndReturnsNoContent() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("uid", userId.toString())
                .build();

        when(userIdentityService.extractUserIdFromJwt(jwt)).thenReturn(userId);

        ResponseEntity<Void> response = controller.logoutAll(jwt);

        verify(logoutService).logoutAllDevices(userId);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
