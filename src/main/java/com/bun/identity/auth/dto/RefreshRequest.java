package com.bun.identity.auth.dto;

import com.bun.identity.auth.DeviceIds;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request for refreshing access token
 */
public record RefreshRequest(
                @NotBlank(message = "Refresh token is required") String refreshToken,

                @Pattern(regexp = DeviceIds.UUID_PATTERN, message = DeviceIds.VALID_UUID_MESSAGE) String deviceId) {
}
