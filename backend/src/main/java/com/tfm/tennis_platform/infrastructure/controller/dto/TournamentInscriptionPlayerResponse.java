package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record TournamentInscriptionPlayerResponse(
        UUID inscriptionId,
        UUID eventId,
        Integer categoryId,
        String category,
        String eventName,
        String eventGender,
        String firstName,
        String lastName,
        String gender
) {
}
