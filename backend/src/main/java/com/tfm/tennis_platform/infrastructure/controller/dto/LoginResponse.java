package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.UserRole;

public record LoginResponse(String accessToken, String refreshToken, UserRole role) {
}
