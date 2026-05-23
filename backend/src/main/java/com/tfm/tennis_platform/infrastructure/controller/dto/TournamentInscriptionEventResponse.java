package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record TournamentInscriptionEventResponse(
        UUID eventId,
        Integer categoryId,
        String category,
        String eventName,
        String eventGender
) {
}
