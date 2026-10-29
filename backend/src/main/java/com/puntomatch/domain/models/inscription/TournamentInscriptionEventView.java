package com.puntomatch.domain.models.inscription;

import java.util.UUID;

public record TournamentInscriptionEventView(
        UUID eventId,
        Integer categoryId,
        String category,
        String eventName,
        String eventGender
) {
}
