package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConsentHistoryResponse(
        List<ConsentEntry> history
) {
    public record ConsentEntry(
            String documentType,
            String action,
            String documentVersion,
            LocalDateTime createdAt
    ) {
    }
}
