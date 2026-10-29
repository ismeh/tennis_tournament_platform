package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record ParticipantPointsUpdateRequest(
        UUID participantId,
        Integer points,
        Integer seed
) {
}
