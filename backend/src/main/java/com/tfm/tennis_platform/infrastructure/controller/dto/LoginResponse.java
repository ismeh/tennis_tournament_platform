package com.tfm.tennis_platform.infrastructure.controller.dto;

public record LoginResponse(String token, String refreshToken) {
}
