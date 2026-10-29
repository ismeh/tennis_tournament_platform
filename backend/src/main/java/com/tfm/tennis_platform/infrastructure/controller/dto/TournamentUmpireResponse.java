package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TournamentUmpireResponse(
        UUID id,
        UUID tournamentId,
        UUID umpireId,
        String umpireEmail,
        String umpireFirstName,
        String umpireLastName,
        LocalDateTime assignedAt
) {
}
