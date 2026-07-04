package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record ParticipantDetailUpdateRequest(
        UUID participantId,
        String clubName,
        String entryStatus
) {
}
