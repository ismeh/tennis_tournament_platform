package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.MatchService;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResponse;
import com.tfm.tennis_platform.infrastructure.controller.mapper.MatchWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final MatchWebMapper matchMapper;

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<MatchResponse>> getByTournament(@PathVariable UUID tournamentId) {
        return ResponseEntity.ok(matchService.findByTournamentId(tournamentId).stream()
                .map(matchMapper::toResponse)
                .toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatchResponse> update(@PathVariable UUID id, @RequestBody MatchResponse request) {
        Match match = matchMapper.toDomain(request);
        Match existingMatch = matchService.findById(id.toString()).orElse(null);
        // Note: Using id from path variable
        Match updatedMatch = Match.builder()
                .id(id)
            .drawId(existingMatch != null ? existingMatch.getDrawId() : null)
                .firstInscription(match.getFirstInscription())
                .secondInscription(match.getSecondInscription())
                .winner(match.getWinner())
                .roundNumber(match.getRoundNumber())
                .scheduledAt(match.getScheduledAt())
                .court(match.getCourt())
                .result(match.getResult())
            .nextMatch(existingMatch != null ? existingMatch.getNextMatch() : null)
                .build();
        
        return ResponseEntity.ok(matchMapper.toResponse(matchService.update(updatedMatch)));
    }
}
