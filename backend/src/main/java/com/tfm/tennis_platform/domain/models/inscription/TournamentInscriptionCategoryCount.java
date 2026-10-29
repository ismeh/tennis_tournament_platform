package com.tfm.tennis_platform.domain.models.inscription;

import java.util.List;

public record TournamentInscriptionCategoryCount(
        Integer categoryId,
        String category,
        long totalPlayers,
        List<TournamentInscriptionGenderCount> genders
) {
}
