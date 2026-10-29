package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.List;

public record TournamentInscriptionCategoryCountResponse(
        Integer categoryId,
        String category,
        long totalPlayers,
        List<TournamentInscriptionGenderCountResponse> genders
) {
}
