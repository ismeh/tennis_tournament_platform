package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MatchResponse(
    UUID id,
    UUID tournamentId,
    UUID categoryId,
    UUID firstInscriptionId,
    UUID secondInscriptionId,
    UUID winnerId,
    Integer roundNumber,
    LocalDateTime scheduledAt,
    String court,
    String result
) {}
