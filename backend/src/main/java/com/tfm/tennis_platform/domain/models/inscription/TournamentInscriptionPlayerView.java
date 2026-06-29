package com.tfm.tennis_platform.domain.models.inscription;

import java.util.UUID;

public record TournamentInscriptionPlayerView(
        UUID inscriptionId,
        UUID participantId,
        UUID eventId,
        Integer categoryId,
        String category,
        String eventName,
        String eventGender,
        UUID personId,
        String playerSource,
        String tennisId,
        String firstName,
        String lastName,
        String gender,
        Integer points,
        Integer seed
) {
}
