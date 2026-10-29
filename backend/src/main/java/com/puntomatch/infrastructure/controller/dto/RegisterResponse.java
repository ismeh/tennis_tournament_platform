package com.puntomatch.infrastructure.controller.dto;

public record RegisterResponse(boolean emailVerificationRequired, String message) {
}
