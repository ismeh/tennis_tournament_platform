package com.puntomatch.domain.models.calendar;

import com.puntomatch.domain.models.enums.Surface;
import com.puntomatch.domain.models.enums.TournamentStatus;

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
