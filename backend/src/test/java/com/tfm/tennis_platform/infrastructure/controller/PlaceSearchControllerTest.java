package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.PlaceSearchService;
import com.tfm.tennis_platform.domain.models.PlaceSuggestion;
import com.tfm.tennis_platform.infrastructure.controller.dto.PlaceSuggestionResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceSearchControllerTest {

    @Mock
    private PlaceSearchService placeSearchService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PlaceSearchController placeSearchController;

    @Test
    void searchShouldDelegateToServiceAndMapResponse() {
        String query = "Madrid";
        Double lat = 40.0;
        Double lng = -3.0;
        String ip = "8.8.8.8";

        when(request.getHeader("X-Forwarded-For")).thenReturn(ip);
        when(placeSearchService.search(query, lat, lng, ip)).thenReturn(List.of(
                new PlaceSuggestion("id1", "Madrid", "Madrid, Spain", 40.4167, -3.7037, "mapsUrl")
        ));

        List<PlaceSuggestionResponse> actual = placeSearchController.search(query, lat, lng, request);

        assertEquals(1, actual.size());
        assertEquals("id1", actual.get(0).placeId());
        assertEquals("Madrid", actual.get(0).name());
        assertEquals("Madrid, Spain", actual.get(0).formattedAddress());
        assertEquals(40.4167, actual.get(0).latitude());
        assertEquals(-3.7037, actual.get(0).longitude());
        assertEquals("mapsUrl", actual.get(0).mapsUrl());

        verify(placeSearchService).search(query, lat, lng, ip);
    }
}
