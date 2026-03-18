package com.tfm.tennis_platform.infrastructure.controller.dto;

public record RegisterRequest(
        String email,
        String password,
        String name
) {
}
