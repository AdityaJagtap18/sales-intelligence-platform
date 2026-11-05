package com.nexora.platform.users;

import java.time.Instant;

public record UserDto(Long id, String username, String role, boolean enabled, Instant createdAt, Instant lastLoginAt) {

    public static UserDto from(AppUser u) {
        return new UserDto(u.getId(), u.getUsername(), u.getRole(), u.isEnabled(), u.getCreatedAt(), u.getLastLoginAt());
    }
}
