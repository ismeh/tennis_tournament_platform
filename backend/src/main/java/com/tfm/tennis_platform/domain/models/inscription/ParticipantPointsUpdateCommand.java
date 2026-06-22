package com.tfm.tennis_platform.domain.models.inscription;

import java.util.UUID;

public record ParticipantPointsUpdateCommand(
        UUID participantId,
        Integer points,
        Integer seed
) {
}
