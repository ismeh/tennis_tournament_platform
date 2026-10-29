package com.puntomatch.infrastructure.controller.dto;

import java.util.List;

public record TournamentInscriptionCategoryCountResponse(
        Integer categoryId,
        String category,
        long totalPlayers,
        List<TournamentInscriptionGenderCountResponse> genders
) {
}
