package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record TournamentInscriptionPlayerResponse(
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
