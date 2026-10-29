package com.puntomatch.infrastructure.controller.dto;

import com.puntomatch.domain.models.enums.MemberTier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProfileResponse(
        UUID memberId,
        String email,
        MemberTier tier,
        LocalDateTime registeredAt,
        UUID personId,
        String firstName,
        String lastName,
        String gender,
        LocalDate birthDate,
        String nationality,
        String federationLicense
) {
}
