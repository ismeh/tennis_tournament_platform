package com.tfm.tennis_platform.infrastructure.geocoding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfm.tennis_platform.domain.models.PlaceSuggestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GooglePlacesSearchAdapter")
class GooglePlacesSearchAdapterTest {

    private final GooglePlacesProperties noKeyProperties = new GooglePlacesProperties(null, null, 5);
    private final GooglePlacesProperties validProperties = new GooglePlacesProperties("test-key", "https://places.googleapis.com", 5);
    private final ObjectMapper realMapper = new ObjectMapper();
    @Mock
    private ObjectMapper objectMapper;

    private JsonNode parseJson(String json) {
        try {
            return realMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final String AUTOCOMPLETE_JSON = """
            {
              "suggestions": [
                {
                  "placePrediction": {
                    "placeId": "ChIJ123",
                    "text": {"text": "Madrid, Spain"},
                    "structuredFormat": {
                      "mainText": {"text": "Madrid"},
                      "secondaryText": {"text": "Spain"}
                    }
                  }
                }
              ]
            }
            """;

    private static final String DETAILS_JSON = """
            {
              "id": "ChIJ123",
              "displayName": {"text": "Madrid"},
              "formattedAddress": "Madrid, Spain",
              "location": {"latitude": 40.4168, "longitude": -3.7038}
            }
            """;

    private static final String DETAILS_JSON_COORDS_ONLY = """
            {
              "id": "ChIJ123",
              "location": {"latitude": 40.4168, "longitude": -3.7038}
            }
            """;

    private static final String DETAILS_JSON_EMPTY = """
            {
              "id": "ChIJ123"
            }
            """;

    private record PostChain(
            RestClient restClient,
            RestClient.RequestBodySpec bodySpec,
            RestClient.ResponseSpec responseSpec
    ) {
    }

    private PostChain stubPostChain() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        doReturn(bodySpec).when(bodySpec).body(any(Object.class));
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        return new PostChain(restClient, bodySpec, responseSpec);
    }

    private void stubGetChain(RestClient restClient, RestClient.ResponseSpec responseSpec) {
        RestClient.RequestHeadersUriSpec getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec getHeadersSpec = mock(RestClient.RequestHeadersSpec.class);

        when(restClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString(), any(Object[].class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.header(anyString(), anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

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

        @Test
        @DisplayName("should return suggestions with full details when response is valid")
        void should_return_suggestions_with_full_details() throws Exception {
            PostChain post = stubPostChain();
            stubGetChain(post.restClient(), post.responseSpec());

            when(post.responseSpec().body(String.class)).thenReturn(AUTOCOMPLETE_JSON).thenReturn(DETAILS_JSON);
            doAnswer(inv -> parseJson(inv.getArgument(0, String.class))).when(objectMapper).readTree(anyString());

            GooglePlacesSearchAdapter adapter = new GooglePlacesSearchAdapter(post.restClient(), validProperties, objectMapper);
            List<PlaceSuggestion> result = adapter.autocomplete("Madrid", null, null);

            assertThat(result).hasSize(1);
            PlaceSuggestion suggestion = result.getFirst();
            assertThat(suggestion.placeId()).isEqualTo("ChIJ123");
            assertThat(suggestion.name()).isEqualTo("Madrid");
            assertThat(suggestion.formattedAddress()).isEqualTo("Madrid, Spain");
            assertThat(suggestion.latitude()).isEqualTo(40.4168);
            assertThat(suggestion.longitude()).isEqualTo(-3.7038);
            assertThat(suggestion.mapsUrl()).contains("ChIJ123");
        }

        @Test
        @DisplayName("should include locationBias when bias coordinates are provided")
        void should_include_location_bias_when_bias_coordinates_provided() throws Exception {
            PostChain post = stubPostChain();
            stubGetChain(post.restClient(), post.responseSpec());

            when(post.responseSpec().body(String.class)).thenReturn(AUTOCOMPLETE_JSON).thenReturn(DETAILS_JSON);
            doAnswer(inv -> parseJson(inv.getArgument(0, String.class))).when(objectMapper).readTree(anyString());

            GooglePlacesSearchAdapter adapter = new GooglePlacesSearchAdapter(post.restClient(), validProperties, objectMapper);
            adapter.autocomplete("Madrid", 40.0, -3.0);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
            verify(post.bodySpec()).body(bodyCaptor.capture());
            Map<String, Object> capturedBody = bodyCaptor.getValue();

            assertThat(capturedBody).containsKey("locationBias");
            @SuppressWarnings("unchecked")
            Map<String, Object> locationBias = (Map<String, Object>) capturedBody.get("locationBias");
            assertThat(locationBias).containsKey("circle");
            @SuppressWarnings("unchecked")
            Map<String, Object> circle = (Map<String, Object>) locationBias.get("circle");
            assertThat(circle).containsKey("center");
            assertThat(circle).containsEntry("radius", 50000.0);
        }

        @Test
        @DisplayName("should return empty list on RestClientException")
        void should_return_empty_list_on_rest_client_exception() {
            PostChain post = stubPostChain();

            when(post.responseSpec().body(String.class)).thenThrow(new RestClientException("Connection refused"));

            GooglePlacesSearchAdapter adapter = new GooglePlacesSearchAdapter(post.restClient(), validProperties, objectMapper);
            List<PlaceSuggestion> result = adapter.autocomplete("Madrid", null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list on parse error")
        void should_return_empty_list_on_parse_error() throws Exception {
            PostChain post = stubPostChain();

            when(post.responseSpec().body(String.class)).thenReturn("not valid json");
            doThrow(new RuntimeException("Unexpected token")).when(objectMapper).readTree(anyString());

            GooglePlacesSearchAdapter adapter = new GooglePlacesSearchAdapter(post.restClient(), validProperties, objectMapper);
            List<PlaceSuggestion> result = adapter.autocomplete("Madrid", null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when response body is null")
        void should_return_empty_list_when_response_body_is_null() {
            PostChain post = stubPostChain();

            when(post.responseSpec().body(String.class)).thenReturn(null);

            GooglePlacesSearchAdapter adapter = new GooglePlacesSearchAdapter(post.restClient(), validProperties, objectMapper);
            List<PlaceSuggestion> result = adapter.autocomplete("Madrid", null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return full details when fetchPlaceDetails succeeds")
        void should_return_full_details_when_fetch_place_details_succeeds() throws Exception {
            PostChain post = stubPostChain();
            stubGetChain(post.restClient(), post.responseSpec());

            when(post.responseSpec().body(String.class)).thenReturn(AUTOCOMPLETE_JSON).thenReturn(DETAILS_JSON);
            doAnswer(inv -> parseJson(inv.getArgument(0, String.class))).when(objectMapper).readTree(anyString());

            GooglePlacesSearchAdapter adapter = new GooglePlacesSearchAdapter(post.restClient(), validProperties, objectMapper);
            List<PlaceSuggestion> result = adapter.autocomplete("Madrid", null, null);

            assertThat(result).hasSize(1);
            PlaceSuggestion suggestion = result.getFirst();
            assertThat(suggestion.placeId()).isEqualTo("ChIJ123");
            assertThat(suggestion.name()).isEqualTo("Madrid");
            assertThat(suggestion.formattedAddress()).isEqualTo("Madrid, Spain");
            assertThat(suggestion.latitude()).isEqualTo(40.4168);
            assertThat(suggestion.longitude()).isEqualTo(-3.7038);
            assertThat(suggestion.mapsUrl()).startsWith("https://www.google.com/maps/search/");
            assertThat(suggestion.mapsUrl()).contains("query_place_id=ChIJ123");
        }

        @Test
        @DisplayName("should fall back to prediction when fetchPlaceDetails throws RestClientException")
        void should_fallback_to_prediction_when_details_throws_rest_client_exception() throws Exception {
            PostChain post = stubPostChain();

            when(post.responseSpec().body(String.class)).thenReturn(AUTOCOMPLETE_JSON);
            doAnswer(inv -> parseJson(inv.getArgument(0, String.class))).when(objectMapper).readTree(anyString());

            RestClient.RequestHeadersUriSpec getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec getHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
            when(post.restClient().get()).thenReturn(getUriSpec);
            when(getUriSpec.uri(anyString(), any(Object[].class))).thenReturn(getHeadersSpec);
            when(getHeadersSpec.header(anyString(), anyString())).thenReturn(getHeadersSpec);
            when(getHeadersSpec.retrieve()).thenThrow(new RestClientException("Details unavailable"));

            GooglePlacesSearchAdapter adapter = new GooglePlacesSearchAdapter(post.restClient(), validProperties, objectMapper);
            List<PlaceSuggestion> result = adapter.autocomplete("Madrid", null, null);

            assertThat(result).hasSize(1);
            PlaceSuggestion suggestion = result.getFirst();
            assertThat(suggestion.placeId()).isEqualTo("ChIJ123");
            assertThat(suggestion.name()).isEqualTo("Madrid");
            assertThat(suggestion.formattedAddress()).isEqualTo("Madrid, Spain");
            assertThat(suggestion.latitude()).isNull();
            assertThat(suggestion.longitude()).isNull();
            assertThat(suggestion.mapsUrl()).contains("query_place_id=ChIJ123");
        }

        @Test
        @DisplayName("should use coordinates in mapsUrl when display name is absent")
        void should_use_coordinates_in_maps_url_when_display_name_absent() throws Exception {
            PostChain post = stubPostChain();
            stubGetChain(post.restClient(), post.responseSpec());

            when(post.responseSpec().body(String.class)).thenReturn(AUTOCOMPLETE_JSON).thenReturn(DETAILS_JSON_COORDS_ONLY);
            doAnswer(inv -> parseJson(inv.getArgument(0, String.class))).when(objectMapper).readTree(anyString());

            GooglePlacesSearchAdapter adapter = new GooglePlacesSearchAdapter(post.restClient(), validProperties, objectMapper);
            List<PlaceSuggestion> result = adapter.autocomplete("Madrid", null, null);

            assertThat(result).hasSize(1);
            PlaceSuggestion suggestion = result.getFirst();
            assertThat(suggestion.name()).isNull();
            assertThat(suggestion.latitude()).isEqualTo(40.4168);
            assertThat(suggestion.longitude()).isEqualTo(-3.7038);
            assertThat(suggestion.mapsUrl()).contains("40.416800").contains("-3.703800");
            assertThat(suggestion.mapsUrl()).contains("query_place_id=ChIJ123");
        }

        @Test
        @DisplayName("should use placeId in mapsUrl when query and coordinates are absent")
        void should_use_place_id_in_maps_url_when_query_and_coordinates_absent() throws Exception {
            PostChain post = stubPostChain();
            stubGetChain(post.restClient(), post.responseSpec());

            when(post.responseSpec().body(String.class)).thenReturn(AUTOCOMPLETE_JSON).thenReturn(DETAILS_JSON_EMPTY);
            doAnswer(inv -> parseJson(inv.getArgument(0, String.class))).when(objectMapper).readTree(anyString());

            GooglePlacesSearchAdapter adapter = new GooglePlacesSearchAdapter(post.restClient(), validProperties, objectMapper);
            List<PlaceSuggestion> result = adapter.autocomplete("Madrid", null, null);

            assertThat(result).hasSize(1);
            PlaceSuggestion suggestion = result.getFirst();
            assertThat(suggestion.name()).isNull();
            assertThat(suggestion.latitude()).isNull();
            assertThat(suggestion.longitude()).isNull();
            assertThat(suggestion.mapsUrl()).contains("query=ChIJ123");
            assertThat(suggestion.mapsUrl()).contains("query_place_id=ChIJ123");
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
