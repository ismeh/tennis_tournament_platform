package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.UserRole;

public record RegisterRequest(
        String email,
        String password,
        String name,
        UserRole role
) {
}
