package com.tfm.tennis_platform.infrastructure.controller.dto;

public record RegisterResponse(boolean emailVerificationRequired, String message) {
}
