package com.tfm.tennis_platform.domain.models.inscription;

import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;

import java.time.LocalDate;
import java.util.UUID;

public record ManualEventInscriptionCommand(
        ParticipantSource playerSource,
        UUID personId,
        String firstName,
        String lastName,
        String gender,
        LocalDate birthDate,
        String nationality,
        String tennisId
) {
}
