package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDate;

public record ProPlayerSearchResponse(
        Integer id,
        String license,
        String fullName,
        String firstName,
        String lastName,
        Integer rankingPosition,
        String ageCategory,
        String clubName,
        LocalDate birthDate,
        String gender
) {
}
