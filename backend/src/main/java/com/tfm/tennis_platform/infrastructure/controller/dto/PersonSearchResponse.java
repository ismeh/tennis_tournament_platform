package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PersonSearchResponse(
        UUID id,
        String tennisId,
        String firstName,
        String lastName,
        String nationality,
        LocalDate birthDate,
        String gender
) {
}