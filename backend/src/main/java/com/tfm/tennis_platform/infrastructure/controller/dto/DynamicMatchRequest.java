package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DynamicMatchRequest(
        UUID drawId,
        UUID firstInscriptionId,
        UUID secondInscriptionId,
        LocalDateTime scheduledAt,
        UUID courtId,
        Integer roundNumber
) {}
