package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.infrastructure.controller.dto.EventRequest;
import com.tfm.tennis_platform.infrastructure.persistence.entity.TournamentEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.TournamentEntityMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaTournamentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {
    private final TournamentRepository tournamentRepository;
    private final TournamentEntityMapper tournamentEntityMapper;

    @Transactional
    public Tournament addEventsToTournament(UUID tournamentId, EventRequest eventRequest) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        List<Event> events = eventRequest.getEvents().stream()
                .map(event -> Event.builder()
                        .tournamentId(tournamentId)
                        .categoryId(event.getCategoryId())
                        .gender(event.getGender())
                        .build())
                .toList();

        tournament = tournament.addEvent(events);

        tournamentRepository.save(tournament);

        return tournament;
    }
}
