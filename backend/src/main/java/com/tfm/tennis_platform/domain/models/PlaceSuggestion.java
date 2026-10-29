package com.tfm.tennis_platform.domain.models;

public record PlaceSuggestion(
        String placeId,
        String name,
        String formattedAddress,
        Double latitude,
        Double longitude,
        String mapsUrl
) {
}
