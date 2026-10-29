package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.ClubQueryService;
import com.tfm.tennis_platform.domain.models.Club;
import com.tfm.tennis_platform.infrastructure.controller.dto.ClubResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubQueryService clubQueryService;

    @GetMapping
    public ResponseEntity<List<ClubResponse>> search(@RequestParam(required = false) String q) {
        List<Club> clubs = clubQueryService.search(q);
        return ResponseEntity.ok(clubs.stream()
                .map(c -> new ClubResponse(c.getId(), c.getName()))
                .toList());
    }
}
