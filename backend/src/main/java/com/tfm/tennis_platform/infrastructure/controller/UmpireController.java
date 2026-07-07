package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.application.services.TournamentUmpireService;
import com.tfm.tennis_platform.domain.models.TournamentUmpire;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.UmpireSearchResult;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentUmpireRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentUmpireResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentUmpireSearchResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UmpireController {

    private final TournamentUmpireService tournamentUmpireService;
    private final TournamentService tournamentService;

    @GetMapping("/umpires/me/tournaments")
    public ResponseEntity<List<TournamentSummaryResponse>> getMyTournaments(Principal principal) {
        List<TournamentSummary> tournaments = tournamentService.findSummariesByUmpire(principal.getName());
        return ResponseEntity.ok(tournaments.stream()
                .map(t -> new TournamentSummaryResponse(
                        t.id(),
                        t.name(),
                        t.playStartDate(),
                        t.playEndDate(),
                        t.startTime(),
                        t.inscriptionStartDate(),
                        t.inscriptionEndDate(),
                        t.surface(),
                        t.maxPlayers(),
                        t.location(),
                        t.status(),
                        t.professionalTournament()
                ))
                .toList());
    }

    @GetMapping("/umpires")
    public ResponseEntity<List<TournamentUmpireSearchResponse>> searchUmpires(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> roles
    ) {
        List<UmpireSearchResult> umpires;
        if (roles != null && !roles.isEmpty()) {
            List<UserRole> userRoles = roles.stream()
                    .map(r -> {
                        try {
                            return UserRole.valueOf(r.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (!userRoles.isEmpty()) {
                umpires = tournamentUmpireService.searchByRoles(userRoles, query);
            } else {
                umpires = tournamentUmpireService.searchUmpires(query);
            }
        } else {
            umpires = tournamentUmpireService.searchUmpires(query);
        }
        return ResponseEntity.ok(umpires.stream()
                .map(umpire -> new TournamentUmpireSearchResponse(
                        umpire.getId(),
                        umpire.getEmail(),
                        umpire.getFirstName(),
                        umpire.getLastName()
                ))
                .toList());
    }

    @GetMapping("/tournaments/{tournamentId}/umpires")
    public ResponseEntity<List<TournamentUmpireResponse>> getTournamentUmpires(
            @PathVariable UUID tournamentId
    ) {
        List<TournamentUmpire> umpires = tournamentUmpireService.findByTournamentId(tournamentId);
        return ResponseEntity.ok(umpires.stream()
                .map(umpire -> new TournamentUmpireResponse(
                        umpire.getId(),
                        umpire.getTournamentId(),
                        umpire.getUmpireId(),
                        umpire.getUmpireEmail(),
                        umpire.getUmpireFirstName(),
                        umpire.getUmpireLastName(),
                        umpire.getAssignedAt()
                ))
                .toList());
    }

    @PostMapping("/tournaments/{tournamentId}/umpires")
    public ResponseEntity<TournamentUmpireResponse> addUmpire(
            @PathVariable UUID tournamentId,
            @RequestBody TournamentUmpireRequest request,
            Principal principal
    ) {
        TournamentUmpire umpire = tournamentUmpireService.addUmpire(
                tournamentId,
                request.id(),
                principal.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(umpire));
    }

    @DeleteMapping("/tournaments/{tournamentId}/umpires/{umpireId}")
    public ResponseEntity<Void> removeUmpire(
            @PathVariable UUID tournamentId,
            @PathVariable UUID umpireId,
            Principal principal
    ) {
        tournamentUmpireService.removeUmpire(tournamentId, umpireId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    private TournamentUmpireResponse toResponse(TournamentUmpire umpire) {
        return new TournamentUmpireResponse(
                umpire.getId(),
                umpire.getTournamentId(),
                umpire.getUmpireId(),
                umpire.getUmpireEmail(),
                umpire.getUmpireFirstName(),
                umpire.getUmpireLastName(),
                umpire.getAssignedAt()
        );
    }
}
