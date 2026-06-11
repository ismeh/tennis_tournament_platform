package com.tfm.tennis_platform.domain.models;

import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TournamentSummary(
        UUID id,
        String name,
        LocalDate playStartDate,
        LocalDate playEndDate,
        LocalTime startTime,
        LocalDate inscriptionStartDate,
        LocalDate inscriptionEndDate,
        Surface surface,
        Integer maxPlayers,
        String location,
        TournamentStatus status,
        boolean professionalTournament
) {}
