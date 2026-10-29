package com.tfm.tennis_platform.infrastructure.geocoding;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.google.places")
public record GooglePlacesProperties(
        String apiKey,
        String baseUrl,
        int autocompleteLimit
) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
