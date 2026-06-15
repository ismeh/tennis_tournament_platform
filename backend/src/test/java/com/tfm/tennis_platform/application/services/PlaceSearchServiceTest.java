package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.GeoCoordinates;
import com.tfm.tennis_platform.domain.models.PlaceSuggestion;
import com.tfm.tennis_platform.domain.port.out.PlaceSearchPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceSearchServiceTest {

    @Mock
    private PlaceSearchPort placeSearchPort;

    @InjectMocks
    private PlaceSearchService placeSearchService;

    @Test
    void searchShouldReturnEmptyListWhenTextIsShortOrNull() {
        assertTrue(placeSearchService.search(null, 40.0, -3.0, "127.0.0.1").isEmpty());
        assertTrue(placeSearchService.search("", 40.0, -3.0, "127.0.0.1").isEmpty());
        assertTrue(placeSearchService.search("a", 40.0, -3.0, "127.0.0.1").isEmpty());
        verifyNoInteractions(placeSearchPort);
    }

    @Test
    void searchShouldUseProvidedCoordinatesAndNotGeolocateIp() {
        String query = "Madrid";
        List<PlaceSuggestion> expected = List.of(new PlaceSuggestion("id1", "Madrid", "Madrid, Spain", 40.4167, -3.7037, "mapsUrl"));
        when(placeSearchPort.autocomplete(query, 40.0, -3.0)).thenReturn(expected);

        List<PlaceSuggestion> actual = placeSearchService.search("  Madrid ", 40.0, -3.0, "192.168.1.1");

        assertEquals(expected, actual);
        verify(placeSearchPort, never()).geolocateIp(anyString());
        verify(placeSearchPort).autocomplete(query, 40.0, -3.0);
    }

    @Test
    void searchShouldGeolocateIpWhenCoordinatesAreNullAndUseItAsBias() {
        String query = "Madrid";
        String clientIp = "8.8.8.8";
        GeoCoordinates mockCoords = new GeoCoordinates(40.4167, -3.7037);
        List<PlaceSuggestion> expected = List.of(new PlaceSuggestion("id1", "Madrid", "Madrid, Spain", 40.4167, -3.7037, "mapsUrl"));

        when(placeSearchPort.geolocateIp(clientIp)).thenReturn(Optional.of(mockCoords));
        when(placeSearchPort.autocomplete(query, 40.4167, -3.7037)).thenReturn(expected);

        List<PlaceSuggestion> actual = placeSearchService.search(query, null, null, clientIp);

        assertEquals(expected, actual);
        verify(placeSearchPort).geolocateIp(clientIp);
        verify(placeSearchPort).autocomplete(query, 40.4167, -3.7037);
    }

    @Test
    void searchShouldAutocompleteWithoutBiasWhenCoordsAreNullAndIpGeolocationFails() {
        String query = "Madrid";
        String clientIp = "8.8.8.8";

        when(placeSearchPort.geolocateIp(clientIp)).thenReturn(Optional.empty());
        when(placeSearchPort.autocomplete(query, null, null)).thenReturn(List.of());

        List<PlaceSuggestion> actual = placeSearchService.search(query, null, null, clientIp);

        assertTrue(actual.isEmpty());
        verify(placeSearchPort).geolocateIp(clientIp);
        verify(placeSearchPort).autocomplete(query, null, null);
    }
}
