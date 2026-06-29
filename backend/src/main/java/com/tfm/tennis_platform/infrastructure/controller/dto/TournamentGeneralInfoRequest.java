package com.tfm.tennis_platform.infrastructure.controller.dto;

import com.tfm.tennis_platform.domain.models.enums.Surface;

import java.time.LocalDate;
import java.time.LocalTime;

public record TournamentGeneralInfoRequest(
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
    String locationFormattedAddress
) {

    public boolean hasFormalName() {
        return formalName != null && !formalName.isBlank();
    }

    public boolean hasPlayPeriod() {
        return playStartDate != null && playEndDate != null;
    }

    public boolean hasInscriptionPeriod() {
        return inscriptionStartDate != null && inscriptionEndDate != null;
    }

    public boolean hasSurfaceCategory() {
        return surfaceCategory != null;
    }

    public boolean hasMaxPlayers() {
        return maxPlayers != null && maxPlayers > 0;
    }

    public boolean hasLocation() {
        return location != null && !location.isBlank();
    }

    public boolean hasTournamentStartTime() {
        return tournamentStartTime != null;
    }
}
