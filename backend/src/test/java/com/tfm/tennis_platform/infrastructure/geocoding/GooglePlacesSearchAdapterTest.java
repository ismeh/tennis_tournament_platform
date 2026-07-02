package com.tfm.tennis_platform.infrastructure.geocoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfm.tennis_platform.domain.models.PlaceSuggestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("GooglePlacesSearchAdapter")
class GooglePlacesSearchAdapterTest {

    private final GooglePlacesProperties noKeyProperties = new GooglePlacesProperties(null, null, 5);
    private final GooglePlacesProperties validProperties = new GooglePlacesProperties("test-key", "https://places.googleapis.com", 5);
    @Mock private ObjectMapper objectMapper;

    @Nested
    @DisplayName("autocomplete")
    class AutocompleteTests {

        @Test
        @DisplayName("should return empty list when API key is not configured")
        void should_return_empty_when_no_api_key() {
            RestClient restClient = mock(RestClient.class);
            GooglePlacesSearchAdapter noKeyAdapter = new GooglePlacesSearchAdapter(restClient, noKeyProperties, objectMapper);

            List<PlaceSuggestion> result = noKeyAdapter.autocomplete("Madrid", null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when geolocateIp is called")
        void should_return_empty_for_geolocate_ip() {
            RestClient restClient = mock(RestClient.class);
            GooglePlacesSearchAdapter testAdapter = new GooglePlacesSearchAdapter(restClient, validProperties, objectMapper);

            Optional<com.tfm.tennis_platform.domain.models.GeoCoordinates> result = testAdapter.geolocateIp("127.0.0.1");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("geolocateIp")
    class GeolocateIpTests {

        @Test
        @DisplayName("should always return empty optional")
        void should_return_empty_optional() {
            RestClient restClient = mock(RestClient.class);
            GooglePlacesSearchAdapter testAdapter = new GooglePlacesSearchAdapter(restClient, validProperties, objectMapper);

            Optional<com.tfm.tennis_platform.domain.models.GeoCoordinates> result = testAdapter.geolocateIp("127.0.0.1");

            assertThat(result).isEmpty();
        }
    }
}
