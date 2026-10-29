package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;

public record LegalDocumentResponse(
        String documentType,
        String version,
        String contentSnapshot,
        LocalDateTime createdAt
) {
}
