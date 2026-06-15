package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.GeoCoordinates;
import com.tfm.tennis_platform.domain.models.PlaceSuggestion;

import java.util.List;
import java.util.Optional;

public interface PlaceSearchPort {

    List<PlaceSuggestion> autocomplete(String text, Double biasLatitude, Double biasLongitude);

    Optional<GeoCoordinates> geolocateIp(String ipAddress);
}
