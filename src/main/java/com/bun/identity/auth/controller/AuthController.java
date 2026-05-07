package com.bun.identity.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bun.identity.auth.dto.GuestTokenRequest;
import com.bun.identity.auth.dto.LoginRequest;
import com.bun.identity.auth.dto.LogoutRequest;
import com.bun.identity.auth.dto.RefreshRequest;
import com.bun.identity.auth.dto.TokenResponse;
import com.bun.identity.auth.service.AuthLoginService;
import com.bun.identity.auth.service.LogoutService;
import com.bun.identity.auth.service.RefreshTokenService;
import com.bun.identity.user.service.UserIdentityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller for authentication endpoints
 * Endpoints: /auth/login, /auth/refresh, /auth/logout
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthLoginService authLoginService;
    private final RefreshTokenService refreshTokenService;
    private final LogoutService logoutService;
    private final UserIdentityService userIdentityService;

    /**
     * Login endpoint
     * POST /auth/login
     *
     * @param request login request with username, password, deviceId
     * @return token response with access and refresh tokens
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authLoginService.login(request));
    }

    @PostMapping("/guest")
    public ResponseEntity<TokenResponse> issueGuestToken(@Valid @RequestBody GuestTokenRequest request) {
        return ResponseEntity.ok(authLoginService.issueGuestToken(request.deviceId()));
    }

    /**
     * Refresh endpoint
     * POST /auth/refresh
     *
     * @param request refresh request with refresh token and device ID
     * @return new token response
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(refreshTokenService.refresh(request));
    }

    /**
     * Logout from current device
     * POST /auth/logout
     *
     * @param request logout request with refresh token and device ID
     * @return 204 No Content on success
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        logoutService.logoutDevice(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Logout from all devices
     * POST /auth/logout/all
     * Requires authentication (userId extracted from JWT)
     *
     * @param userId the authenticated user ID (from JWT)
     * @return 204 No Content on success
     */
    @PostMapping("/logout/all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        logoutService.logoutAllDevices(userIdentityService.extractUserIdFromJwt(jwt));
        return ResponseEntity.noContent().build();
    }
}
