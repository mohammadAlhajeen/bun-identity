package com.bun.identity.auth;

import java.util.UUID;

import org.springframework.util.StringUtils;

import com.bun.identity.exception.IdentityException;

public final class DeviceIds {

    public static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-7][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";
    public static final String VALID_UUID_MESSAGE = "Device ID must be a valid UUID";

    private DeviceIds() {
    }

    public static String require(String rawDeviceId) {
        if (!StringUtils.hasText(rawDeviceId)) {
            throw IdentityException.validation("Device ID is required for guest access");
        }

        try {
            return UUID.fromString(rawDeviceId.trim()).toString();
        } catch (IllegalArgumentException ex) {
            throw IdentityException.validation(VALID_UUID_MESSAGE);
        }
    }
}
