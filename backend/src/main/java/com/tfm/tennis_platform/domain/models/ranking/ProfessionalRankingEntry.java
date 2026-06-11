package com.tfm.tennis_platform.domain.models.ranking;

import java.time.LocalDate;

public record ProfessionalRankingEntry(
        Integer position,
        Integer playerId,
        String license,
        String fullName,
        String firstName,
        String lastName,
        String gender,
        String category,
        String clubName,
        LocalDate birthDate,
        Integer points
) {}
