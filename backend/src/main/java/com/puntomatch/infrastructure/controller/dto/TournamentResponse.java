package com.puntomatch.infrastructure.controller.dto;

import com.puntomatch.domain.models.Member;
import com.puntomatch.domain.models.enums.Surface;
import com.puntomatch.domain.models.enums.TournamentStatus;

import java.time.LocalDate;
import java.util.List;
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
    Member providerOrganisationId,
    List<TournamentEventResponse> events
) {}
