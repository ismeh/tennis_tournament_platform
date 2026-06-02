package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TournamentCalendarResponse(
        UUID id,
        String formalName,
        LocalDate playStartDate,
        LocalDate playEndDate,
        LocalTime tournamentStartTime,
        String location,
        Surface surfaceCategory,
        Integer maxPlayers,
        TournamentStatus status
) {}
