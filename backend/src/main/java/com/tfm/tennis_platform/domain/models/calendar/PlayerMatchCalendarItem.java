package com.tfm.tennis_platform.domain.models.calendar;

import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlayerMatchCalendarItem(
        UUID tournamentId,
        String tournamentName,
        UUID eventId,
        String eventName,
        UUID matchId,
        Integer roundNumber,
        LocalDateTime scheduledAt,
        ScheduleTimeType scheduleTimeType,
        UUID courtId,
        String court,
        UUID firstInscriptionId,
        String firstParticipantName,
        UUID secondInscriptionId,
        String secondParticipantName,
        String result
) {}
