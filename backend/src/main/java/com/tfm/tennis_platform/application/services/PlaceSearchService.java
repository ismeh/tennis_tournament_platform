package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.GeoCoordinates;
import com.tfm.tennis_platform.domain.models.PlaceSuggestion;
import com.tfm.tennis_platform.domain.port.out.PlaceSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceSearchService {

    private final PlaceSearchPort placeSearchPort;

    public List<PlaceSuggestion> search(String text, Double latitude, Double longitude, String clientIp) {
        if (text == null || text.trim().length() < 2) {
            return List.of();
        }

        Double biasLatitude = latitude;
        Double biasLongitude = longitude;

        if (biasLatitude == null || biasLongitude == null) {
            GeoCoordinates ipLocation = placeSearchPort.geolocateIp(clientIp).orElse(null);
            if (ipLocation != null) {
                biasLatitude = ipLocation.latitude();
                biasLongitude = ipLocation.longitude();
            }
        }

        return placeSearchPort.autocomplete(text.trim(), biasLatitude, biasLongitude);
    }
}
