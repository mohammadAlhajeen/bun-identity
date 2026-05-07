package com.bun.identity.auth.dto;

import com.bun.identity.auth.DeviceIds;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request for user login
 */
public record LoginRequest(
                @NotBlank(message = "Username is required") String username,

                @NotBlank(message = "Password is required") String password,

                @Pattern(regexp = DeviceIds.UUID_PATTERN, message = DeviceIds.VALID_UUID_MESSAGE) String deviceId) {
}
