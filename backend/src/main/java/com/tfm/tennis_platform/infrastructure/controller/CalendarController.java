package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.CalendarService;
import com.tfm.tennis_platform.domain.models.calendar.PlayerMatchCalendarItem;
import com.tfm.tennis_platform.domain.models.calendar.TournamentCalendarItem;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.controller.dto.PlayerMatchCalendarResponse;
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
    public ResponseEntity<List<TournamentCalendarResponse>> getPublishedTournaments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Surface surface,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean professionalTournament,
            @RequestParam(required = false) TournamentStatus status,
            Principal principal
    ) {
        String requesterEmail = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(calendarService.findPublishedTournaments(from, to, surface, location, name, professionalTournament, status, requesterEmail).stream()
                .map(CalendarController::toTournamentResponse)
                .toList());
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
