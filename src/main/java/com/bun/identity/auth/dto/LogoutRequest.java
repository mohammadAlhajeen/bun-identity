package com.bun.identity.auth.dto;

import com.bun.identity.auth.DeviceIds;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request for user logout
 */
public record LogoutRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken,

        @Pattern(regexp = DeviceIds.UUID_PATTERN, message = DeviceIds.VALID_UUID_MESSAGE) String deviceId) {
}
