package com.puntomatch.infrastructure.controller.dto;

public record AuthResponse(
        String username,
        String password
) {
}
