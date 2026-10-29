package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.RankingService;
import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.ranking.RankingPage;
import com.tfm.tennis_platform.domain.models.ranking.TournamentRankingEntry;
import com.tfm.tennis_platform.infrastructure.controller.dto.RankingPageResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentRankingResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;
    private final TournamentService tournamentService;

    @GetMapping("/tournaments")
    public ResponseEntity<List<TournamentSummaryResponse>> getRankingTournaments() {
        return ResponseEntity.ok(tournamentService.findSummaries().stream()
                .map(RankingController::toTournamentSummaryResponse)
                .toList());
    }

    @GetMapping("/tournaments/{tournamentId}")
    public ResponseEntity<RankingPageResponse<TournamentRankingResponse>> getTournamentRanking(
            @PathVariable UUID tournamentId,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        return ResponseEntity.ok(toTournamentRankingPageResponse(
                rankingService.findTournamentRanking(tournamentId, gender, categoryId, page, size, sortBy, sortDirection)
        ));
    }

    private static RankingPageResponse<TournamentRankingResponse> toTournamentRankingPageResponse(
            RankingPage<TournamentRankingEntry> page
    ) {
        return new RankingPageResponse<>(
                page.items().stream().map(RankingController::toTournamentRankingResponse).toList(),
                page.page(),
                page.size(),
                page.totalItems(),
                page.totalPages(),
                page.sortBy(),
                page.sortDirection()
        );
    }

    private static TournamentRankingResponse toTournamentRankingResponse(TournamentRankingEntry entry) {
        return new TournamentRankingResponse(
                entry.position(),
                entry.participantId(),
                entry.license(),
                entry.firstName(),
                entry.lastName(),
                entry.gender(),
                entry.points(),
                entry.victories()
        );
    }

    private static TournamentSummaryResponse toTournamentSummaryResponse(TournamentSummary tournament) {
        return new TournamentSummaryResponse(
                tournament.id(),
                tournament.name(),
                tournament.playStartDate(),
                tournament.playEndDate(),
                tournament.startTime(),
                tournament.inscriptionStartDate(),
                tournament.inscriptionEndDate(),
                tournament.surface(),
                tournament.maxPlayers(),
                tournament.location(),
                tournament.status(),
                tournament.professionalTournament()
        );
    }
}
