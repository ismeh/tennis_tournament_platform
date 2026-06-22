package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.commands.EventCommand;
import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.application.services.MatchService;
import com.tfm.tennis_platform.application.services.InscriptionService;
import com.tfm.tennis_platform.application.services.EventService;
import com.tfm.tennis_platform.application.services.CourtService;
import com.tfm.tennis_platform.application.services.ScheduleConfigService;
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
import com.tfm.tennis_platform.domain.models.ScheduleConfig;
import com.tfm.tennis_platform.domain.models.TimeSlot;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.infrastructure.controller.dto.CourtRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.CourtResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventInscriptionRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventInscriptionResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ManualEventInscriptionRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchScheduleRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentSummaryResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResultRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionCategoryCountResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionEventResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionGenderCountResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionPlayerResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentInscriptionsResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentGeneralInfoRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentStatusUpdateRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.ParticipantPointsUpdateRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.ScheduleConfigRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.ScheduleConfigResponse;
import com.tfm.tennis_platform.domain.models.inscription.ParticipantPointsUpdateCommand;
import com.tfm.tennis_platform.infrastructure.controller.mapper.TournamentWebMapper;
import com.tfm.tennis_platform.infrastructure.controller.mapper.MatchWebMapper;
import com.tfm.tennis_platform.infrastructure.pdf.TournamentPdfExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final TournamentPdfExporter tournamentPdfExporter;
    private final ScheduleConfigService scheduleConfigService;

    @PostMapping
    public ResponseEntity<TournamentResponse> create(@RequestBody TournamentRequest request, Principal principal) {
        Tournament tournament = tournamentWebMapper.toDomain(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toTournamentResponse(tournamentService.create(tournament, principal.getName(), request.courtCount())));
    }

    @GetMapping
    public ResponseEntity<List<TournamentSummaryResponse>> getAll() {
        return ResponseEntity.ok(tournamentService.findSummaries().stream()
                .map(TournamentController::toTournamentSummaryResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getById(@PathVariable UUID id) {
        Tournament tournament = tournamentService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", id));
        return ResponseEntity.ok(toTournamentResponse(tournament));
    }

    @PostMapping("/{tournamentId}/events")
    public ResponseEntity<TournamentResponse> addEventsToTournament(
            @PathVariable("tournamentId") UUID tournamentId,
            @RequestBody EventRequest eventRequest,
            Principal principal
    ) {
        EventCommand command = new EventCommand(eventRequest.getEvents().stream()
            .map(event -> new EventCommand.EventItem(event.getId(), event.getCategoryId(), event.getGender(), event.getStages()))
            .toList());
        Tournament updatedTournament = eventService.replaceAllEvents(tournamentId, command, principal.getName());
        return ResponseEntity.ok(toTournamentResponse(updatedTournament));
    }

    @DeleteMapping("/{tournamentId}/events/{eventId}")
    public ResponseEntity<TournamentResponse> removeEventFromTournament(
            @PathVariable("tournamentId") UUID tournamentId,
            @PathVariable("eventId") UUID eventId,
            Principal principal
    ) {
        Tournament updatedTournament = eventService.removeEventFromTournament(tournamentId, eventId, principal.getName());
        return ResponseEntity.ok(toTournamentResponse(updatedTournament));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TournamentResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody TournamentStatusUpdateRequest request,
            Principal principal
    ) {
        Tournament updatedTournament = tournamentService.updateStatus(id, request.status(), principal.getName());
        return ResponseEntity.ok(toTournamentResponse(updatedTournament));
    }

    @PutMapping("/{id}/general-info")
    public ResponseEntity<TournamentResponse> updateGeneralInfo(
            @PathVariable UUID id,
            @RequestBody TournamentGeneralInfoRequest request,
            Principal principal
    ) {
        com.tfm.tennis_platform.domain.models.TournamentPeriod playPeriod =
                new com.tfm.tennis_platform.domain.models.TournamentPeriod(
                        request.playStartDate(), request.playEndDate());
        com.tfm.tennis_platform.domain.models.TournamentPeriod inscriptionPeriod =
                new com.tfm.tennis_platform.domain.models.TournamentPeriod(
                        request.inscriptionStartDate(), request.inscriptionEndDate());

        Tournament updatedTournament = tournamentService.updateGeneralInfo(
                id,
                request.formalName(),
                playPeriod,
                request.tournamentStartTime(),
                inscriptionPeriod,
                request.surfaceCategory(),
                request.maxPlayers(),
                request.location(),
                request.locationLatitude(),
                request.locationLongitude(),
                request.locationPlaceId(),
                request.locationFormattedAddress(),
                principal.getName()
        );
        return ResponseEntity.ok(toTournamentResponse(updatedTournament));
    }

    @GetMapping("/{tournamentId}/courts")
    public ResponseEntity<List<CourtResponse>> getCourts(@PathVariable UUID tournamentId) {
        return ResponseEntity.ok(courtService.findByTournamentId(tournamentId).stream()
                .map(TournamentController::toCourtResponse)
                .toList());
    }

    @PostMapping("/{tournamentId}/courts")
    public ResponseEntity<CourtResponse> createCourt(
            @PathVariable UUID tournamentId,
            @RequestBody CourtRequest request,
            Principal principal
    ) {
        Court court = courtService.create(tournamentId, request.name(), principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toCourtResponse(court));
    }

    @PatchMapping("/{tournamentId}/courts/{courtId}")
    public ResponseEntity<CourtResponse> updateCourt(
            @PathVariable UUID tournamentId,
            @PathVariable UUID courtId,
            @RequestBody CourtRequest request,
            Principal principal
    ) {
        Court court = courtService.update(tournamentId, courtId, request.name(), principal.getName());
        return ResponseEntity.ok(toCourtResponse(court));
    }

    @DeleteMapping("/{tournamentId}/courts/{courtId}")
    public ResponseEntity<Void> deleteCourt(
            @PathVariable UUID tournamentId,
            @PathVariable UUID courtId,
            Principal principal
    ) {
        courtService.delete(tournamentId, courtId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tournamentId}/schedule-config")
    public ResponseEntity<ScheduleConfigResponse> getScheduleConfig(@PathVariable UUID tournamentId) {
        ScheduleConfig config = scheduleConfigService.findByTournamentId(tournamentId);
        if (config == null) {
            return ResponseEntity.ok(new ScheduleConfigResponse(null, tournamentId, List.of(), 60));
        }
        return ResponseEntity.ok(toScheduleConfigResponse(config));
    }

    @PutMapping("/{tournamentId}/schedule-config")
    public ResponseEntity<ScheduleConfigResponse> saveScheduleConfig(
            @PathVariable UUID tournamentId,
            @RequestBody ScheduleConfigRequest request,
            Principal principal
    ) {
        List<TimeSlot> timeSlots = request.timeSlots() != null
                ? request.timeSlots().stream()
                    .map(ts -> new TimeSlot(ts.startTime(), ts.endTime()))
                    .toList()
                : List.of();

        ScheduleConfig config = scheduleConfigService.save(
                tournamentId, timeSlots, request.matchDurationMinutes(), principal.getName());
        return ResponseEntity.ok(toScheduleConfigResponse(config));
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

    @PatchMapping("/{tournamentId}/participants/points")
    public ResponseEntity<Void> updateParticipantsPoints(
            @PathVariable UUID tournamentId,
            @RequestBody List<ParticipantPointsUpdateRequest> updates,
            Principal principal
    ) {
        tournamentService.assertTournamentAdmin(tournamentId, principal.getName());
        List<ParticipantPointsUpdateCommand> commands = updates.stream()
                .map(u -> new ParticipantPointsUpdateCommand(u.participantId(), u.points(), u.seed()))
                .toList();
        inscriptionService.updateParticipantsPoints(tournamentId, commands, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tournamentId}/events/{eventId}/generate-draws")
    public ResponseEntity<TournamentResponse> generateDraws(
            @PathVariable UUID tournamentId,
            @PathVariable UUID eventId,
            Principal principal
    ) {
        Tournament updatedTournament = eventService.generateDrawsForEvent(tournamentId, eventId, principal.getName());
        return ResponseEntity.ok(toTournamentResponse(updatedTournament));
    }

        @PostMapping("/{tournamentId}/matches/{matchId}/result")
        public ResponseEntity<MatchResponse> recordMatchResult(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
                @RequestBody MatchResultRequest request,
                Principal principal
        ) {
        return ResponseEntity.ok(matchWebMapper.toResponse(
            matchService.recordResult(tournamentId, matchId, request.winnerId(), request.scoreString(), principal.getName())
        ));
        }

        @PatchMapping("/{tournamentId}/matches/{matchId}/schedule")
        public ResponseEntity<MatchResponse> scheduleMatch(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @RequestBody MatchScheduleRequest request,
            Principal principal
        ) {
        return ResponseEntity.ok(matchWebMapper.toResponse(
            matchService.schedule(tournamentId, matchId, request.courtId(), request.scheduledAt(), request.scheduleTimeType(), Boolean.TRUE.equals(request.cascade()), principal.getName())
        ));
        }

    @GetMapping("/{tournamentId}/export/pdf")
    public ResponseEntity<byte[]> exportTournamentPdf(
            @PathVariable UUID tournamentId
    ) {
        Tournament tournament = tournamentService.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));

        TournamentInscriptionsView inscriptions = inscriptionService.findByTournament(tournamentId, null);
        List<Court> courts = courtService.findByTournamentId(tournamentId);
        List<com.tfm.tennis_platform.domain.models.Match> matches = matchService.findByTournamentId(tournamentId);

        byte[] pdfBytes = tournamentPdfExporter.exportTournamentData(tournament, inscriptions, courts, matches);

        String filename = sanitizeFilename(tournament.getName()) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    private String sanitizeFilename(String name) {
        if (name == null) {
            return "torneo";
        }
        return name.replaceAll("[^a-zA-Z0-9_\\- ]", "")
                   .replaceAll("\\s+", "_")
                   .toLowerCase();
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

    private TournamentResponse toTournamentResponse(Tournament tournament) {
        TournamentResponse response = tournamentWebMapper.toResponse(tournament);
        boolean professionalTournament = tournamentService.isProfessionalTournament(tournament.getId());

        return new TournamentResponse(
                response.id(),
                response.formalName(),
                response.playStartDate(),
                response.playEndDate(),
                response.tournamentStartTime(),
                response.inscriptionStartDate(),
                response.inscriptionEndDate(),
                response.surfaceCategory(),
                response.maxPlayers(),
                response.location(),
                response.locationLatitude(),
                response.locationLongitude(),
                response.locationPlaceId(),
                response.locationFormattedAddress(),
                response.status(),
                response.providerOrganisationId(),
                response.events(),
                professionalTournament
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
                player.gender(),
                player.points(),
                player.seed()
        );
    }

    private static ScheduleConfigResponse toScheduleConfigResponse(ScheduleConfig config) {
        List<ScheduleConfigResponse.TimeSlotResponse> slots = config.getTimeSlots() != null
                ? config.getTimeSlots().stream()
                    .map(ts -> new ScheduleConfigResponse.TimeSlotResponse(ts.startTime(), ts.endTime()))
                    .toList()
                : List.of();
        return new ScheduleConfigResponse(
                config.getId(),
                config.getTournamentId(),
                slots,
                config.getMatchDurationMinutes()
        );
    }
}
