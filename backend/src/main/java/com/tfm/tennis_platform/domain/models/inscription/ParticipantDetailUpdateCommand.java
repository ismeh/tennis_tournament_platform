package com.tfm.tennis_platform.domain.models.inscription;

import java.util.UUID;

public record ParticipantDetailUpdateCommand(
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
