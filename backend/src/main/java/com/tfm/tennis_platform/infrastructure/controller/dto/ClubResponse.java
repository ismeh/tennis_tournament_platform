package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClubResponse(
        UUID id,
        String name,
        String slug,
        String tagline,
        String logoUrl,
        String primaryColor,
        String city,
        String country,
        Long memberCount
) {
}
