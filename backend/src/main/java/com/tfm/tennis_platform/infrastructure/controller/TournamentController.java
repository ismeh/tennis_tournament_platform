package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.commands.EventCommand;
import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.application.services.MatchService;
import com.tfm.tennis_platform.application.services.InscriptionService;
import com.tfm.tennis_platform.application.services.EventService;
import com.tfm.tennis_platform.application.services.CourtService;
import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionResult;
import com.tfm.tennis_platform.domain.models.inscription.ManualEventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionCategoryCount;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionEventView;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionGenderCount;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionPlayerView;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionsView;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.infrastructure.controller.dto.CourtRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.CourtResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventInscriptionRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventInscriptionResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ManualEventInscriptionRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchScheduleRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResultRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionCategoryCountResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionEventResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionGenderCountResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionPlayerResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionsResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentStatusUpdateRequest;
import com.tfm.tennis_platform.infrastructure.controller.mapper.TournamentWebMapper;
import com.tfm.tennis_platform.infrastructure.controller.mapper.MatchWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final MatchService matchService;
    private final TournamentWebMapper tournamentWebMapper;
    private final MatchWebMapper matchWebMapper;
    private final EventService eventService;
    private final InscriptionService inscriptionService;
    private final CourtService courtService;

    @PostMapping
    public ResponseEntity<TournamentResponse> create(@RequestBody TournamentRequest request, Principal principal) {
        Tournament tournament = tournamentWebMapper.toDomain(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tournamentWebMapper.toResponse(tournamentService.create(tournament, principal.getName(), request.courtCount())));
    }

    @GetMapping
    public ResponseEntity<List<TournamentResponse>> getAll() {
        return ResponseEntity.ok(tournamentService.findAll().stream()
                .map(tournamentWebMapper::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getById(@PathVariable UUID id) {
        Tournament tournament = tournamentService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", id));
        return ResponseEntity.ok(tournamentWebMapper.toResponse(tournament));
    }

    @PostMapping("/{tournamentId}/events")
    public ResponseEntity<TournamentResponse> addEventsToTournament(@PathVariable("tournamentId") UUID tournamentId, @RequestBody EventRequest eventRequest) {
        EventCommand command = new EventCommand(eventRequest.getEvents().stream()
            .map(event -> new EventCommand.EventItem(event.getId(), event.getCategoryId(), event.getGender(), event.getStages()))
            .toList());
        Tournament updatedTournament = eventService.replaceAllEvents(tournamentId, command);
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

    @GetMapping("/{tournamentId}/courts")
    public ResponseEntity<List<CourtResponse>> getCourts(@PathVariable UUID tournamentId) {
        return ResponseEntity.ok(courtService.findByTournamentId(tournamentId).stream()
                .map(TournamentController::toCourtResponse)
                .toList());
    }

    @PostMapping("/{tournamentId}/courts")
    public ResponseEntity<CourtResponse> createCourt(@PathVariable UUID tournamentId, @RequestBody CourtRequest request) {
        Court court = courtService.create(tournamentId, request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(toCourtResponse(court));
    }

    @PatchMapping("/{tournamentId}/courts/{courtId}")
    public ResponseEntity<CourtResponse> updateCourt(
            @PathVariable UUID tournamentId,
            @PathVariable UUID courtId,
            @RequestBody CourtRequest request
    ) {
        Court court = courtService.update(tournamentId, courtId, request.name());
        return ResponseEntity.ok(toCourtResponse(court));
    }

    @DeleteMapping("/{tournamentId}/courts/{courtId}")
    public ResponseEntity<Void> deleteCourt(
            @PathVariable UUID tournamentId,
            @PathVariable UUID courtId
    ) {
        courtService.delete(tournamentId, courtId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tournamentId}/events/{eventId}/inscriptions")
    public ResponseEntity<EventInscriptionResponse> createInscription(
            @PathVariable UUID tournamentId,
            @PathVariable UUID eventId,
            @RequestBody EventInscriptionRequest request,
            Principal principal
    ) {
        EventInscriptionCommand command = new EventInscriptionCommand(request.categoryId(), request.partnerId());
        EventInscriptionResult result = inscriptionService.register(tournamentId, eventId, command, principal.getName());
        EventInscriptionResponse response = toEventInscriptionResponse(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{tournamentId}/events/{eventId}/manual-inscriptions")
    public ResponseEntity<EventInscriptionResponse> createManualInscription(
            @PathVariable UUID tournamentId,
            @PathVariable UUID eventId,
            @RequestBody ManualEventInscriptionRequest request,
            Principal principal
    ) {
        ManualEventInscriptionCommand command = new ManualEventInscriptionCommand(
            request.playerSource(),
            request.personId(),
            request.firstName(),
            request.lastName(),
            request.gender(),
            request.birthDate(),
            request.nationality(),
            request.tennisId(),
            request.proPlayerId()
        );
        EventInscriptionResult result = inscriptionService.registerManual(tournamentId, eventId, command, principal.getName());
        EventInscriptionResponse response = toEventInscriptionResponse(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{tournamentId}/events/{eventId}/inscriptions")
    public ResponseEntity<List<EventInscriptionResponse>> getInscriptionsByEvent(
            @PathVariable UUID tournamentId,
            @PathVariable UUID eventId
    ) {
        return ResponseEntity.ok(inscriptionService.findByEvent(tournamentId, eventId).stream()
            .map(TournamentController::toEventInscriptionResponse)
            .toList());
    }

    @GetMapping("/{tournamentId}/inscriptions")
    public ResponseEntity<TournamentInscriptionsResponse> getTournamentInscriptions(
            @PathVariable UUID tournamentId,
            @RequestParam(required = false) UUID eventId
    ) {
        TournamentInscriptionsView view = inscriptionService.findByTournament(tournamentId, eventId);
        return ResponseEntity.ok(toTournamentInscriptionsResponse(view));
    }

    @PostMapping("/{tournamentId}/events/{eventId}/generate-draws")
    public ResponseEntity<TournamentResponse> generateDraws(
            @PathVariable UUID tournamentId,
            @PathVariable UUID eventId
    ) {
        Tournament updatedTournament = eventService.generateDrawsForEvent(tournamentId, eventId);
        return ResponseEntity.ok(tournamentWebMapper.toResponse(updatedTournament));
    }

        @PostMapping("/{tournamentId}/matches/{matchId}/result")
        public ResponseEntity<MatchResponse> recordMatchResult(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
                @RequestBody MatchResultRequest request
        ) {
        return ResponseEntity.ok(matchWebMapper.toResponse(
            matchService.recordResult(tournamentId, matchId, request.winnerId(), request.scoreString())
        ));
        }

        @PatchMapping("/{tournamentId}/matches/{matchId}/schedule")
        public ResponseEntity<MatchResponse> scheduleMatch(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @RequestBody MatchScheduleRequest request
        ) {
        return ResponseEntity.ok(matchWebMapper.toResponse(
            matchService.schedule(tournamentId, matchId, request.courtId(), request.scheduledAt(), request.scheduleTimeType())
        ));
        }

    private static EventInscriptionResponse toEventInscriptionResponse(EventInscriptionResult result) {
        return new EventInscriptionResponse(
                result.id(),
                result.eventId(),
                result.participantId(),
                result.status(),
                result.paymentStatus(),
                result.registeredAt()
        );
    }

    private static CourtResponse toCourtResponse(Court court) {
        return new CourtResponse(
                court.getId(),
                court.getTournamentId(),
                court.getName(),
                court.isActive()
        );
    }

    private static TournamentInscriptionsResponse toTournamentInscriptionsResponse(TournamentInscriptionsView view) {
        return new TournamentInscriptionsResponse(
                view.tournamentId(),
                view.selectedEventId(),
                view.events().stream().map(TournamentController::toTournamentEventResponse).toList(),
                view.categoryCounts().stream().map(TournamentController::toCategoryCountResponse).toList(),
                view.inscriptions().stream().map(TournamentController::toPlayerResponse).toList()
        );
    }

    private static TournamentInscriptionEventResponse toTournamentEventResponse(TournamentInscriptionEventView event) {
        return new TournamentInscriptionEventResponse(
                event.eventId(),
                event.categoryId(),
                event.category(),
                event.eventName(),
                event.eventGender()
        );
    }

    private static TournamentInscriptionCategoryCountResponse toCategoryCountResponse(TournamentInscriptionCategoryCount category) {
        return new TournamentInscriptionCategoryCountResponse(
                category.categoryId(),
                category.category(),
                category.totalPlayers(),
                category.genders().stream().map(TournamentController::toGenderCountResponse).toList()
        );
    }

    private static TournamentInscriptionGenderCountResponse toGenderCountResponse(TournamentInscriptionGenderCount gender) {
        return new TournamentInscriptionGenderCountResponse(gender.gender(), gender.totalPlayers());
    }

    private static TournamentInscriptionPlayerResponse toPlayerResponse(TournamentInscriptionPlayerView player) {
        return new TournamentInscriptionPlayerResponse(
                player.inscriptionId(),
                player.eventId(),
                player.categoryId(),
                player.category(),
                player.eventName(),
                player.eventGender(),
                player.personId(),
                player.playerSource(),
                player.tennisId(),
                player.firstName(),
                player.lastName(),
                player.gender()
        );
    }
}
