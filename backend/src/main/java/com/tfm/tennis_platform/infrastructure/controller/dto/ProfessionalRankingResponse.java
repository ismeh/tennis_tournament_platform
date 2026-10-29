package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDate;

public record ProfessionalRankingResponse(
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
