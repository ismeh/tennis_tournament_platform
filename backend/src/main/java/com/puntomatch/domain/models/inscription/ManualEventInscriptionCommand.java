package com.puntomatch.domain.models.inscription;

import com.puntomatch.domain.models.enums.ParticipantSource;

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
        String tennisId,
        Integer proPlayerId
) {
}
