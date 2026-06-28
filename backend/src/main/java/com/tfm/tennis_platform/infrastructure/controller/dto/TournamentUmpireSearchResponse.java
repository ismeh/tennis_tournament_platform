package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record TournamentUmpireSearchResponse(
        UUID id,
        String email,
        String firstName,
        String lastName
) {
}
