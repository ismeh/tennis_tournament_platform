package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.application.dto.EventCommand;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {
    private final TournamentRepository tournamentRepository;

    @Transactional
    public Tournament addEventsToTournament(UUID tournamentId, EventCommand eventCommand) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        List<Event> events = eventCommand.events().stream()
                .map(event -> Event.builder()
                        .tournamentId(tournamentId)
                        .categoryId(event.categoryId())
                        .gender(event.gender())
                        .build())
                .toList();

        Tournament tournamentWithNewEvents = tournament.addEvent(events);

        return tournamentRepository.save(tournamentWithNewEvents);
    }

    @Transactional
    public Tournament removeEventFromTournament(UUID tournamentId, UUID eventId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        List<Event> updatedEvents = tournament.getEvents().stream()
                .filter(event -> !eventId.equals(event.getId()))
                .toList();

        if (updatedEvents.size() == tournament.getEvents().size()) {
            throw new IllegalArgumentException("Event not found in tournament");
        }

        Tournament tournamentWithoutEvent = tournament.toBuilder()
                .clearEvents()
                .events(updatedEvents)
                .build();

        return tournamentRepository.save(tournamentWithoutEvent);
    }
}
