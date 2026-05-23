package com.tfm.tennis_platform.domain.models.inscription;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventInscriptionResult(
        UUID id,
        UUID eventId,
        UUID participantId,
        String status,
        String paymentStatus,
        LocalDateTime registeredAt
) {
}
