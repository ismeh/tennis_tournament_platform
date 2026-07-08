package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.CalendarService;
import com.tfm.tennis_platform.domain.models.calendar.PlayerInscriptionItem;
import com.tfm.tennis_platform.domain.models.calendar.PlayerMatchCalendarItem;
import com.tfm.tennis_platform.domain.models.calendar.TournamentCalendarItem;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.controller.dto.PlayerInscriptionResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.PlayerMatchCalendarResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentCalendarPageResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentCalendarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/tournaments")
    public ResponseEntity<TournamentCalendarPageResponse> getPublishedTournaments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Surface surface,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean professionalTournament,
            @RequestParam(required = false) TournamentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal
    ) {
        String requesterEmail = principal != null ? principal.getName() : null;
        var result = calendarService.findPublishedTournamentsPaginated(from, to, surface, location, name, professionalTournament, status, requesterEmail, page, size);
        List<TournamentCalendarResponse> content = result.content().stream()
                .map(CalendarController::toTournamentResponse)
                .toList();
        return ResponseEntity.ok(new TournamentCalendarPageResponse(
                content,
                page,
                size,
                result.totalElements(),
                (int) Math.ceil((double) result.totalElements() / size)
        ));
    }

    @GetMapping("/my-matches")
    public ResponseEntity<List<PlayerMatchCalendarResponse>> getMyMatches(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal
    ) {
        return ResponseEntity.ok(calendarService.findScheduledMatchesForPlayer(principal.getName(), from, to).stream()
                .map(CalendarController::toMatchResponse)
                .toList());
    }

    @GetMapping("/my-tournaments")
    public ResponseEntity<List<TournamentCalendarResponse>> getMyTournaments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal
    ) {
        return ResponseEntity.ok(calendarService.findMyTournaments(principal.getName(), from, to).stream()
                .map(CalendarController::toTournamentResponse)
                .toList());
    }

    @GetMapping("/my-inscriptions")
    public ResponseEntity<List<PlayerInscriptionResponse>> getMyInscriptions(Principal principal) {
        return ResponseEntity.ok(calendarService.findMyInscriptions(principal.getName()).stream()
                .map(item -> new PlayerInscriptionResponse(
                        item.tournamentId(),
                        item.tournamentName(),
                        item.eventId(),
                        item.eventName(),
                        item.categoryName(),
                        item.entryStatus(),
                        item.paymentStatus(),
                        item.playStartDate(),
                        item.playEndDate()
                ))
                .toList());
    }

    private static TournamentCalendarResponse toTournamentResponse(TournamentCalendarItem item) {
        return new TournamentCalendarResponse(
                item.id(),
                item.name(),
                item.playStartDate(),
                item.playEndDate(),
                item.startTime(),
                item.location(),
                item.surface(),
                item.maxPlayers(),
                item.status(),
                item.professionalTournament()
        );
    }

    private static PlayerMatchCalendarResponse toMatchResponse(PlayerMatchCalendarItem item) {
        return new PlayerMatchCalendarResponse(
                item.tournamentId(),
                item.tournamentName(),
                item.eventId(),
                item.eventName(),
                item.matchId(),
                item.roundNumber(),
                item.scheduledAt(),
                item.scheduleTimeType() != null ? item.scheduleTimeType().name() : null,
                item.courtId(),
                item.court(),
                item.firstInscriptionId(),
                item.firstParticipantName(),
                item.secondInscriptionId(),
                item.secondParticipantName(),
                item.result()
        );
    }
}
