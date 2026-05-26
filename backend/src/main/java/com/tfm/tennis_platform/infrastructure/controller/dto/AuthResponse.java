package com.tfm.tennis_platform.infrastructure.controller.dto;

public record AuthResponse(
        String username,
        String password
) {
}
