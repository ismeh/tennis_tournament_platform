package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.PlaceSearchService;
import com.tfm.tennis_platform.infrastructure.controller.dto.PlaceSuggestionResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceSearchController {

    private final PlaceSearchService placeSearchService;

    @GetMapping("/search")
    public List<PlaceSuggestionResponse> search(
            @RequestParam("query") String query,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lng", required = false) Double lng,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);
        return placeSearchService.search(query, lat, lng, clientIp).stream()
                .map(suggestion -> new PlaceSuggestionResponse(
                        suggestion.placeId(),
                        suggestion.name(),
                        suggestion.formattedAddress(),
                        suggestion.latitude(),
                        suggestion.longitude(),
                        suggestion.mapsUrl()
                ))
                .collect(Collectors.toList());
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
