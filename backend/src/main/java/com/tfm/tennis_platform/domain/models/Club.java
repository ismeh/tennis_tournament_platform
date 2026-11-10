package com.tfm.tennis_platform.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Club {
    private final UUID id;
    private final String name;
    private final String slug;
    private final String tagline;
    private final String description;
    private final String logoUrl;
    private final String bannerUrl;
    private final String primaryColor;
    private final String secondaryColor;
    private final String city;
    private final String country;
    private final String contactEmail;
    private final String phone;
    private final String website;
    private final String instagramUrl;
    private final String facebookUrl;
    private final String xUrl;
    private final Integer foundedYear;
    private final boolean publicPageEnabled;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
