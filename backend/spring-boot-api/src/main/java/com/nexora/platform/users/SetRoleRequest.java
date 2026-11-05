package com.nexora.platform.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetRoleRequest(@NotBlank @Pattern(regexp = "ADMIN|ANALYST|VIEWER") String role) {
}
