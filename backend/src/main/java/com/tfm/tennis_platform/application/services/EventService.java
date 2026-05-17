package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.commands.EventCommand;
import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.port.out.InscriptionRepository;
import com.tfm.tennis_platform.domain.port.out.MatchRepository;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static com.tfm.tennis_platform.domain.models.Event.buildEventName;
import static com.tfm.tennis_platform.domain.models.Event.createOrUpdateEvent;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private final TournamentRepository tournamentRepository;
    private final InscriptionRepository inscriptionRepository;
    private final MatchRepository matchRepository;
    private final StageGenerationService stageGenerationService;
    private final DrawGenerationService drawGenerationService;
    private final MatchGenerationService matchGenerationService;
        private final MatchPersistenceService matchPersistenceService;

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

                    // At this point we only attach empty draws; matches will be generated after
                    List<Draw> drawsWithoutMatches = draws.stream()
                            .map(draw -> draw.toBuilder().matches(List.of()).build())
                            .collect(Collectors.toList());

                    return stage.toBuilder()
                            .clearDraws()
                            .draws(drawsWithoutMatches)
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

                // Persist tournament + draws first so draws exist in DB (draw ids are used by matches)
                Tournament persistedTournament = tournamentRepository.save(updatedTournament);

                // Now generate and persist matches for each draw. Use retries on optimistic lock failures.
                Map<UUID, List<Match>> matchesByDraw = new java.util.HashMap<>();
                for (Event ev : persistedTournament.getEvents()) {
                        if (!ev.getId().equals(eventId)) continue;

                        for (Stage st : ev.getStages()) {
                                for (Draw dr : st.getDraws()) {
                                        List<Match> matches = matchGenerationService.generateMatchesForDraw(dr, eventInscriptions);

                                        // Sort matches so that referenced nextMatch (higher rounds) are saved first
                                        List<Match> sortedMatches = matches.stream()
                                                        .sorted((a, b) -> Integer.compare(b.getRoundNumber() == null ? 0 : b.getRoundNumber(), a.getRoundNumber() == null ? 0 : a.getRoundNumber()))
                                                        .toList();

                                        // Retry loop for optimistic locking conflicts
                                        int attempts = 0;
                                        final int maxAttempts = 3;
                                        // Persist matches in a separate transaction to avoid flush/order interactions
                                        matchPersistenceService.saveMatches(sortedMatches);

                                        matchesByDraw.put(dr.getId(), matches);
                                }
                        }
                }

                // Rebuild returned Tournament domain object with matches attached to draws
                List<Event> rebuiltEvents = persistedTournament.getEvents().stream()
                                .map(ev -> {
                                        if (!ev.getId().equals(eventId)) return ev;
                                        List<Stage> rebuiltStages = ev.getStages().stream()
                                                        .map(st -> {
                                                                List<Draw> rebuiltDraws = st.getDraws().stream()
                                                                                .map(dr -> dr.toBuilder()
                                                                                                .matches(matchesByDraw.getOrDefault(dr.getId(), java.util.List.of()))
                                                                                                .build())
                                                                                .collect(Collectors.toList());
                                                                return st.toBuilder().draws(rebuiltDraws).build();
                                                        })
                                                        .collect(Collectors.toList());
                                        return ev.toBuilder().stages(rebuiltStages).build();
                                })
                                .collect(Collectors.toList());

                return persistedTournament.toBuilder().events(rebuiltEvents).build();
    }
}
