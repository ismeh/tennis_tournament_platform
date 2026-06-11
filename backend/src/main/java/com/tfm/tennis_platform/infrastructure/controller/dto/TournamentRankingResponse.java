package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record TournamentRankingResponse(
        Integer position,
        UUID participantId,
        String license,
        String firstName,
        String lastName,
        String gender,
        Long victories
) {}
