package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record ClubRankingResponse(
        Integer position,
        UUID personId,
        String firstName,
        String lastName,
        String gender,
        String license,
        Long points,
        Long victories,
        Long tournaments
) {
}
