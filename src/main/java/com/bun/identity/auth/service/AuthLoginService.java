package com.bun.identity.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bun.identity.auth.dto.LoginRequest;
import com.bun.identity.auth.dto.TokenResponse;
import com.bun.identity.exception.IdentityException;
import com.bun.identity.user.security.SecurityUserDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling user login and authentication
 */
@Service
@Slf4j
public class AuthLoginService {

    private final AuthenticationManager authenticationManager;
    private final TokenIssueService tokenIssueService;
    private final GuestTokenService guestTokenService;

    private static final String PFX_LOGIN = "[LOGIN] ";
    private static final String PFX_AUTH = "[AUTH] ";

    public AuthLoginService(
            AuthenticationManager authenticationManager,
            TokenIssueService tokenIssueService,
            GuestTokenService guestTokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenIssueService = tokenIssueService;
        this.guestTokenService = guestTokenService;
    }

    /**
     * Authenticate user and issue tokens
     *
     * @param request login request with username, password, deviceId
     * @return token response with access and refresh tokens
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {

        log.info(PFX_LOGIN + "Login attempt for username: {}", request.username());

        // 1) Authenticate credentials
        // ensure from spring security context for proper audit logging
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()));

        log.debug(PFX_AUTH + "Authentication successful for username: {}", request.username());

        // 2) Get authenticated user
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof SecurityUserDetails details)) {
            log.error("Unexpected authentication principal type: {}", principal.getClass().getName());
            throw IdentityException.internal("Unexpected authentication principal type", null);
        }

        return tokenIssueService.issue(request.deviceId(), details.user());
    }

    @Transactional
    public TokenResponse issueGuestToken(String deviceId) {
        return guestTokenService.issueGuestToken(deviceId);
    }

}
