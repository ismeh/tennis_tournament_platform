package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * DTO estándar para respuestas de error.
 * Incluye código de error, mensaje, timestamp y detalles opcionales.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String code,
    String message,
    Integer status,
    LocalDateTime timestamp,
    String path,
    Object details
) {
    /**
     * Constructor alternativo sin detalles.
     */
    public ErrorResponse(String code, String message, Integer status, LocalDateTime timestamp, String path) {
        this(code, message, status, timestamp, path, null);
    }
}
