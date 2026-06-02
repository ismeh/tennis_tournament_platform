package com.tfm.tennis_platform.domain.models.calendar;

import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TournamentCalendarItem(
        UUID id,
        String name,
        LocalDate playStartDate,
        LocalDate playEndDate,
        LocalTime startTime,
        String location,
        Surface surface,
        Integer maxPlayers,
        TournamentStatus status
) {}
