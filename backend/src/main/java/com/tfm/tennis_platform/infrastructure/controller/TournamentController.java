package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.application.services.InscriptionService;
import com.tfm.tennis_platform.application.service.EventService;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventInscriptionRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventInscriptionResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionsResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentStatusUpdateRequest;
import com.tfm.tennis_platform.infrastructure.controller.mapper.TournamentWebMapper;
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
    private final InscriptionService inscriptionService;

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

    @DeleteMapping("/{tournamentId}/events/{eventId}")
    public ResponseEntity<TournamentResponse> removeEventFromTournament(
            @PathVariable("tournamentId") UUID tournamentId,
            @PathVariable("eventId") UUID eventId
    ) {
        Tournament updatedTournament = eventService.removeEventFromTournament(tournamentId, eventId);
        return ResponseEntity.ok(tournamentWebMapper.toResponse(updatedTournament));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TournamentResponse> updateStatus(@PathVariable UUID id, @RequestBody TournamentStatusUpdateRequest request) {
        Tournament updatedTournament = tournamentService.updateStatus(id, request.status());
        return ResponseEntity.ok(tournamentWebMapper.toResponse(updatedTournament));
    }

    @PostMapping("/{tournamentId}/events/{eventId}/inscriptions")
    public ResponseEntity<EventInscriptionResponse> createInscription(
            @PathVariable UUID tournamentId,
            @PathVariable UUID eventId,
                @RequestBody EventInscriptionRequest request,
            Principal principal
    ) {
        EventInscriptionResponse response = inscriptionService.register(tournamentId, eventId, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{tournamentId}/events/{eventId}/inscriptions")
    public ResponseEntity<List<EventInscriptionResponse>> getInscriptionsByEvent(
            @PathVariable UUID tournamentId,
            @PathVariable UUID eventId
    ) {
        return ResponseEntity.ok(inscriptionService.findByEvent(tournamentId, eventId));
    }

    @GetMapping("/{tournamentId}/inscriptions")
    public ResponseEntity<TournamentInscriptionsResponse> getTournamentInscriptions(
            @PathVariable UUID tournamentId,
            @RequestParam(required = false) UUID eventId
    ) {
        return ResponseEntity.ok(inscriptionService.findByTournament(tournamentId, eventId));
    }
}
