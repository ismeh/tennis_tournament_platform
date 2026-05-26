package com.tfm.tennis_platform.infrastructure.controller.dto;

public record TournamentInscriptionGenderCountResponse(
        String gender,
        long totalPlayers
) {
}
