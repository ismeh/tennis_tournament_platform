package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.Surface;

import java.time.LocalDate;
import java.time.LocalTime;

public record TournamentRequest(
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
    Integer courtCount
) {}
