package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.commands.EventCommand;
import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.port.out.InscriptionRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tfm.tennis_platform.domain.models.Event.buildEventName;
import static com.tfm.tennis_platform.domain.models.Event.createOrUpdateEvent;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private final TournamentRepository tournamentRepository;
    private final InscriptionRepository inscriptionRepository;
    private final StageGenerationService stageGenerationService;
    private final DrawGenerationService drawGenerationService;

    @Transactional
    public Tournament replaceAllEvents(UUID tournamentId, EventCommand eventCommand) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        List<Event> events = eventCommand.events().stream()
                .map(item -> {
                    var stages = stageGenerationService.generateStagesForEvent(
                            buildEventName(item),

                            item.categoryId(),
                            item.gender(),
                            item.stages()
                    );
                    return createOrUpdateEvent(tournamentId, item, stages);
                })
                .toList();

        return tournamentRepository.save(tournament.setEvents(events));
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
                .events(updatedEvents)
                .build();

        return tournamentRepository.save(tournamentWithoutEvent);
    }

    @Transactional
    public Tournament generateDrawsForEvent(UUID tournamentId, UUID eventId) {
        log.info("Generating draws for event {} in tournament {}", eventId, tournamentId);
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        Event event = tournament.getEvents().stream()
                .filter(e -> e.getId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Event not found in tournament"));

        List<Inscription> eventInscriptions = inscriptionRepository.findByEventId(eventId);

        if (eventInscriptions.isEmpty()) {
            throw new IllegalArgumentException("No inscriptions found for event");
        }

        List<Stage> updatedStages = event.getStages().stream()
                .map(stage -> {
                    List<Draw> draws = drawGenerationService
                            .generateDrawsForStage(stage, eventInscriptions);
                    return stage.toBuilder()
                            .clearDraws()
                            .draws(draws)
                            .build();
                })
                .collect(Collectors.toList());

        Event updatedEvent = event.toBuilder()
                .clearStages()
                .stages(updatedStages)
                .build();

        List<Event> updatedEvents = tournament.getEvents().stream()
                .map(e -> e.getId().equals(eventId) ? updatedEvent : e)
                .collect(Collectors.toList());

        Tournament updatedTournament = tournament.toBuilder()
                .events(updatedEvents)
                .build();

        return tournamentRepository.save(updatedTournament);
    }
}
