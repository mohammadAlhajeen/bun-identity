package com.bun.identity.user.dto;

import com.bun.identity.auth.DeviceIds;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AppUserRegisterReq(
                @NotBlank(message = "Username is required") @Email(message = "Invalid email format") String username,

                @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$", message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number") @NotBlank(message = "Password is required") String password,

                @NotBlank(message = "Name is required") @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters") String name,

                @Pattern(regexp = "^(\\+?[1-9]\\d{1,14}|0\\d{6,14})$", message = "Invalid phone number format") String phone,

                @NotBlank(message = "Device ID is required") @Pattern(regexp = DeviceIds.UUID_PATTERN, message = DeviceIds.VALID_UUID_MESSAGE) String deviceId) {
}
