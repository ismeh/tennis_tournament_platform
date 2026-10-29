package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlayerMatchCalendarResponse(
        UUID tournamentId,
        String tournamentName,
        UUID eventId,
        String eventName,
        UUID matchId,
        Integer roundNumber,
        LocalDateTime scheduledAt,
        String scheduleTimeType,
        UUID courtId,
        String court,
        UUID firstInscriptionId,
        String firstParticipantName,
        UUID secondInscriptionId,
        String secondParticipantName,
        String result
) {}
