package com.tfm.tennis_platform.infrastructure.geocoding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfm.tennis_platform.domain.models.GeoCoordinates;
import com.tfm.tennis_platform.domain.models.PlaceSuggestion;
import com.tfm.tennis_platform.domain.port.out.PlaceSearchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesSearchAdapter implements PlaceSearchPort {

    private static final String API_KEY_HEADER = "X-Goog-Api-Key";
    private static final String FIELD_MASK_HEADER = "X-Goog-FieldMask";
    private static final String AUTOCOMPLETE_FIELD_MASK = "suggestions.placePrediction.placeId,suggestions.placePrediction.text,suggestions.placePrediction.structuredFormat";
    private static final String DETAILS_FIELD_MASK = "id,displayName,formattedAddress,location";

    private final RestClient googlePlacesRestClient;
    private final GooglePlacesProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public List<PlaceSuggestion> autocomplete(String text, Double biasLatitude, Double biasLongitude) {
        if (!properties.hasApiKey()) {
            log.warn("Google Places API key is not configured; returning no place suggestions.");
            return List.of();
        }

        try {
            String responseBody = googlePlacesRestClient.post()
                    .uri("/v1/places:autocomplete")
                    .header(API_KEY_HEADER, properties.apiKey())
                    .header(FIELD_MASK_HEADER, AUTOCOMPLETE_FIELD_MASK)
                    .body(buildAutocompleteRequest(text, biasLatitude, biasLongitude))
                    .retrieve()
                    .body(String.class);

            if (responseBody == null) {
                return List.of();
            }

            JsonNode response = objectMapper.readTree(responseBody);
            return toSuggestions(response);
        } catch (RestClientException exception) {
            log.warn("Google Places autocomplete request failed for text '{}'", text, exception);
            return List.of();
        } catch (Exception exception) {
            log.warn("Failed to parse Places autocomplete response for text '{}'", text, exception);
            return List.of();
        }
    }

    @Override
    public Optional<GeoCoordinates> geolocateIp(String ipAddress) {
        return Optional.empty();
    }

    private Map<String, Object> buildAutocompleteRequest(String text, Double biasLatitude, Double biasLongitude) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("input", text);
        request.put("languageCode", "es");
        request.put("regionCode", "ES");

        if (biasLatitude != null && biasLongitude != null) {
            request.put("locationBias", Map.of(
                    "circle", Map.of(
                            "center", Map.of(
                                    "latitude", biasLatitude,
                                    "longitude", biasLongitude
                            ),
                            "radius", 50000.0
                    )
            ));
        }

        return request;
    }

    private List<PlaceSuggestion> toSuggestions(JsonNode response) {
        List<PlaceSuggestion> suggestions = new ArrayList<>();

        if (response == null) {
            return suggestions;
        }

        for (JsonNode suggestion : response.path("suggestions")) {
            JsonNode prediction = suggestion.path("placePrediction");
            String placeId = prediction.path("placeId").asText(null);
            if (placeId == null || placeId.isBlank()) {
                continue;
            }

            suggestions.add(fetchPlaceDetails(placeId)
                    .orElseGet(() -> toPredictionSuggestion(placeId, prediction)));
        }

        return suggestions;
    }

    private Optional<PlaceSuggestion> fetchPlaceDetails(String placeId) {
        try {
            String responseBody = googlePlacesRestClient.get()
                    .uri("/v1/places/{placeId}", placeId)
                    .header(API_KEY_HEADER, properties.apiKey())
                    .header(FIELD_MASK_HEADER, DETAILS_FIELD_MASK)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null) {
                return Optional.empty();
            }

            JsonNode response = objectMapper.readTree(responseBody);

            String formattedAddress = response.path("formattedAddress").asText(null);
            String displayName = response.path("displayName").path("text").asText(null);
            JsonNode location = response.path("location");
            Double latitude = location.has("latitude") ? location.path("latitude").asDouble() : null;
            Double longitude = location.has("longitude") ? location.path("longitude").asDouble() : null;

            return Optional.of(new PlaceSuggestion(
                    placeId,
                    displayName != null ? displayName : formattedAddress,
                    formattedAddress,
                    latitude,
                    longitude,
                    buildMapsUrl(displayName != null ? displayName : formattedAddress, placeId, latitude, longitude)
            ));
        } catch (RestClientException exception) {
            log.warn("Google Places details request failed for placeId '{}'", placeId, exception);
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("Failed to parse Places details response for placeId '{}'", placeId, exception);
            return Optional.empty();
        }
    }

    private PlaceSuggestion toPredictionSuggestion(String placeId, JsonNode prediction) {
        String text = prediction.path("text").path("text").asText(null);
        String mainText = prediction.path("structuredFormat").path("mainText").path("text").asText(null);
        return new PlaceSuggestion(
                placeId,
                mainText != null ? mainText : text,
                text,
                null,
                null,
                buildMapsUrl(text, placeId, null, null)
        );
    }

    private String buildMapsUrl(String query, String placeId, Double latitude, Double longitude) {
        String resolvedQuery = query;
        if ((resolvedQuery == null || resolvedQuery.isBlank()) && latitude != null && longitude != null) {
            resolvedQuery = toCoordinate(latitude) + "," + toCoordinate(longitude);
        }
        if (resolvedQuery == null || resolvedQuery.isBlank()) {
            resolvedQuery = placeId;
        }

        return "https://www.google.com/maps/search/?api=1&query="
                + encodeUrlValue(resolvedQuery)
                + "&query_place_id="
                + encodeUrlValue(placeId);
    }

    private String toCoordinate(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private String encodeUrlValue(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
