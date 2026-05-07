package com.bun.identity.user.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bun.identity.auth.dto.TokenResponse;
import com.bun.identity.auth.service.TokenIssueService;
import com.bun.identity.user.AppUser;
import com.bun.identity.user.Role;
import com.bun.identity.user.dto.AppUserRegisterReq;
import com.bun.identity.user.dto.ChangePasswordReq;
import com.bun.identity.user.dto.UpdateAppUserDto;
import com.bun.identity.user.service.AppUserService;
import com.bun.identity.user.service.UserIdentityService;
import com.bun.identity.user.service.UserOnboardingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;
    private final UserIdentityService userIdentityService;
    private final UserOnboardingService userOnboardingService;
    private final TokenIssueService tokenIssueService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody AppUserRegisterReq request) {
        AppUser user = userOnboardingService.registerLocalUser(request, Set.of(Role.ROLE_USER));
        TokenResponse tokenResponse = tokenIssueService.issue(request.deviceId(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenResponse);
    }

    /**
     * Change the current user's password.
     *
     * @param changePassword request containing old/new password and device context
     * @param jwt            authenticated user token
     * @return message indicating success
     */
    @PostMapping("/change-password")
    public Map<String, String> changePassword(@RequestBody @Valid ChangePasswordReq changePassword,
            @AuthenticationPrincipal Jwt jwt) {
        appUserService.changePassword(userIdentityService.extractUserIdFromJwt(jwt), changePassword);
        return Map.of("message", "Password changed successfully");
    }

    /**
     * Get the current user's profile.
     *
     * @param jwt authenticated user token
     * @return user profile data
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(appUserService.getProfile(userIdentityService.extractUserIdFromJwt(jwt)));
    }

    /**
     * Update the current user's profile.
     *
     * @param appUserDto updated profile data
     * @param jwt        authenticated user token
     * @return updated user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateAppUserDto appUserDto,
            @AuthenticationPrincipal Jwt jwt) {
        var updatedUser = appUserService.updateProfile(
                userIdentityService.extractUserIdFromJwt(jwt),
                appUserDto);
        return ResponseEntity.ok(updatedUser);
    }

}
