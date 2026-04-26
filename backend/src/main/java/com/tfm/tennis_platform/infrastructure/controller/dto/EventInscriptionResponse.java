package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventInscriptionResponse(
        UUID id,
        UUID eventId,
        UUID participantId,
        String status,
        String paymentStatus,
        LocalDateTime registeredAt
) {
}
