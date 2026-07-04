package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.util.UUID;

public record ClubResponse(
        UUID id,
        String name
) {
}
