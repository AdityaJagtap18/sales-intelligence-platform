package com.nexora.platform.users;

public record LoginResponse(boolean success, String message, UserDto user) {
}
