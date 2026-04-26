package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDate;

public record ProfileRequest(
        String firstName,
        String lastName,
        String gender,
        LocalDate birthDate,
        String nationality,
        String federationLicense
) {
}
