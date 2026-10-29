package com.puntomatch.infrastructure.controller;

import com.puntomatch.application.services.TournamentService;
import com.puntomatch.application.service.EventService;
import com.puntomatch.domain.models.Tournament;
import com.puntomatch.infrastructure.controller.dto.TournamentRequest;
import com.puntomatch.infrastructure.controller.dto.TournamentResponse;
import com.puntomatch.infrastructure.controller.dto.EventRequest;
import com.puntomatch.infrastructure.controller.dto.TournamentStatusUpdateRequest;
import com.puntomatch.infrastructure.controller.mapper.TournamentWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final TournamentWebMapper tournamentWebMapper;
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<TournamentResponse> create(@RequestBody TournamentRequest request, Principal principal) {
        Tournament tournament = tournamentWebMapper.toDomain(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tournamentWebMapper.toResponse(tournamentService.create(tournament, principal.getName())));
    }

    @GetMapping
    public ResponseEntity<List<TournamentResponse>> getAll() {
        return ResponseEntity.ok(tournamentService.findAll().stream()
                .map(tournamentWebMapper::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getById(@PathVariable UUID id) {
        return tournamentService.findById(id)
                .map(t -> ResponseEntity.ok(tournamentWebMapper.toResponse(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{tournamentId}/events")
    public ResponseEntity<TournamentResponse> addEventsToTournament(@PathVariable("tournamentId") UUID tournamentId, @RequestBody EventRequest eventRequest) {
        Tournament updatedTournament = eventService.addEventsToTournament(tournamentId, eventRequest);
        return ResponseEntity.ok(tournamentWebMapper.toResponse(updatedTournament));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TournamentResponse> updateStatus(@PathVariable UUID id, @RequestBody TournamentStatusUpdateRequest request) {
        Tournament updatedTournament = tournamentService.updateStatus(id, request.status());
        return ResponseEntity.ok(tournamentWebMapper.toResponse(updatedTournament));
    }
}
