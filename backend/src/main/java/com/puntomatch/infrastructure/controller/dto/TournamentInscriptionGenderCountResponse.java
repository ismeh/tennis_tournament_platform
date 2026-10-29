package com.puntomatch.infrastructure.controller.dto;

public record TournamentInscriptionGenderCountResponse(
        String gender,
        long totalPlayers
) {
}
