package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.Surface;

import java.time.LocalDate;

public record TournamentRequest(
    String formalName,
    LocalDate playStartDate,
    LocalDate playEndDate,
    LocalDate inscriptionStartDate,
    LocalDate inscriptionEndDate,
    Surface surfaceCategory,
    Integer maxPlayers,
    String location
) {}
