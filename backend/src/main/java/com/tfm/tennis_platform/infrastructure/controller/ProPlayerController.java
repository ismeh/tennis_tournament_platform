package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.ProPlayerQueryService;
import com.tfm.tennis_platform.domain.models.ProPlayer;
import com.tfm.tennis_platform.infrastructure.controller.dto.ProPlayerSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pro-players")
@RequiredArgsConstructor
public class ProPlayerController {

    private final ProPlayerQueryService proPlayerQueryService;

    @GetMapping
    public ResponseEntity<List<ProPlayerSearchResponse>> search(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(proPlayerQueryService.search(query).stream()
                .map(ProPlayerController::toResponse)
                .toList());
    }

    private static ProPlayerSearchResponse toResponse(ProPlayer player) {
        return new ProPlayerSearchResponse(
                player.getId(),
                player.getLicense(),
                player.getFullName(),
                player.getFirstName(),
                player.getLastName(),
                player.getRankingPosition(),
                player.getAgeCategory(),
                player.getClubName(),
                player.getBirthDate(),
                player.getGender()
        );
    }
}
