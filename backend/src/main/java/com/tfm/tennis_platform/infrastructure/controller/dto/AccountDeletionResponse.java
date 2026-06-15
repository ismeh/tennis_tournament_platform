package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountDeletionResponse(
        String message,
        LocalDateTime processedAt
) {
}
