package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;

import java.time.LocalDate;
import java.util.UUID;

public record TournamentResponse(
    UUID id,
    String formalName,
    LocalDate playStartDate,
    LocalDate playEndDate,
    LocalDate inscriptionStartDate,
    LocalDate inscriptionEndDate,
    Surface surfaceCategory,
    Integer maxPlayers,
    String location,
    TournamentStatus status,
    UUID providerOrganisationId
) {}
