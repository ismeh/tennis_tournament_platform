package com.puntomatch.infrastructure.controller.dto;

public record LoginResponse(String accessToken, String refreshToken) {
}
