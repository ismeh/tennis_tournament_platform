package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MatchResponse(
    UUID id,
    UUID firstInscriptionId,
    UUID secondInscriptionId,
    UUID winnerId,
    Integer roundNumber,
    LocalDateTime scheduledAt,
    String scheduleTimeType,
    UUID courtId,
    String court,
    String result
) {}
