package com.tfm.tennis_platform.infrastructure.controller.dto;

public record PlaceSuggestionResponse(
        String placeId,
        String name,
        String formattedAddress,
        Double latitude,
        Double longitude,
        String mapsUrl
) {
}
