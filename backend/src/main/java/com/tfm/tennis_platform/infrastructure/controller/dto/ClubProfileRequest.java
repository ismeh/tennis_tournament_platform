package com.tfm.tennis_platform.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClubProfileRequest(
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
        Boolean publicPageEnabled
) {
    public com.tfm.tennis_platform.application.commands.ClubProfileCommand toCommand() {
        return new com.tfm.tennis_platform.application.commands.ClubProfileCommand(
                name, slug, tagline, description, logoUrl, bannerUrl, primaryColor, secondaryColor,
                city, country, contactEmail, phone, website, instagramUrl, facebookUrl, xUrl,
                foundedYear, publicPageEnabled
        );
    }
}
