package com.tfm.tennis_platform.domain.models.inscription;

import java.util.UUID;

public record ParticipantDetailUpdateCommand(
        UUID participantId,
        String clubName,
        String entryStatus
) {
}
