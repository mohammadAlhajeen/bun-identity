package com.bun.identity.auth.dto;

import com.bun.identity.auth.DeviceIds;

import jakarta.validation.constraints.Pattern;

public record GuestTokenRequest(
        @Pattern(regexp = DeviceIds.UUID_PATTERN, message = DeviceIds.VALID_UUID_MESSAGE) String deviceId) {
}
