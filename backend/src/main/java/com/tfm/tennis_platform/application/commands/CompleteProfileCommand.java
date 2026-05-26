package com.tfm.tennis_platform.application.commands;

import java.time.LocalDate;

public record CompleteProfileCommand(
        String firstName,
        String lastName,
        String gender,
        LocalDate birthDate,
        String nationality,
        String federationLicense
) {
}
