package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MatchResponse(
    UUID id,
    UUID firstInscriptionId,
    UUID secondInscriptionId,
    UUID winnerId,
    Integer roundNumber,
    Integer bracketPosition,
    LocalDateTime scheduledAt,
    String scheduleTimeType,
    UUID courtId,
    String court,
    String result,
    Boolean professionalMatch,
    Integer firstWinPoints,
    Integer secondWinPoints
) {}
