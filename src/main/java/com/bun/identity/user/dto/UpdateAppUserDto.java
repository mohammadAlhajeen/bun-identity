package com.bun.identity.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAppUserDto(
                @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters") String name,
                @Pattern(regexp = "^(\\+?[1-9]\\d{1,14}|0\\d{6,14})$", message = "Invalid phone number format") String phone) {
}
