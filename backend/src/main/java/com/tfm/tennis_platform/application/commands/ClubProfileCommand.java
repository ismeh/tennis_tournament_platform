package com.tfm.tennis_platform.application.commands;

public record ClubProfileCommand(
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
) {}
