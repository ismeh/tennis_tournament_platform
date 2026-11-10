package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClubPageResponse(
        UUID id,
        String name,
        String slug,
        String tagline,
        String description,
        String logoUrl,
        String bannerUrl,
        String primaryColor,
        String secondaryColor,
        String city,
        String country,
        String contactEmail,
        String phone,
        String website,
        String instagramUrl,
        String facebookUrl,
        String xUrl,
        Integer foundedYear,
        boolean publicPageEnabled,
        Long memberCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
