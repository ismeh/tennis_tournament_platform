package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record ParticipantDetailUpdateRequest(
        UUID inscriptionId,
        UUID participantId,
        String clubName,
        String entryStatus,
        String paymentStatus,
        String firstName,
        String lastName,
        String gender,
        UUID eventId
) {
}
