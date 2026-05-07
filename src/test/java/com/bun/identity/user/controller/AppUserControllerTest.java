package com.bun.identity.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import com.bun.identity.auth.dto.TokenResponse;
import com.bun.identity.auth.service.TokenIssueService;
import com.bun.identity.user.AppUser;
import com.bun.identity.user.dto.AppUserInfoDto;
import com.bun.identity.user.dto.AppUserRegisterReq;
import com.bun.identity.user.dto.ChangePasswordReq;
import com.bun.identity.user.dto.UpdateAppUserDto;
import com.bun.identity.user.service.AppUserService;
import com.bun.identity.user.service.UserIdentityService;
import com.bun.identity.user.service.UserOnboardingService;

@ExtendWith(MockitoExtension.class)
class AppUserControllerTest {

    @Mock
    private AppUserService appUserService;
    @Mock
    private UserIdentityService userIdentityService;
    @Mock
    private UserOnboardingService userOnboardingService;
    @Mock
    private TokenIssueService tokenIssueService;

    @InjectMocks
    private AppUserController controller;

    @Test
    void registerReturnsCreatedToken() {
        String deviceId = UUID.randomUUID().toString();
        AppUserRegisterReq request = new AppUserRegisterReq(
                "a@example.com",
                "Password1",
                "Alice",
                "+970599123456",
                deviceId);
        AppUser user = AppUser.builder().id(UUID.randomUUID()).username("a@example.com").build();
        TokenResponse expected = new TokenResponse("access", "refresh", 900L);

        when(userOnboardingService.registerLocalUser(any(), any())).thenReturn(user);
        when(tokenIssueService.issue(deviceId, user)).thenReturn(expected);

        ResponseEntity<TokenResponse> response = controller.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void changePasswordReturnsServiceMessage() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user").build();
        ChangePasswordReq req = new ChangePasswordReq("OldPassword1", "NewPassword1");

        Map<String, String> expected = Map.of("message", "Password changed successfully");
        when(userIdentityService.extractUserIdFromJwt(jwt)).thenReturn(userId);

        Map<String, String> actual = controller.changePassword(req, jwt);

        assertEquals(expected, actual);
    }

    @Test
    void getProfileReturnsOk() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user").build();
        AppUserInfoDto profile = new AppUserInfoDto("Alice", null, "a@example.com");

        when(userIdentityService.extractUserIdFromJwt(jwt)).thenReturn(userId);
        when(appUserService.getProfile(userId)).thenReturn(profile);

        ResponseEntity<?> response = controller.getProfile(jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(profile, response.getBody());
    }

    @Test
    void updateProfileReturnsOk() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user").build();
        UpdateAppUserDto req = new UpdateAppUserDto("Alice", "+970599123456");
        AppUserInfoDto updated = new AppUserInfoDto("Alice", "+970599123456", null);

        when(userIdentityService.extractUserIdFromJwt(jwt)).thenReturn(userId);
        when(appUserService.updateProfile(any(), any())).thenReturn(updated);

        ResponseEntity<?> response = controller.updateProfile(req, jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updated, response.getBody());
    }
}
