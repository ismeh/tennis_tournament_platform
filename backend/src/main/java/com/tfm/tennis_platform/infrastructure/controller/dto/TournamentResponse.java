package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TournamentResponse(
    UUID id,
    String formalName,
    LocalDate playStartDate,
    LocalDate playEndDate,
    LocalTime tournamentStartTime,
    LocalDate inscriptionStartDate,
    LocalDate inscriptionEndDate,
    Surface surfaceCategory,
    Integer maxPlayers,
    String location,
    Double locationLatitude,
    Double locationLongitude,
    String locationPlaceId,
    String locationFormattedAddress,
    TournamentStatus status,
    Member providerOrganisationId,
    List<TournamentEventResponse> events,
    Boolean professionalTournament,
    Integer setsPerMatch,
    Integer decisiveTiebreakPoints,
    Integer gamesPerSet
) {}
