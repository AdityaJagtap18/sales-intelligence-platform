package com.nexora.platform.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Size(min = 8, max = 256) String password,
        @NotBlank @Pattern(regexp = "ADMIN|ANALYST|VIEWER") String role) {
}
