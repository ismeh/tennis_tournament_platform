package com.tfm.tennis_platform.domain.models.inscription;

import java.util.UUID;

public record TournamentInscriptionEventView(
        UUID eventId,
        Integer categoryId,
        String category,
        String eventName,
        String eventGender
) {
}
